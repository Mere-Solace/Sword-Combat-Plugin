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
	
	public boolean cannotPerformAction() {
		return !isGrabbing && !isGrabbed();
	}
	// for every runnable, must reset task to null when finished.
	public boolean cannotPerformExclusiveAction() {
		return cannotPerformAction() && abilityTask != null;
	}
	
	public void performBasicAttack() {
	
	}
	
	public void throwGrabbedEntity() {
		abilityTask.cancel();
		isGrabbing = false;
		Bukkit.getScheduler().runTaskLater(Sword.getInstance(), MovementAction.toss(this, grabbedEntity), 2);
	}
}
