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
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.entity.EntityUtil;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.sound.SoundType;
import btm.sword.utility.sound.SoundUtil;

public class Dash {
    private final Combatant executor;
    private final LivingEntity ex;
    private final int direction;
    private boolean onGround;
    private boolean flatDash;
    private ItemDisplay targetedDisplay = null;
    private DashType dashType;
    private double dashPower;
    private boolean holdingLink;

    private static final Supplier<Double> MAX_STRAIGHT_DASH_DISTANCE = () -> Config.Movement.DASH_MAX_DISTANCE;
    private static final Supplier<Double> NORMAL_ITEM_RAY_WIDTH = () -> Config.Movement.DASH_RAY_HITBOX_RADIUS;
    private static final Supplier<Double> UMBRAL_RAY_WIDTH = () -> 1.2; // TODO: Config

    double FLAT_DASH_PITCH_THRESHOLD = 0.30; // TODO: Config

    public Dash(Combatant executor, int direction) {
        this.executor = executor;
        this.ex = executor.self();
        this.direction = direction;
    }

    private enum DashType {
        FLAT_TO_UMBRAL_BLADE, // onGround, certain pitch and height diff with blade, holding soul link, and targeted umbral blade
        NORMAL_TO_UMBRAL_BLADE, // some checks don't pass from flat: just straight to blade, no initial upward velocity.
        FLAT_TO_ITEM, // onGround, certain pitch and height checks are made, not holding anything.
        NORMAL_TO_ITEM, // some checks from flat don't pass: straight to item
        FLAT_DASH, // onGround with certain pitch checks: either no item targeted or hands are holding wrong item
        DASH, // some checks fail for flat.
        NOTHING // Unset value
    }

    public void execute() {
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

        if (dashType == DashType.NOTHING || (targetedDisplay != null && isDashToItemImpeded())) {
            if (flatDash) {
                dashType = DashType.FLAT_DASH;
            }
            else {
                dashType = DashType.DASH;
            }
        }

        Debug.movement("DashType="+dashType);

        dash();
    }

    public void checkFlatDash() {
        //          |__  < flat always
        //          |/   < flat when backwards and not targeting item, otherwise not flat
        //          |
        //           \   < flat when forwards and not targeting item, otherwise not flat

        boolean belowForwardThreshold = executor.dir().dot(Config.Direction.UP()) < FLAT_DASH_PITCH_THRESHOLD;
        boolean aboveBackwardThreshold = executor.dir().dot(Config.Direction.UP()) > -FLAT_DASH_PITCH_THRESHOLD * Config.Movement.DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER;

        flatDash = onGround && (
            (direction > 0 && belowForwardThreshold) ||
                (direction <= 0 && aboveBackwardThreshold)
        );
    }

    public ItemDisplay raycastForUmbralBlade() {
        ItemDisplay itemDisplay = (ItemDisplay) HitboxUtil.ray(
            executor.eyeLoc(),
            executor.dir(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            UMBRAL_RAY_WIDTH.get(),
            entity ->  entity instanceof ItemDisplay id &&
                    !entity.isDead() &&
                    InteractiveItemArbiter.checkIfInteractive(id) &&
                    !InteractiveItemArbiter.isImpaling(executor, id) &&
                    InteractiveItemArbiter.isUmbralBlade(id)
        );

        if (itemDisplay != null) Debug.movement("Umbral Blade Targeted.. itemDisplay=" + itemDisplay.getName());

        return itemDisplay;
    }

    public ItemDisplay raycastForNormalItem() {
        ItemDisplay itemDisplay = (ItemDisplay) HitboxUtil.ray(
            executor.eyeLoc(),
            executor.dir(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            NORMAL_ITEM_RAY_WIDTH.get(),
            entity ->  entity instanceof ItemDisplay id &&
                !entity.isDead() &&
                InteractiveItemArbiter.checkIfInteractive(id) &&
                !InteractiveItemArbiter.isImpaling(executor, id) &&
                !InteractiveItemArbiter.isUmbralBlade(id)
        );

        if (itemDisplay != null) Debug.movement("Normal Item Targeted.. itemDisplay=" + itemDisplay.getName());

        return itemDisplay;
    }

    private void dash() {
        dashPower = flatDash ? Config.Movement.DASH_BASE_POWER * 2 : Config.Movement.DASH_BASE_POWER; // TODO: Config the 2 here

        dashEffects();

        switch (dashType) {
            case NOTHING -> {}
            case DASH, FLAT_DASH -> plainDash(); // can actually go back or forward
            case FLAT_TO_ITEM, NORMAL_TO_ITEM, FLAT_TO_UMBRAL_BLADE, NORMAL_TO_UMBRAL_BLADE -> dashToItem();
            // ^^ will always go towards the item targeted, and then jump backwards/forwards depending on input.
        }

        if (!onGround) executor.increaseAirDashesPerformed();
    }

    private void plainDash() {
        executor.setDashDirection(executor.dir().multiply(direction));
        double dashMag = dashPower * direction;
        Vector d = flatDash ? executor.getFlatDir() : executor.dir();
        executor.setVelocity(d.multiply(dashMag).add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_BOOST)));
    }

    private Vector calcVectorToItem() {
        return targetedDisplay.getLocation().toVector().subtract(executor.getChestLocation().toVector());
    }

    private Vector calcPostRetrievalVector() {
        return executor.getFlatDir().multiply(direction * Config.Movement.DASH_FORWARD_MULTIPLIER)
            .add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));
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
        executor.setVelocity(executor.dir().multiply(Math.log(distanceToItem/2))); //  TODO: Config the 1/2
    }

    // Dash up first, then dash to the item
    private void performFlatDashToItem(double distanceToItem) {
        double heightDiff = targetedDisplay.getLocation().getY() - ex.getLocation().getY();

        if (!holdingLink &&
            heightDiff <= Config.Movement.DASH_FLAT_HEIGHT_UPPER &&
            heightDiff > Config.Movement.DASH_FLAT_HEIGHT_LOWER) {
            executor.setVelocity(executor.dir()
                .add(Config.Direction.UP().multiply(Config.Movement.DASH_FLAT_ITEM_DASH_UPWARD_SCALER)));
        }
        SwordScheduler.runBukkitTaskLater(() ->
                executor.setVelocity(executor.dir().multiply(Math.log(distanceToItem * Config.Movement.DASH_FLAT_ITEM_DASH_DISTANCE_SCALER))),
            holdingLink ? 0 : 100, TimeUnit.MILLISECONDS // TODO: Config the 100
        );
    }

    private void scheduleCheckForItemPickup() {
        int period = 50; // TODO: Config
        TimeArbiter.runFixedIterationTaskTimer(
            null, null,
            0, period, 1500/period, // TODO: Config
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
        new ParticleWrapper(Particle.BLOCK, // TODO: Prefab the whole particle wrapper
            Config.Movement.DASH_PARTICLE_COUNT,
            Config.Movement.DASH_PARTICLE_SPREAD_X,
            Config.Movement.DASH_PARTICLE_SPREAD_Y,
            Config.Movement.DASH_PARTICLE_SPREAD_Z,
            blockData).display(ex.getLocation());
        Prefab.Particles.GRAB_ATTEMPT.display(targetedDisplay.getLocation());
        SoundUtil.playSound(ex, SoundType.ENTITY_ENDER_DRAGON_FLAP, Config.Movement.DASH_FLAP_SOUND_VOLUME, Config.Movement.DASH_FLAP_SOUND_PITCH);
        SoundUtil.playSound(ex, SoundType.ENTITY_PLAYER_ATTACK_SWEEP, Config.Movement.DASH_SWEEP_SOUND_VOLUME, Config.Movement.DASH_SWEEP_SOUND_PITCH);
        executor.setVelocity(calcPostRetrievalVector());
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

    public static void scheduleParticleDisplay(Combatant executor) {
        int MAX_ITERATIONS = 5;
        int[] iteration = {0};
        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> {
                new ParticleWrapper(Particle.CLOUD, MAX_ITERATIONS + 1 - iteration[0], 0.25, 0.25, 0.25, 0)
                    .display(executor.getLocation());
                new ParticleWrapper(Particle.SMOKE, 3 * (MAX_ITERATIONS + 1 - iteration[0]), 0.3, 0.3, 0.3, 0)
                    .display(executor.getLocation());

                iteration[0]++;
            },
            Config.Movement.DASH_VELOCITY_TASK_DELAY, Config.Movement.DASH_VELOCITY_TASK_PERIOD,
            MAX_ITERATIONS,
            MovementAction.class, "dashStraight", null
        );
    }
}
