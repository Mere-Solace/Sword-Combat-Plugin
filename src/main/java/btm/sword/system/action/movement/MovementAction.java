package btm.sword.system.action.movement;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
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
import btm.sword.system.action.SwordAction;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.sound.SoundType;
import btm.sword.utility.sound.SoundUtil;


/**
 * Provides movement-based actions for {@link Combatant} entities.
 * <p>
 * Includes dashing, directional movement, and throwing/manipulating
 * {@link SwordEntity} targets.
 */
public class MovementAction extends SwordAction {
    private static final Supplier<Double> MAX_STRAIGHT_DASH_DISTANCE = () -> Config.Movement.DASH_MAX_DISTANCE;

    /**
     * Performs a dash action for the executor.
     * <p>
     * The dash moves the executor forward or backward, handles velocity adjustments,
     * particle effects, ground checks, and can target {@link ItemDisplay} entities
     * if within range. Airborne dashes increment the executor's air dash count.
     *
     * @param executor The combatant performing the dash.
     */ // TODO: #125 - This method is illegible, needs refactoring for readability
    public static void dash(Combatant executor, DashDirection direction) {
        boolean onGround = executor.isGrounded();
        Runnable dashConsumer;
        switch (direction) {
            case FORWARD -> dashConsumer = () -> dashStraight(executor, onGround, 1);
            case BACKWARD -> dashConsumer = () -> dashStraight(executor, onGround, -1);
            case RIGHT -> dashConsumer = () -> strafe(executor, onGround, 1);
            case LEFT -> dashConsumer = () -> strafe(executor, onGround, -1);
            default -> {
                return;
            }
        }
        cast (executor, Config.Movement.DASH_CAST_DURATION, dashConsumer);
    }

    private static void dashStraight(Combatant executor, boolean onGround, int direction) {
        LivingEntity ex = executor.self();
        Location o = ex.getEyeLocation();

        // for use in dash attack method.
        executor.setDashDirection(ex.getEyeLocation().getDirection().multiply(direction));

        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Config.Movement.SPEED_DURATION, Config.Movement.SPEED_AMPLIFIER);
        ex.addPotionEffect(speed);

        // check for an item that may be the target of the dash
        Entity targetedItem;
        boolean shouldDash = !executor.holdingSoulLink();

        if (executor.getItemStackInHand(true).isEmpty() || executor.holdingSoulLink()) {
            targetedItem = HitboxUtil.ray(o, o.getDirection(), MAX_STRAIGHT_DASH_DISTANCE.get(), Config.Movement.DASH_RAY_HITBOX_RADIUS,
                entity -> (entity.getType() == EntityType.ITEM_DISPLAY &&
                    !entity.isDead() &&
                    entity instanceof ItemDisplay id &&
                    InteractiveItemArbiter.checkIfInteractive(id)) &&
                    !InteractiveItemArbiter.isImpaling(SwordEntityArbiter.get(ex), id));
        } else {
            targetedItem = null; //  can't dash and grab a new item off the ground if already holding something
        }
        // TODO: #147 clean this nest up
        if (targetedItem instanceof ItemDisplay display &&
            InteractiveItemArbiter.isUmbralBlade(display) &&
            executor.holdingSoulLink()) {
            if (dashToItem(executor, display, direction)) return;
        }

        if (shouldDash &&
            targetedItem instanceof ItemDisplay id &&
            !id.isDead() &&
            !id.getItemStack().isEmpty()) {
            if (dashToItem(executor, id, direction)) return;
        }

        double dashPower = Config.Movement.DASH_BASE_POWER;
        double s = dashPower * direction;
        Vector up = Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_BOOST);

        int[] iteration = {0};
        final int particles = 3;

        Vector eyeDirection = executor.dir();
        boolean flatDash = onGround && (
            (direction > 0 && eyeDirection.dot(new Vector(0, 1, 0)) < 0) ||
                (direction <= 0 && eyeDirection.dot(new Vector(0, 1, 0)) > 0));

        AtomicReference<Vector> dir = new AtomicReference<>(flatDash ? executor.getFlatDir() : executor.dir());

        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> iteration[0]++,
            () -> {
                new ParticleWrapper(Particle.CLOUD, particles + 1 - iteration[0], 0.3, 0.3, 0.3, 0)
                    .display(executor.getLocation());
                new ParticleWrapper(Particle.SMOKE, 3 * (particles + 1 - iteration[0]), 0.3, 0.3, 0.3, 0)
                    .display(executor.getLocation());
            },
            Config.Movement.DASH_VELOCITY_TASK_DELAY, Config.Movement.DASH_VELOCITY_TASK_PERIOD,
            MovementAction.class, "dashStraight",
            new PredicateRunnablePair(
                () -> {
                    if (iteration[0] == 0)
                        ex.setVelocity(dir.get().multiply(s).add(up));
                    else if (iteration[0] == 1) {
                        ex.setVelocity(dir.get().multiply(s));
                    }
                    else {
                        return true;
                    }
                    return false;
                },
                null
            )
        );
        if (!onGround) executor.increaseAirDashesPerformed();
    }

    @SuppressWarnings("all")
    private static boolean dashToItem(Combatant executor, ItemDisplay itemDisplay, int direction) {
        LivingEntity ex = executor.self();
        RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
            ex.getLocation().add(new Vector(0, Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y, 0)),
            itemDisplay.getLocation().subtract(ex.getLocation()).toVector().normalize(),
            MAX_STRAIGHT_DASH_DISTANCE.get() / 2, FluidCollisionMode.NEVER,
            true,
            block -> !block.isCollidable());

        int[] iteration = {0};
        int particles = Config.Movement.DASH_PARTICLE_TIMER_THRESHOLD;

        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                new ParticleWrapper(Particle.CLOUD, particles + 1 - iteration[0], 0.3, 0.3, 0.3, 0)
                    .display(executor.getLocation());
                new ParticleWrapper(Particle.SMOKE, 3 * (particles + 1 - iteration[0]), 0.3, 0.3, 0.3, 0)
                    .display(executor.getLocation());
                iteration[0] += Config.Movement.DASH_PARTICLE_TIMER_INCREMENT;
            },
            null,
            0, Config.Movement.DASH_PARTICLE_TASK_PERIOD,
            MovementAction.class, "dashToItem",
            new PredicateRunnablePair(
                () -> iteration[0] > Config.Movement.DASH_PARTICLE_TIMER_THRESHOLD, null
            )
        );

        if (impedanceCheck != null && impedanceCheck.getHitBlock() != null && impedanceCheck.getHitBlock().isCollidable())
            return false;


        double length = itemDisplay.getLocation().subtract(ex.getEyeLocation()).length();

        executor.setVelocity(ex.getEyeLocation().getDirection().multiply(Math.log(length)));

        Vector u = executor.getFlatDir().multiply(direction * Config.Movement.DASH_FORWARD_MULTIPLIER)
            .add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));

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
        }, Config.Movement.DASH_GRAB_CHECK_DELAY, TimeUnit.MILLISECONDS);

        return true;
    }


    private static void strafe(Combatant executor, boolean onGround, int direction) {
        if (!onGround) return; // handle this later

        Basis basis = VectorUtil.getBasisWithoutPitch(executor.getLocation());

        new ParticleWrapper(Particle.CLOUD, 3, 0.3, 0.3, 0.3, 0)
            .display(executor.getLocation());
        new ParticleWrapper(Particle.SMOKE, 9, 0.3, 0.3, 0.3, 0)
            .display(executor.getLocation());

        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Config.Movement.SPEED_DURATION-1, Config.Movement.SPEED_AMPLIFIER-1);
        executor.self().addPotionEffect(speed);

        executor.setVelocity(basis.forward().multiply(0.25).add(basis.right().multiply(1.5 * direction)));
    }


    /**
     * Tosses the specified {@link SwordEntity} away from the executor.
     * <p>
     * Applies velocity to the target in the executor's facing direction, creates
     * particle effects along the trajectory, performs collision checks with blocks
     * and nearby entities, and triggers a small explosion on impact.
     *
     * @param executor The combatant performing the toss.
     * @param target   The sword entity to toss.
     */
    public static void toss(Combatant executor, SwordEntity target) {
        Vector throwDirection = executor.dir();

        LivingEntity targetEntity = target.self();

        double baseForce = Config.Movement.TOSS_BASE_FORCE;
        double force = executor.calcValueAdditive(AspectType.MIGHT, Config.Movement.TOSS_MIGHT_MULTIPLIER_BASE, baseForce, Config.Movement.TOSS_MIGHT_MULTIPLIER_INCREMENT);

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> target.setVelocity(new Vector(0, Config.Movement.TOSS_UPWARD_VELOCITY_Y, 0)),
            0, 50,
            Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS,
            MovementAction.class, "toss"
        );

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> targetEntity.setVelocity(executor.dir().multiply(force)),
            SwordTimeUnit.ticksToMillis(Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS), 50,
            Config.Movement.TOSS_FORWARD_PHASE_ITERATIONS,
            MovementAction.class, "toss"
        );

        boolean[] check = {true};

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> {
                DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_WHITE), target.getChestLocation(), throwDirection, target.getAverageSize()*2, 0.25);
                Prefab.Particles.THROW_TRAIl.display(target.getChestLocation());

                World world = targetEntity.getWorld();
                Location currentLocation = target.getChestLocation();
                Location newLocation = currentLocation.add(throwDirection.clone().multiply(target.getAverageSize()));

                if (!newLocation.isFinite()) return;

                RayTraceResult blockResult = world.rayTraceBlocks(target.getChestLocation(), throwDirection,
                    target.getAverageSize()*2, FluidCollisionMode.NEVER,
                    true,
                    block -> block.getType().isCollidable());

                Debug.debug(MovementAction.class, 285, "blockResult :: " + blockResult);

                double entityRadius = Config.Movement.TOSS_ENTITY_DETECTION_RADIUS;
                Collection<LivingEntity> entities = world.getNearbyLivingEntities(
                    newLocation, entityRadius, entityRadius, entityRadius,
                    entity ->
                        !entity.getUniqueId().equals(targetEntity.getUniqueId()) &&
                            !entity.getUniqueId().equals(executor.getUniqueId()));

                if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
                    if (!entities.isEmpty()) {
                        Vector knockbackDir = currentLocation.toVector().subtract(((LivingEntity) Arrays.stream(entities.toArray()).toList().getFirst()).getLocation().toVector());
                        targetEntity.setVelocity(knockbackDir.normalize().multiply(Config.Movement.TOSS_KNOCKBACK_MULTIPLIER * force));
                    }
                    world.createExplosion(newLocation, Config.Movement.TOSS_EXPLOSION_POWER, false, false);
                    target.hit(executor, 5,
                        Config.Movement.TOSS_HIT_INVULNERABILITY_TICKS,
                        Config.Movement.TOSS_HIT_SHARD_DAMAGE,
                        Config.Movement.TOSS_HIT_TOUGHNESS_DAMAGE,
                        Config.Movement.TOSS_HIT_SOULFIRE_REDUCTION,
                        new Vector());
                    check[0] = false;
                }
            },
            SwordTimeUnit.ticksToMillis(Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS), 50,
            Config.Movement.TOSS_ANIMATION_ITERATIONS,
            MovementAction.class, "toss",
            new PredicateRunnablePair(
                () -> !check[0], null
            )
        );
    }
}
