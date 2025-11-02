package btm.sword.system.entity.types;

import btm.sword.system.action.MovementAction;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.util.display.Prefab;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Abstract class representing combat-capable entities within the Sword plugin.
 * Extends {@link SwordEntity} with combat-specific state and behaviors such as grabbing,
 * air dashes, throwing, and ability casting.
 * <p>
 * Maintains references to thrown items, ability cast tasks, and tracks input-sensitive mechanics.
 * </p>
 */
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

    private final AttributeInstance attrHealth;
    private final AttributeInstance attrAbsorption;
    private final AttributeInstance attrArmor;
    private final AttributeInstance attrInteractionRange;

    /**
     * Constructs a new Combatant wrapping the given {@link LivingEntity}
     * and using the specified {@link CombatProfile}.
     *
     * @param associatedEntity the Bukkit living entity to associate
     * @param combatProfile the combat profile defining combat stats and settings
     */
    public Combatant(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
        airDashesPerformed = 0;

        attrHealth = entity().getAttribute(Attribute.MAX_HEALTH);
        if (attrHealth != null) attrHealth.setBaseValue(combatProfile.getStat(AspectType.SHARDS).getValue());

        attrAbsorption = entity().getAttribute(Attribute.MAX_ABSORPTION);
        if (attrAbsorption != null) attrAbsorption.setBaseValue(combatProfile.getStat(AspectType.TOUGHNESS).getValue());

        attrArmor = entity().getAttribute(Attribute.ARMOR);
        if (attrArmor != null) attrArmor.setBaseValue(combatProfile.getStat(AspectType.FORM).getValue());

        attrInteractionRange = entity().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
    }

    /**
     * Sets the currently active ability cast task.
     *
     * @param abilityCastTask the BukkitTask representing the ability cast
     */
    public void setCastTask(BukkitTask abilityCastTask) {
        this.abilityCastTask = abilityCastTask;
    }

    /**
     * Initiates a grab action on the specified target {@link SwordEntity}.
     * Applies minor damage to the target, sets grab states, and displays a particle effect.
     *
     * @param target the SwordEntity that is being grabbed
     */
    public void onGrab(SwordEntity target) {
        LivingEntity t = target.entity();
        setGrabbing(true);
        target.setGrabbed(true);
        setGrabbedEntity(target);
        t.damage(0.25, self);
        Prefab.Particles.GRAB_CLOUD.display(t.getLocation().add(new Vector(0, 1, 0)));
    }

    /**
     * Releases the currently grabbed entity without further action.
     */
    public void onGrabLetGo() {
        isGrabbing = false;
        grabbedEntity.setGrabbed(false);
    }

    /**
     * Throws the currently grabbed entity, applying movement and damage effects.
     * Resets grab state and calls {@link MovementAction#toss(Combatant, SwordEntity)}.
     */
    public void onGrabThrow() {
        onGrabHit();

        isGrabbing = false;
        grabbedEntity.setGrabbed(false);
        MovementAction.toss(this, grabbedEntity);
    }

    /**
     * Performs the hit action during a grab, dealing a fixed amount of damage to the grabbed entity
     * and displaying associated particle effects.
     */
    public void onGrabHit() {
        LivingEntity target = grabbedEntity.entity();
        Location hitLoc = target.getLocation().add(0, target.getEyeHeight()*0.5, 0);
        Prefab.Particles.GRAB_HIT_1.display(hitLoc);
        Prefab.Particles.GRAB_HIT_2.display(hitLoc);
        grabbedEntity.hit(this, 0, 0, 5, 15,
                target.getEyeLocation().subtract(self.getEyeLocation()).toVector());
    }

    /**
     * Checks if this combatant can perform an action.
     * <p>
     * This returns {@code true} if the combatant is not currently casting an ability,
     * is not grabbing another entity, and is not grabbed themselves.
     * </p>
     *
     * @return true if able to perform actions, false otherwise
     */
    public boolean canPerformAction() {
        return abilityCastTask == null && !isGrabbing && !isGrabbed();
    }

    /**
     * Checks if this combatant can perform an air dash.
     * Combines action availability with limit on air dash count from {@link CombatProfile}.
     *
     * @return true if air dash is possible, false otherwise
     */
    public boolean canAirDash() {
        return canPerformAction() && getAirDashesPerformed() < getCombatProfile().getMaxAirDodges();
    }

    /**
     * Checks if the combatant can perform a throw action.
     * Requires action availability, main hand holding an appropriate throwable item,
     * and off hand holding a shield.
     *
     * @return true if throwing is possible, false otherwise
     */
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

    /**
     * Resets the count of air dashes performed to zero.
     */
    public void resetAirDashesPerformed() {
        this.airDashesPerformed = 0;
    }

    /**
     * Increments the count of air dashes performed by one.
     */
    public void increaseAirDashesPerformed() {
        airDashesPerformed++;
    }

    /**
     * Calculates an additive value for a stat based on an {@link AspectType}.
     *
     * @param stat the aspect type representing the stat
     * @param max maximum allowed result value
     * @param base base value before addition
     * @param multiplier multiplier applied to the aspect value before addition
     * @return the calculated additive value capped at max
     */
    public double calcValueAdditive(AspectType stat, double max, double base, double multiplier) {
        return Math.min(max, base + (multiplier * aspects.getAspectVal(stat)));
    }

    /**
     * Calculates a reductive value for a stat based on an {@link AspectType}.
     *
     * @param stat the aspect type representing the stat
     * @param min minimum allowed result value
     * @param base base value before reduction
     * @param multiplier multiplier applied to the aspect value before reduction
     * @return the calculated reductive value floored at min
     */
    public double calcValueReductive(AspectType stat, double min, double base, double multiplier) {
        return Math.max(min, base - (multiplier * aspects.getAspectVal(stat)));
    }

    /**
     * Calculates a cooldown duration in milliseconds based on an {@link AspectType}.
     *
     * @param type the aspect type affecting cooldown
     * @param min minimum cooldown in milliseconds
     * @param base base cooldown in milliseconds
     * @param multiplier multiplier applied to aspect value for reduction
     * @return the calculated cooldown duration floored at min
     */
    public long calcCooldown(AspectType type, double min, double base, double multiplier) {
        return (long) Math.max(min, base - (multiplier * aspects.getAspectVal(type)) );
    }
}
