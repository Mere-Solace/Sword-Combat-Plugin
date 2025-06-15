package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

public abstract class Combatant extends SwordEntity {
	CombatProfile combatProfile;
	
	private BukkitTask abilityTask;
	
	private boolean isGrabbing = false;
	private BukkitTask grabTask;
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
	
	public BukkitTask getGrabTask() {
		return grabTask;
	}
	
	public void setGrabTask(BukkitTask grabTask) {
		this.grabTask = grabTask;
	}
	
	public SwordEntity getGrabbedEntity() {
		return grabbedEntity;
	}
	
	public void setGrabbedEntity(SwordEntity grabbedEntity) {
		this.grabbedEntity = grabbedEntity;
	}
	
	public boolean cannotPerformAction() {
		return !isGrabbing && !isBeingGrabbed();
	}
}
