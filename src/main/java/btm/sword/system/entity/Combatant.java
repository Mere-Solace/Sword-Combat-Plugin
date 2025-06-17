package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.AttackAction;
import btm.sword.system.action.MovementAction;
import btm.sword.system.input.InputAction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.StatType;
import btm.sword.util.Cache;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public abstract class Combatant extends SwordEntity {
	protected CombatProfile combatProfile;
	
	private BukkitTask abilityTask;
	private int basicAttackStage = 0;
	private boolean isGrabbing = false;
	private SwordEntity grabbedEntity;
	
	public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity);
		this.combatProfile = combatProfile;
	}
	
	public CombatProfile getCombatProfile() {
		return combatProfile;
	}
	
	public BukkitTask getAbilityTask() {
		return abilityTask;
	}
	
	public void setAbilityTask(BukkitTask abilityTask) {
		this.abilityTask = abilityTask;
	}
	
	public boolean isAbilityTaskFinished() {
		return abilityTask == null;
	}
	
	public boolean isGrabbing() {
		return isGrabbing;
	}
	
	public void setGrabbing(boolean isGrabbing) {
		this.isGrabbing = isGrabbing;
	}
	
	public SwordEntity getGrabbedEntity() {
		return grabbedEntity;
	}
	
	public void setGrabbedEntity(SwordEntity grabbedEntity) {
		this.grabbedEntity = grabbedEntity;
	}
	
	public void onGrab(SwordEntity target) {
		LivingEntity t = target.entity();
		target.setGrabbed(true);
		setGrabbing(true);
		setGrabbedEntity(target);
		t.damage(0.25, associatedEntity);
		Cache.grabCloudParticle.display(t.getLocation().add(new Vector(0, 1, 0)));
	}
	
	public void onGrabLetGo() {
		isGrabbing = false;
		grabbedEntity.setGrabbed(false);
		associatedEntity.sendMessage("it broke free. ended task: " + abilityTask);
		endAction();
	}
	
	public void onGrabThrow() {
		isGrabbing = false;
		grabbedEntity.setGrabbed(false);
		Bukkit.getScheduler().runTaskLater(Sword.getInstance(), MovementAction.toss(this, grabbedEntity), 1);
		associatedEntity.sendMessage("You threw that sunofa gun. ended task: " + abilityTask);
		endAction();
	}
	
	// if the player is grabbing, is being grabbed, or is currently casting an ability,
	// return true, that they CANNOT perform an action
	public boolean cannotPerformAction() {
		boolean cannotPerform = false;
		if (isGrabbing) {
			entity().sendMessage("You're grabbing sum1 rn lad");
			cannotPerform = true;
		}
		if (isGrabbed()) {
			entity().sendMessage("You're being pulled off bro!");
			cannotPerform = true;
		}
		if (abilityTask != null) {
			entity().sendMessage("You're already casting something rn: " + abilityTask.toString());
			cannotPerform = true;
		}
		return cannotPerform;
	}
	// for every runnable, must reset task to null when finished.

	
	public void performBasicAttack() {
		associatedEntity.sendMessage("Performing basic attack");
		if (basicAttackStage > 2) basicAttackStage = 0;
		new InputAction(
			AttackAction.basic(this, basicAttackStage),
			executor -> executor.calcCooldown(200L, 1000L, StatType.FORM, 10),
			Combatant::cannotPerformAction,
			false).execute((SwordPlayer) this, Bukkit.getScheduler(), Sword.getInstance());
		basicAttackStage++;
	}
	
	public void endAction() {
		if (abilityTask != null) {
			abilityTask.cancel();
			abilityTask = null;
		}
	}
	
	public double calcValue(StatType stat, double base, double multiplier) {
		return base + (multiplier * combatProfile.getStat(stat));
	}
}
