package btm.sword.system.entity;

import btm.sword.system.action.MovementAction;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.util.Cache;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

@Getter
@Setter
public abstract class Combatant extends SwordEntity {
	private BukkitTask abilityCastTask = null;
	
	private int airDashesPerformed;
	
	private boolean isGrabbing = false;
	private SwordEntity grabbedEntity;
	
	private ThrownItem thrownItem;
	private ItemStack offHandItemStackDuringThrow;
	private ItemStack mainHandItemStackDuringThrow;
	private boolean attemptingThrow;
	private boolean throwCancelled;
	private boolean throwSuccessful;
	
	public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity, combatProfile);
		airDashesPerformed = 0;
	}

	public void setCastTask(BukkitTask abilityCastTask) {
		this.abilityCastTask = abilityCastTask;
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
		grabbedEntity.hit(this, 0, 0, 5, 15,
				target.getEyeLocation().subtract(self.getEyeLocation()).toVector());
	}
	
	// if the player is not grabbing, is not being grabbed, and is currently not casting an ability,
	// return true, that they CAN perform an action
	public boolean canPerformAction() {
		return abilityCastTask == null && !isGrabbing && !isGrabbed();
	}
	
	public boolean canAirDash() {
		return canPerformAction() && getAirDashesPerformed() < getCombatProfile().getMaxAirDodges();
	}

    public boolean canThrow() {
        ItemStack main = getItemStackInHand(true);
        ItemStack off = getItemStackInHand(false);

        boolean throwable =
                        !main.getType().equals(Material.CROSSBOW) &&
                        !main.getType().equals(Material.BOW) &&
                        !main.getType().isEdible() &&
                        !main.getType().isEmpty();

        return canPerformAction() && throwable && off.getType().equals(Material.SHIELD);
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
}
