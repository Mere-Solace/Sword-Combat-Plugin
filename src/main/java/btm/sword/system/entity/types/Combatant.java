package btm.sword.system.entity.types;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.MovementAction;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.UmbralState;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.Prefab;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

    private UmbralBlade umbralBlade;
    private boolean umbralBladeActive;

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
        this.airDashesPerformed = 0;

        this.attrHealth = entity().getAttribute(Attribute.MAX_HEALTH);
        if (attrHealth != null) attrHealth.setBaseValue(combatProfile.getStat(AspectType.SHARDS).getValue());

        this.attrAbsorption = entity().getAttribute(Attribute.MAX_ABSORPTION);
        if (attrAbsorption != null) attrAbsorption.setBaseValue(combatProfile.getStat(AspectType.TOUGHNESS).getValue());

        this.attrArmor = entity().getAttribute(Attribute.ARMOR);
        if (attrArmor != null) attrArmor.setBaseValue(combatProfile.getStat(AspectType.FORM).getValue());

        this.attrInteractionRange = entity().getAttribute(Attribute.ENTITY_INTERACTION_RANGE);

        this.umbralBladeActive = false;
    }

    /**
     * Called when the entity dies.
     * Cleans up the sheathed sword display entity.
     */
    @Override
    public void onDeath() {
        super.onDeath();
        removeUmbralBlade();
    }

    @Override
    protected void onTick() {
        super.onTick();

        handleUmbralBladeTick();
    }

    public void handleUmbralBladeTick() {
        if (entity().isValid() && umbralBlade == null && !isUmbralBladeActive()) {
            message("Starting Umbral Blade");
            startUmbralBlade();
            return;
        }

        if (!entity().isValid()) {
            message("Ending Umbral Blade");
            endUmbralBlade();
        }

        if (umbralBlade.getWeaponDisplay() != null && isUmbralBladeActive() && umbralBlade.getState().equals(UmbralState.SHEATHED)) {
            message("Updating UmbralBlade to Sheathed Position");
            updateUmbralBlade();
        }

        if (ticks % 2 == 0) {
            if ((umbralBlade.getWeaponDisplay() == null || umbralBlade.getWeaponDisplay().isDead()) && isUmbralBladeActive()) {
                message("Restarting UmbralBlade Display");
                restartUmbralBladeDisplay();
            }
        }
    }

    public void startUmbralBlade() {
        Combatant pass = this;
        new BukkitRunnable() {
            @Override
            public void run() {
                umbralBlade = new UmbralBlade(pass, ItemStack.of(Material.STONE_SWORD));
                setUmbralBladeActive(true);
            }
        }.runTaskLater(Sword.getInstance(), 4L);
    }

    /**
     * Sets the currently active ability cast task.
     *
     * @param abilityCastTask the BukkitTask representing the ability cast
     */
    public void setCastTask(BukkitTask abilityCastTask) {
        this.abilityCastTask = abilityCastTask;
    }

//    /**
//     * Recreates and reinitializes the entity's sheathed weapon display.
//     * <p>
//     * This method first marks the sheathed weapon as not ready using {@link #setSheathedActive(boolean)}.
//     * After a short delay (5 ticks), it verifies that the entity is still valid and online,
//     * ensures the entity's current chunk is loaded, and then spawns a new {@link ItemDisplay}
//     * entity at the entity's location. This entity visually represents the entity's
//     * sheathed weapon (currently a {@link Material#STONE_SWORD}).
//     * </p>
//     *
//     * <p>
//     * The spawned {@link ItemDisplay} is given a custom {@link org.bukkit.util.Transformation}
//     * that positions and rotates the weapon relative to the entity's model, making it appear
//     * naturally attached to their side or back. Once the entity is created, the sheathed
//     * state is marked as ready again.
//     * </p>
//     *
//     * <p><b>Threading:</b> Executed on the main server thread using {@link Bukkit#getScheduler()}.</p>
//     *
//     * @implNote The delayed execution (5 ticks) ensures that entity and world state
//     *           are stable before spawning the entity, which avoids null or invalid references
//     *           that might occur immediately after entity load or teleport events.
//     *
//     * @see ItemDisplay
//     * @see World#spawnEntity(org.bukkit.Location, org.bukkit.entity.EntityType)
//     * @see #setSheathedActive(boolean)
//     */
    public void restartUmbralBladeDisplay() {
        setUmbralBladeActive(false);
        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
            if (!entity().isValid()) return;

            World world = entity().getWorld();
            Location loc = entity().getLocation();

            if (!loc.getChunk().isLoaded()) loc.getChunk().load();

            umbralBlade.setWeaponDisplay((ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY));
            umbralBlade.getWeaponDisplay().setItemStack(new ItemStack(Material.STONE_SWORD)); // TODO: Later - make dynamic
            umbralBlade.getWeaponDisplay().setTransformation(new Transformation(
                    new Vector3f(0.28f, -1.4f, -0.75f),
                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                    new Vector3f(0.85f, 1.5f, 1f),
                    new Quaternionf()
            ));
            umbralBlade.getWeaponDisplay().setPersistent(false);

            entity().addPassenger(umbralBlade.getWeaponDisplay());
            umbralBlade.getWeaponDisplay().setBillboard(Display.Billboard.FIXED);
            setUmbralBladeActive(true);
        }, 2L);
    }

//    /**
//     * Gradually updates the position and orientation of the entity's sheathed weapon display
//     * to maintain alignment with the entity's current facing direction and location.
//     * <p>
//     * This method performs multiple delayed updates (controlled by the loop count {@code x})
//     * to achieve a smooth visual interpolation using {@link DisplayUtil#smoothTeleport(org.bukkit.entity.Display, int)}.
//     * Each iteration schedules a task via {@link SwordScheduler#runBukkitTaskLater(Runnable, int, java.util.concurrent.TimeUnit)}
//     * that repositions the {@link UmbralBlade#getWeaponDisplay()} {@link org.bukkit.entity.ItemDisplay} entity relative to the entity's location.
//     * <p>
//     * The display entity is temporarily attached as a passenger to the entity using
//     * {@link org.bukkit.entity.Player#addPassenger(org.bukkit.entity.Entity)} to ensure its position follows the entity.
//     * The direction is recalculated each update using {@link #getFlatDir()} for consistent orientation.
//     * <p>
//     * Once the update sequence completes, the sheathed weapon display is typically finalized by setting
//     * its billboard mode to {@link org.bukkit.entity.Display.Billboard#FIXED} and marking it as ready via
//     * {@link #setSheathedActive(boolean)}.
//     *
//     * @implNote The update uses a fixed delay of {@code 50/x} milliseconds between each scheduled iteration,
//     * producing a brief animation-like effect as the weapon display aligns to the entity's orientation.
//     *
//     * @see DisplayUtil#smoothTeleport(org.bukkit.entity.Display, int)
//     * @see SwordScheduler#runBukkitTaskLater(Runnable, int, java.util.concurrent.TimeUnit)
//     * @see org.bukkit.entity.Display.Billboard#FIXED
//     * @see org.bukkit.entity.Player#addPassenger(org.bukkit.entity.Entity)
//     * @see #getFlatDir()
//     * @see #setSheathedActive(boolean)
//     */
    public void updateUmbralBlade() {
        int x = 3;
        for (int i = 0; i < x; i++) {
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    DisplayUtil.smoothTeleport(umbralBlade.getWeaponDisplay(), 2);
                    umbralBlade.getWeaponDisplay().teleport(entity().getLocation().setDirection(getFlatDir()));
                    entity().addPassenger(umbralBlade.getWeaponDisplay());
                }
            }, 50/x, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Safely remove the sheathed weapon item display and disallow the re-spawning and updating of it.
     */
    public void endUmbralBlade() {
        removeUmbralBlade();
        setUmbralBladeActive(false);
    }

    /**
     * Safely remove the sheathed item weapon display.
     */
    public void removeUmbralBlade() {
        if (umbralBlade != null && umbralBlade.getWeaponDisplay() != null)
            umbralBlade.getWeaponDisplay().remove();
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
        Prefab.Particles.GRAB_ATTEMPT.display(hitLoc);
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
                        !main.getType().isAir();

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
