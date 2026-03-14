package btm.sword.system.action.movement;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
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

    private static final Supplier<Double> MAX_STRAIGHT_DASH_DISTANCE = () -> Config.Movement.DASH_MAX_DISTANCE;
    double FLAT_DASH_PITCH_THRESHOLD = 0.20;

    public Dash(Combatant executor, int direction) {
        this.executor = executor;
        this.ex = executor.self();
        this.direction = direction;
    }

    public void execute() {
        onGround = EntityUtil.isOnGround(ex);
        dashStraight();
    }


    private void umbralDash(Combatant executor, int direction) {

    }

    private void dashStraight() {
        LivingEntity ex = executor.self();
        Location eyeLoc = executor.eyeLoc();

        // Set dash direction and apply speed buff
        executor.setDashDirection(ex.getEyeLocation().getDirection().multiply(direction));

        // Try to find an interactive item to dash-to/ grab
        ItemDisplay targetedItem = findTargetedItem(eyeLoc);

        // if grounded and looking
        flatDash = onGround && (
            (direction > 0 && executor.dir().dot(Config.Direction.UP()) < FLAT_DASH_PITCH_THRESHOLD) ||
                (direction <= 0 && executor.dir().dot(Config.Direction.UP()) > -FLAT_DASH_PITCH_THRESHOLD * Config.Movement.DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER)
        );

        Debug.movement("flatDash=" + flatDash);

        // Handle item dash/grab logic separately; if it succeeds, return early
        if (handleTargetedItemDash(targetedItem)) return;

        // Default straight dash behavior
        double dashPower = flatDash ? Config.Movement.DASH_BASE_POWER * 2 : Config.Movement.DASH_BASE_POWER;
        double dashMag = dashPower * direction;

        Vector dir = flatDash ? executor.getFlatDir() : executor.dir();

        performDash(dir, dashMag);

        if (!onGround) executor.increaseAirDashesPerformed();
    }

    private void addDashSpeedPotion() {
        Prefab.PotionEffects.DASH_SPEED.apply(executor);
    }

    private ItemDisplay findTargetedItem(Location eyeLoc) {
        if (executor.getItemStackInHand(true).isEmpty() || executor.holdingSoulLink()) {
            return retrieveTargetedItemDisplayIfInteractive(eyeLoc, executor.self());
        }
        return null;
    }

    private boolean handleTargetedItemDash(Entity targetedItem) {
        if (targetedItem instanceof ItemDisplay display &&
            InteractiveItemArbiter.isUmbralBlade(display) &&
            executor.holdingSoulLink()) {
            return dashToItem(display, true);
        }

        boolean shouldDash = !executor.holdingSoulLink();

        if (shouldDash &&
            targetedItem instanceof ItemDisplay id &&
            !id.isDead() &&
            !id.getItemStack().isEmpty()) {
            return dashToItem(id, false);
        }

        return false;
    }

    private void performDash(Vector direction, double effectivePower) {
        addDashSpeedPotion();
        executor.setVelocity(direction.multiply(effectivePower).add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_BOOST)));
        scheduleParticleDisplay(executor);
    }

    private ItemDisplay retrieveTargetedItemDisplayIfInteractive(Location eyeLocation, LivingEntity dashingEntity) {
        return (ItemDisplay) HitboxUtil.ray(
            eyeLocation,
            eyeLocation.getDirection(),
            MAX_STRAIGHT_DASH_DISTANCE.get(),
            Config.Movement.DASH_RAY_HITBOX_RADIUS,
            entity -> (entity.getType() == EntityType.ITEM_DISPLAY &&
                !entity.isDead() &&
                entity instanceof ItemDisplay id &&
                InteractiveItemArbiter.checkIfInteractive(id)) &&
                !InteractiveItemArbiter.isImpaling(SwordEntityArbiter.get(dashingEntity), id));
    }

    @SuppressWarnings("all")
    private boolean dashToItem(ItemDisplay itemDisplay, boolean umbral) {
        scheduleParticleDisplay(executor);

        if (isDashToItemImpeded(itemDisplay))
            return false;

        double length = itemDisplay.getLocation().subtract(ex.getEyeLocation()).length();

        // flat dashing was for making dashing strong when the player would be dashing across the ground
        // to mitigate the effects of friction from the ground.
        // Since dashing to an item in the air is only a forward action,
        // we should only check if the dash is a 'flat dash' if the direction is forward

        if (flatDash && !umbral) {
            performFlatDashToItemFromGround(length);
        }
        else {
            executor.setVelocity(executor.dir()
                .multiply(Math.log(length/2)));
        }
        Vector u = executor.getFlatDir().multiply(direction * Config.Movement.DASH_FORWARD_MULTIPLIER)
            .add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));

        int delay = flatDash ?
            (int) (Config.Movement.DASH_GRAB_CHECK_DELAY * 1.5) :
            Config.Movement.DASH_GRAB_CHECK_DELAY;

        SwordScheduler.runBukkitTaskLater(() -> {
            if (itemDisplay.getLocation().subtract(ex.getEyeLocation()).lengthSquared() < Config.Movement.DASH_GRAB_DISTANCE_SQUARED) {
                BlockData blockData = ex.getLocation().add(new Vector(0, Config.Movement.DASH_BLOCK_CHECK_OFFSET_Y, 0)).getBlock().getBlockData();
                new ParticleWrapper(Particle.BLOCK,
                    Config.Movement.DASH_PARTICLE_COUNT,
                    Config.Movement.DASH_PARTICLE_SPREAD_X,
                    Config.Movement.DASH_PARTICLE_SPREAD_Y,
                    Config.Movement.DASH_PARTICLE_SPREAD_Z,
                    blockData).display(ex.getLocation());
                Prefab.Particles.GRAB_ATTEMPT.display(itemDisplay.getLocation());
                SoundUtil.playSound(ex, SoundType.ENTITY_ENDER_DRAGON_FLAP, Config.Movement.DASH_FLAP_SOUND_VOLUME, Config.Movement.DASH_FLAP_SOUND_PITCH);
                SoundUtil.playSound(ex, SoundType.ENTITY_PLAYER_ATTACK_SWEEP, Config.Movement.DASH_SWEEP_SOUND_VOLUME, Config.Movement.DASH_SWEEP_SOUND_PITCH);
                executor.setVelocity(u);
                InteractiveItemArbiter.onGrab(itemDisplay, executor); // here is where the display is taken care of
            }
            else {
                Vector v = ex.getVelocity();
                double damping = Config.Movement.DASH_VELOCITY_DAMPING;
                ex.setVelocity(new Vector(v.getX() * damping, v.getY() * damping, v.getZ() * damping));
            }
        }, delay, TimeUnit.MILLISECONDS);

        return true;
    }

    private void performFlatDashToItemFromGround(double distanceToItem) {
        executor.setVelocity(executor.dir()
            .add(Config.Direction.UP().multiply(Config.Movement.DASH_FLAT_ITEM_DASH_UPWARD_SCALER)));

        SwordScheduler.runBukkitTaskLater(() ->
            executor.setVelocity(executor.dir().multiply(Math.log(distanceToItem * Config.Movement.DASH_FLAT_ITEM_DASH_DISTANCE_SCALER))),
            100, TimeUnit.MILLISECONDS
        );
    }

    @SuppressWarnings("all")
    private boolean isDashToItemImpeded(ItemDisplay itemDisplay) {

        RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
            ex.getLocation().add(new Vector(0, Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y, 0)),
            itemDisplay.getLocation().subtract(ex.getLocation()).toVector().normalize(),
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
            MovementAction.class, "dashStraight"
        );
    }
}
