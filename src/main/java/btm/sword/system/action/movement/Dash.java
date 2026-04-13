package btm.sword.system.action.movement;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItem;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.WaitingState;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.entity.EntityUtil;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.sound.SoundUtil;
import btm.sword.utility.sound.SwordSoundType;

/**
 * Executes a directional dash for a {@link Combatant}.
 * <p>
 * Resolves which {@link DashType} applies based on ground state, pitch, held items, and
 * raycasts for nearby interactive items or the UmbralBlade, then launches the appropriate
 * movement impulse via {@link btm.sword.system.control.TimeArbiter}.
 * </p>
 *
 * <p>Direction is encoded as an integer (e.g. {@code 1} = forward, {@code -1} = backward);
 * see the input registrar for the mapping.</p>
 */
public class Dash {
    private final Combatant executor;
    private final LivingEntity ex;
    private final int direction;
    private boolean onGround;
    private boolean flatDash;
    private ItemDisplay targetedDisplay = null;
    private UmbralBlade targetedBlade = null;
    private DashType dashType;
    private double dashPower;
    private boolean holdingLink;
    private boolean itemRetrieved;

    private static final Supplier<Double> MAX_STRAIGHT_DASH_DISTANCE = () -> Config.Movement.DASH_MAX_DISTANCE;
    private static final Supplier<Double> NORMAL_ITEM_RAY_WIDTH = () -> Config.Movement.DASH_RAY_HITBOX_RADIUS;
    private static final Supplier<Double> UMBRAL_RAY_WIDTH = () -> Config.Movement.DASH_UMBRAL_RAY_HITBOX_RADIUS;

    double flatDashPitchThreshold = Config.Movement.FLAT_DASH_PITCH_THRESHOLD;

    /**
     * Creates a Dash action for the given combatant and direction.
     *
     * @param executor  the entity performing the dash
     * @param direction dash direction multiplier (positive = forward, negative = backward)
     */
    public Dash(Combatant executor, int direction) {
        this.executor = executor;
        this.ex = executor.self();
        this.direction = direction;
    }

    /** Classifies the kind of dash to execute based on context (ground state, held item, target). */
    private enum DashType {
        FLAT_TO_UMBRAL_BLADE, // onGround, certain pitch and height diff with blade, holding soul link, and targeted umbral blade
        NORMAL_TO_UMBRAL_BLADE, // some checks don't pass from flat: just straight to blade, no initial upward velocity.
        FLAT_TO_ITEM, // onGround, certain pitch and height checks are made, not holding anything.
        NORMAL_TO_ITEM, // some checks from flat don't pass: straight to item
        FLAT_DASH, // onGround with certain pitch checks: either no item targeted or hands are holding wrong item
        DASH, // some checks fail for flat.
        NOTHING // Unset value
    }

    /**
     * Resolves the dash type and fires the appropriate movement impulse.
     * Must be called from the main server thread.
     */
    public void execute() {
        itemRetrieved = false;
        dashType = DashType.NOTHING;
        onGround = EntityUtil.isOnGround(ex);
        holdingLink = executor.holdingSoulLink();
        boolean holdingNothing = executor.holdingNothing();
        checkFlatDash();

        // only apply the upward force if targeting a normal item
        if (holdingNothing) {
            targetedDisplay = raycastForNormalItem();
            if (targetedDisplay != null) {
                if (flatDash) {
                    dashType = DashType.FLAT_TO_ITEM;
                }
                else {
                    dashType = DashType.NORMAL_TO_ITEM;
                }
            }
        }
        else if (holdingLink) {
            targetedDisplay = raycastForUmbralBlade();
            if (targetedDisplay != null) {
                if (flatDash) {
                    dashType = DashType.FLAT_TO_UMBRAL_BLADE;
                }
                else {
                    dashType = DashType.NORMAL_TO_UMBRAL_BLADE;
                }
            }
        }
        // Holding an ability item — only pick up matching ability projectiles
        else if (executor instanceof SwordPlayer sp && !sp.notHoldingAbilityItem()) {
            targetedDisplay = raycastForAbilityProjectile();
            if (targetedDisplay != null) {
                dashType = flatDash ? DashType.FLAT_TO_ITEM : DashType.NORMAL_TO_ITEM;
            }
        }

        if (dashType == DashType.NOTHING || (targetedDisplay != null && isDashToItemImpeded())) {
            if (flatDash) {
                dashType = DashType.FLAT_DASH;
            }
            else {
                dashType = DashType.DASH;
            }
        }

        Debug.movement("DashType=" + dashType);

        dash();
    }

    /** Determines whether the dash should be flat (horizontal) based on pitch and ground state. */
    public void checkFlatDash() {
        //          |__  < flat always
        //          |/   < flat when backwards and not targeting item, otherwise not flat
        //          |
        //           \   < flat when forwards and not targeting item, otherwise not flat

        boolean belowForwardThreshold = executor.dir().dot(Config.Direction.up()) < flatDashPitchThreshold;
        boolean aboveBackwardThreshold = executor.dir().dot(Config.Direction.up()) > -flatDashPitchThreshold * Config.Movement.DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER;

        flatDash = onGround && (
            (direction > 0 && belowForwardThreshold) ||
                (direction <= 0 && aboveBackwardThreshold)
        );
    }

    /** Raycasts forward to detect a thrown UmbralBlade display entity the executor can retrieve. */
    public ItemDisplay raycastForUmbralBlade() {
        ItemDisplay itemDisplay = (ItemDisplay) HitboxUtil.ray(
            executor.eyeLoc(),
            executor.dir(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            UMBRAL_RAY_WIDTH.get(),
            entity ->  {
                if (!(entity instanceof ItemDisplay id)) return false;

                InteractiveItem targetedItem = InteractiveItemArbiter.get(id);

                if (targetedItem == null) return false;

                if (targetedItem instanceof UmbralBlade umbralBlade) {
                    targetedBlade = umbralBlade;
                }
                else return false;

                return !entity.isDead() && InteractiveItemArbiter.notImpaled(executor, id);
            }
        );

        if (itemDisplay != null) Debug.movement("Umbral Blade Targeted.. itemDisplay=" + itemDisplay.getName());

        return itemDisplay;
    }

    /** Raycasts forward to detect any non-blade interactive item display entity within dash range. */
    public ItemDisplay raycastForNormalItem() {
        ItemDisplay itemDisplay = (ItemDisplay) HitboxUtil.ray(
            executor.eyeLoc(),
            executor.dir(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            NORMAL_ITEM_RAY_WIDTH.get(),
            entity ->  entity instanceof ItemDisplay id &&
                !entity.isDead() &&
                InteractiveItemArbiter.checkIfInteractive(id) &&
                InteractiveItemArbiter.notImpaled(executor, id) &&
                !InteractiveItemArbiter.isUmbralBlade(id)
        );

        if (itemDisplay != null) Debug.movement("Normal Item Targeted.. itemDisplay=" + itemDisplay.getName());

        return itemDisplay;
    }

    /**
     * Raycasts for ability projectiles only (items tagged with {@code ABILITY_ID_KEY}).
     * Used when the executor is holding an ability item and can only pick up matching projectiles.
     */
    public ItemDisplay raycastForAbilityProjectile() {
        ItemDisplay itemDisplay = (ItemDisplay) HitboxUtil.ray(
            executor.eyeLoc(),
            executor.dir(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            NORMAL_ITEM_RAY_WIDTH.get(),
            entity -> entity instanceof ItemDisplay id &&
                !entity.isDead() &&
                InteractiveItemArbiter.checkIfInteractive(id) &&
                InteractiveItemArbiter.isAbilityProjectile(id)
        );

        if (itemDisplay != null) Debug.movement("Ability Projectile Targeted.. itemDisplay=" + itemDisplay.getName());

        return itemDisplay;
    }

    private void dash() {
        dashPower = flatDash ? Config.Movement.DASH_BASE_POWER * Config.Movement.FLAT_DASH_POWER_MULTIPLIER : Config.Movement.DASH_BASE_POWER;

        dashEffects();

        switch (dashType) {
            case NOTHING -> {}
            case DASH, FLAT_DASH -> plainDash(); // can actually go back or forward
            case FLAT_TO_ITEM, NORMAL_TO_ITEM, FLAT_TO_UMBRAL_BLADE, NORMAL_TO_UMBRAL_BLADE -> dashToItem();
            // ^^ will always go towards the item targeted, and then jump backwards/forwards depending on input.
        }

        if (!onGround && !executor.isSubmergedInLiquid()) {
            executor.increaseAirDashesPerformed();
        }
    }

    private void plainDash() {
        executor.setDashDirection(executor.dir().multiply(direction));
        double dashMag = dashPower * direction;
        Vector d = flatDash ? executor.getFlatDir() : executor.dir();
        executor.setVelocity(d.multiply(dashMag).add(Config.Direction.up().multiply(Config.Movement.DASH_UPWARD_BOOST)));
    }

    private Vector calcVectorToItem() {
        return targetedDisplay.getLocation().toVector().subtract(executor.getChestLocation().toVector());
    }

    private Vector calcPostRetrievalVector() {
        return executor.getFlatDir().multiply(direction * Config.Movement.DASH_FORWARD_MULTIPLIER)
            .add(Config.Direction.up().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));
    }

    private void dashToItem() {
        Vector toItem = calcVectorToItem();
        double length = toItem.length();

        if (flatDash) {
            performFlatDashToItem(length);
        }
        else {
            performNormalDashToItem(length);
        }

        scheduleCheckForItemPickup();
    }

    private void performNormalDashToItem(double distanceToItem) {
        executor.setVelocity(executor.dir().multiply(Math.log(distanceToItem / Config.Movement.DASH_NORMAL_ITEM_DISTANCE_DIVISOR)));
    }

    // Dash up first, then dash to the item
    private void performFlatDashToItem(double distanceToItem) {
        double heightDiff = targetedDisplay.getLocation().getY() - ex.getLocation().getY();

        Debug.movement("heightDiff=" + heightDiff);

        boolean umbralHeightBoost = !holdingLink || targetedBlade.inState(WaitingState.class) || targetedBlade.inState(LodgedState.class);

        final Vector exDir = executor.dir();

        if (umbralHeightBoost &&
            heightDiff <= Config.Movement.DASH_FLAT_HEIGHT_UPPER &&
            heightDiff > Config.Movement.DASH_FLAT_HEIGHT_LOWER) {

            Vector dashVelocity = exDir.add(
                Config.Direction.up().multiply(Config.Movement.DASH_FLAT_ITEM_DASH_UPWARD_SCALER)
            );
            executor.setVelocity(dashVelocity);
        }

        final Vector toItem = calcVectorToItem().normalize();
        if (VectorUtil.isBroken(toItem)) return;

        if (!itemRetrieved && distanceToItem > 3) { // TODO: COnfig
            SwordScheduler.runBukkitTaskLater(() -> // Velocity applied after the initial height boost
                    executor.setVelocity(toItem.multiply(Math.log(distanceToItem * Config.Movement.DASH_FLAT_ITEM_DASH_DISTANCE_SCALER))),
                !umbralHeightBoost ? 0 : Config.Movement.FLAT_DASH_HEIGHT_BOOST_DELAY_MS, TimeUnit.MILLISECONDS
            );
        }
    }

    private void scheduleCheckForItemPickup() {
        int period = Config.Movement.DASH_ITEM_CHECK_PERIOD_MS;
        TimeArbiter.runFixedIterationTaskTimer(
            null, null,
            0, period, Config.Movement.DASH_ITEM_CHECK_DURATION_MS / period,
            Dash.class, "scheduleCheckForItemPickup",
            this::failureToReach,
            new PredicateRunnablePair(
                () -> calcVectorToItem().lengthSquared() < Config.Movement.DASH_GRAB_DISTANCE_SQUARED,
                this::onItemRetrieval
            )
        );
    }

    private void onItemRetrieval() {
        BlockData blockData = ex.getLocation().add(new Vector(0, Config.Movement.DASH_BLOCK_CHECK_OFFSET_Y, 0)).getBlock().getBlockData();
        new ParticleWrapper(() -> Particle.BLOCK,
            () -> Config.Movement.DASH_PARTICLE_COUNT,
            () -> Config.Movement.DASH_PARTICLE_SPREAD_X,
            () -> Config.Movement.DASH_PARTICLE_SPREAD_Y,
            () -> Config.Movement.DASH_PARTICLE_SPREAD_Z)
            .withBlockData(() -> blockData).display(ex.getLocation());
        Prefab.Particles.GRAB_ATTEMPT.display(targetedDisplay.getLocation());
        SoundUtil.playSound(ex, SwordSoundType.ENTITY_ENDER_DRAGON_FLAP, Config.Movement.DASH_FLAP_SOUND_VOLUME, Config.Movement.DASH_FLAP_SOUND_PITCH);
        SoundUtil.playSound(ex, SwordSoundType.ENTITY_PLAYER_ATTACK_SWEEP, Config.Movement.DASH_SWEEP_SOUND_VOLUME, Config.Movement.DASH_SWEEP_SOUND_PITCH);
        executor.setVelocity(calcPostRetrievalVector());
        itemRetrieved = true;
        InteractiveItemArbiter.onGrab(targetedDisplay, executor); // here is where the display is taken care of
    }

    private void failureToReach() {
        Vector v = ex.getVelocity();
        double damping = Config.Movement.DASH_VELOCITY_DAMPING;
        ex.setVelocity(new Vector(v.getX() * damping, v.getY() * damping, v.getZ() * damping));
    }

    private void dashEffects() {
        addDashSpeedPotion();
        scheduleParticleDisplay(executor);
    }

    private void addDashSpeedPotion() {
        Prefab.PotionEffects.DASH_SPEED.apply(executor);
    }

    @SuppressWarnings("all")
    private boolean isDashToItemImpeded() {
        RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
            ex.getLocation().add(new Vector(0, Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y, 0)),
            targetedDisplay.getLocation().subtract(ex.getLocation()).toVector().normalize(),
            MAX_STRAIGHT_DASH_DISTANCE.get() / 2,
            FluidCollisionMode.NEVER,
            true,
            block -> !block.isCollidable());
        return
            impedanceCheck != null &&
                impedanceCheck.getHitBlock() != null &&
                impedanceCheck.getHitBlock().isCollidable();
    }

    /** Schedules a burst of cloud and smoke particles at the executor's location to visualise a dash. */
    public static void scheduleParticleDisplay(Combatant executor) {
        int maxIterations = 5;
        int[] iteration = {0};
        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> {
                new ParticleWrapper(() -> Particle.CLOUD, () -> maxIterations + 1 - iteration[0], () -> 0.25, () -> 0.25, () -> 0.25)
                    .withSpeed(() -> 0.0).display(executor.getLocation());
                new ParticleWrapper(() -> Particle.SMOKE, () -> 3 * (maxIterations + 1 - iteration[0]), () -> 0.3, () -> 0.3, () -> 0.3)
                    .withSpeed(() -> 0.0).display(executor.getLocation());

                iteration[0]++;
            },
            Config.Movement.DASH_VELOCITY_TASK_DELAY, Config.Movement.DASH_VELOCITY_TASK_PERIOD,
            maxIterations,
            MovementAction.class, "dashStraight", null
        );
    }
}
