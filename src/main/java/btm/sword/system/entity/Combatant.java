package btm.sword.system.entity;

import btm.sword.system.action.MovementAction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.util.Cache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Objects;

public abstract class Combatant extends SwordEntity {
	private BukkitTask abilityCastTask = null;
	
	private int airDashesPerformed;
	
	private boolean isGrabbing = false;
	private SwordEntity grabbedEntity;
	
	public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity, combatProfile);
		airDashesPerformed = 0;
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
		setGrabbing(true);
		target.setGrabbed(true);
		setGrabbedEntity(target);
		t.damage(0.25, self);
		Cache.grabCloudParticle.display(t.getLocation().add(new Vector(0, 1, 0)));
	}
	
	public void onGrabLetGo() {
		isGrabbing = false;
		grabbedEntity.setGrabbed(false);
	}
	
	public void onGrabThrow() {
		onGrabHit();
		
		isGrabbing = false;
		grabbedEntity.setGrabbed(false);
		MovementAction.toss(this, grabbedEntity);
	}
	
	public void onGrabHit() {
		LivingEntity target = grabbedEntity.entity();
		Location hitLoc = target.getLocation().add(0, target.getEyeHeight()*0.5, 0);
		Cache.grabHitParticle.display(hitLoc);
		Cache.grabHitParticle2.display(hitLoc);
		target.damage(1, self);
	}
	
	// if the player is not grabbing, is not being grabbed, and is currently not casting an ability,
	// return true, that they CAN perform an action
	public boolean canPerformAction() {
		return abilityCastTask == null && !isGrabbing && !isGrabbed();
	}
	
	public boolean canAirDash() {
		return canPerformAction() && getAirDashesPerformed() < getCombatProfile().getMaxAirDodges();
	}
	
	public int getAirDashesPerformed() {
		return airDashesPerformed;
	}
	
	public void resetAirDashesPerformed() {
		this.airDashesPerformed = 0;
	}
	
	public void increaseAirDashesPerformed() {
		airDashesPerformed++;
	}
	
	public double calcValueAdditive(AspectType stat, double max, double base, double multiplier) {
		return Math.min(max, base + (multiplier * aspects.getAspectVal(stat)));
	}
	
	public double calcValueReductive(AspectType stat, double min, double base, double multiplier) {
		return Math.max(min, base - (multiplier * aspects.getAspectVal(stat)));
	}
	
	public long calcCooldown(AspectType type, double min, double base, double multiplier) {
		return (long) Math.max(min, base - (multiplier * aspects.getAspectVal(type)) );
	}
	
	public Material getItemInMainHand() {
		if (self instanceof Player) {
			return ((Player) self).getInventory().getItemInMainHand().getType();
		}
		return Objects.requireNonNull(self.getEquipment()).getItemInMainHand().getType();
	}
	
	public void message(String message) {
		self.sendMessage(message);
	}
}
