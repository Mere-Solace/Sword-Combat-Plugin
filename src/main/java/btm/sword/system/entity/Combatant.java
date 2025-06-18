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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public abstract class Combatant extends SwordEntity {
	protected CombatProfile combatProfile;
	
	private BukkitTask basicAttackSequenceTimeout = null;
	private BukkitTask basicAttackCooldownReset = null;
	private boolean canBasicAttack = true;
	private BukkitTask abilityTask = null;
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
		endAction();
	}
	
	public void onGrabThrow() {
		isGrabbing = false;
		grabbedEntity.setGrabbed(false);
		Bukkit.getScheduler().runTaskLater(Sword.getInstance(), MovementAction.toss(this, grabbedEntity), 1);
		endAction();
	}
	
	// if the player is grabbing, is being grabbed, or is currently casting an ability,
	// return true, that they CANNOT perform an action
	public boolean cannotPerformAnyAction() {
		return isGrabbing || isGrabbed() || abilityTask != null;
	}
	// for every runnable, must reset task to null when finished.

	public void performBasicAttack() {
		if (!canBasicAttack) {
			associatedEntity.sendMessage("Can't perform it rn lil bro");
			return;
		}
		if (basicAttackSequenceTimeout != null && basicAttackSequenceTimeout.getTaskId() != -1) {
			basicAttackSequenceTimeout.cancel();
			basicAttackSequenceTimeout = null;
		}
		associatedEntity.sendMessage("Trying to perform stage: " + basicAttackStage);
		if (!new InputAction(
				AttackAction.basic(this, basicAttackStage),
				executor -> executor.calcCooldown(0L, 0L, StatType.CELERITY, 124),
				Combatant::cannotPerformAnyAction,
				false)
				.execute((SwordPlayer) this, Bukkit.getScheduler(), Sword.getInstance())) {
			associatedEntity.sendMessage("It didn't work lol");
			return;
		}
		
		basicAttackStage++;
		if (basicAttackStage > 2){
			basicAttackStage = 0;
			canBasicAttack = false;
			
			if (basicAttackCooldownReset != null && basicAttackCooldownReset.getTaskId() != -1) {
				basicAttackCooldownReset.cancel();
			}
			basicAttackCooldownReset = new BukkitRunnable() {
				@Override
				public void run() {
					canBasicAttack = true;
					associatedEntity.sendMessage("you can basic again.");
					basicAttackCooldownReset = null;
				}
			}.runTaskLater(Sword.getInstance(), 20);
		} else {
			basicAttackSequenceTimeout = new BukkitRunnable() {
				@Override
				public void run() {
					basicAttackStage = 0;
					basicAttackSequenceTimeout = null;
				}
			}.runTaskLater(Sword.getInstance(), 30);
		}
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
