package btm.sword.system.action.movement;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.SwordAction;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;


/**
 * Provides movement-based actions for {@link Combatant} entities.
 * <p>
 * Includes dashing, directional movement, and throwing/manipulating
 * {@link SwordEntity} targets.
 */
public class MovementAction extends SwordAction {
    /**
     * Performs a dash action for the executor.
     * <p>
     * The dash moves the executor forward or backward, handles velocity adjustments,
     * particle effects, ground checks, and can target {@link ItemDisplay} entities
     * if within range. Airborne dashes increment the executor's air dash count.
     *
     * @param executor The combatant performing the dash.
     */
    public static void dash(Combatant executor, DashDirection direction) {
        boolean onGround = executor.isGrounded();
        Runnable dashConsumer;
        switch (direction) {
            case FORWARD -> dashConsumer = () -> new Dash(executor, 1).execute();
            case BACKWARD -> dashConsumer = () -> new Dash(executor, -1).execute();
            default -> {
                return;
            }
        }
        // execution occurs immediately; input-driven casting is handled by the InputAction's castDuration
        dashConsumer.run();
    }

    private static void strafe(Combatant executor, boolean onGround, int direction) {
        if (!onGround) return; // handle this later

        Basis basis = VectorUtil.getBasisWithoutPitch(executor.getLocation());

        Dash.scheduleParticleDisplay(executor);

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

                Debug.movement("blockResult=" + blockResult);

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
