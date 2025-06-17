package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.MovementAction;
import btm.sword.system.playerdata.CombatProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

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
	
	}
	
	public void throwGrabbedEntity() {
		abilityTask.cancel();
		isGrabbing = false;
		Bukkit.getScheduler().runTaskLater(Sword.getInstance(), MovementAction.toss(this, grabbedEntity), 1);
	}
}
