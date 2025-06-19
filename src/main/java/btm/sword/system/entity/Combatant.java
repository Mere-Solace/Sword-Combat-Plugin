package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.MovementAction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.StatType;
import btm.sword.util.Cache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Objects;

public abstract class Combatant extends SwordEntity {
	protected CombatProfile combatProfile;
	
	private BukkitTask abilityCastTask = null;
	
	private boolean isGrabbing = false;
	private SwordEntity grabbedEntity;
	
	public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity);
		this.combatProfile = combatProfile;
	}
	
	public CombatProfile getCombatProfile() {
		return combatProfile;
	}
	
	public BukkitTask getAbilityCastTask() {
		return abilityCastTask;
	}
	
	public void setCastTask(BukkitTask abilityCastTask) {
		this.abilityCastTask = abilityCastTask;
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
		return isGrabbing || isGrabbed() || abilityCastTask != null;
	}
	
	public void endAction() {
		abilityCastTask = null;
	}
	
	public double calcValue(StatType stat, double base, double multiplier) {
		return base + (multiplier * combatProfile.getStat(stat));
	}
	
	public long calcCooldown(long min, long base, StatType stat, double multiplier) {
		return (long) Math.max(min, base - (this.getCombatProfile().getStat(stat) * multiplier));
	}
	
	public Material getItemInMainHand() {
		if (associatedEntity instanceof Player) {
			return ((Player) associatedEntity).getInventory().getItemInMainHand().getType();
		}
		return Objects.requireNonNull(associatedEntity.getEquipment()).getItemInMainHand().getType();
	}
	
	public void message(String message) {
		associatedEntity.sendMessage(message);
	}
}
