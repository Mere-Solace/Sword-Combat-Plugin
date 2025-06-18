package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.MovementAction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.StatType;
import btm.sword.util.Cache;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public abstract class Combatant extends SwordEntity {
	protected CombatProfile combatProfile;
	
	private BukkitTask abilityTask = null;
	private String abilityTaskName = null;
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
	
	public void setAbilityTask(BukkitTask abilityTask, String abilityTaskName) {
		this.abilityTask = abilityTask;
		this.abilityTaskName = abilityTaskName;
	}
	
	public boolean isAbilityTaskFinished() {
		return abilityTask == null;
	}
	
	public String getAbilityTaskName() {
		return abilityTaskName;
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
	public void endAction() {
		abilityTask = null;
		abilityTaskName = null;
	}
	
	public double calcValue(StatType stat, double base, double multiplier) {
		return base + (multiplier * combatProfile.getStat(stat));
	}
}
