package btm.sword.system.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.Prefab;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.sound.SoundType;
import btm.sword.util.sound.SoundUtil;


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
     * @param forward  True for forward dash, false for backward dash.
     */ // TODO: #125 - This method is illegible, needs refactoring for readability
    public static void dash(Combatant executor, boolean forward) {
        double maxDistance = Config.Movement.DASH_MAX_DISTANCE;
        cast(executor, Config.Movement.DASH_CAST_DURATION, new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity ex = executor.self();

                // Starting point of dash effects
                final Location dashStartLocation = ex.getLocation().add(new Vector(0, Config.Movement.DASH_INITIAL_OFFSET_Y, 0));
                boolean onGround = executor.isGrounded();
                Location o = ex.getEyeLocation();

                // Store initial direction for follow-up dash attack logic
                Vector dashDir = o.getDirection().multiply(forward ? 1 : -1);
                executor.setDashDirection(dashDir);
                // Apply dash speed
                PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Config.Movement.SPEED_DURATION, Config.Movement.SPEED_AMPLIFIER);
                ex.addPotionEffect(speed);

                // Determine if the dash has a target item
                Entity targetItem = findTargetItem(executor, o, maxDistance);

                if (isValidDisplayItem(targetItem)) {

                    ItemDisplay display = (ItemDisplay) targetItem;

                    // Check if blocks obstruct the dash path
                    boolean blocked = hasObstruction(ex, display, maxDistance);

                    startDashParticles(dashStartLocation, ex);

                    if (!blocked) {
                        handleTargetedDash(executor, ex, display, forward);
                        return;
                    }

                    executor.message("You can't dash to that item...");
                }

                // Default untargeted dash
                performNormalDash(executor, ex, onGround, forward);
            }
        });
    }


private static Entity findTargetItem(Combatant executor, Location eye, double maxDistance) {

    if (!executor.getItemStackInHand(true).isEmpty()) {
        return null;
    }

    return HitboxUtil.ray(
            eye,
            eye.getDirection(),
            maxDistance,
            Config.Movement.DASH_RAY_HITBOX_RADIUS,
            entity -> entity instanceof ItemDisplay id &&
                      !entity.isDead() &&
                      !InteractiveItemArbiter.isImpaling(
                          SwordEntityArbiter.get(executor.self().getUniqueId()), id) &&
                      InteractiveItemArbiter.checkIfInteractive(id));
}

private static boolean isValidDisplayItem(Entity e) {
    return e instanceof ItemDisplay id &&
           !id.isDead() &&
           !id.getItemStack().isEmpty();
}

private static boolean hasObstruction(LivingEntity ex, ItemDisplay target, double maxDistance) {

    // Where to begin checking for a block that might interrupt the dash
    Location start = ex.getLocation().add(
            0,
            Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y,
            0
    );

    // Direction from player → target
    Vector dir = target.getLocation().subtract(ex.getLocation()).toVector().normalize();

    RayTraceResult result = ex.getWorld().rayTraceBlocks(
            start,
            dir,
            maxDistance / 2,
            FluidCollisionMode.NEVER,
            true,
            block -> !block.isCollidable()
    );

    return result != null && result.getHitBlock() != null;
}

private static void startDashParticles(Location dashStartLocation, LivingEntity ex) {

    new BukkitRunnable() {
        int t = 0;

        @Override
        public void run() {
            DrawUtil.secant(
                    List.of(Prefab.Particles.TEST_SWORD_BLUE),
                    dashStartLocation,
                    ex.getLocation(),
                    Config.Movement.DASH_SECANT_RADIUS
            );

            t += Config.Movement.DASH_PARTICLE_TIMER_INCREMENT;
            if (t > Config.Movement.DASH_PARTICLE_TIMER_THRESHOLD) {
                cancel();
            }
        }
    }.runTaskTimer(
            Sword.getInstance(),
            Config.Movement.DASH_PARTICLE_TASK_DELAY,
            Config.Movement.DASH_PARTICLE_TASK_PERIOD
    );
}

private static void handleTargetedDash(Combatant executor, LivingEntity ex, ItemDisplay target, boolean forward) {

    double distance = target.getLocation().subtract(ex.getEyeLocation()).length();

    // Initial velocity toward target based on distance
    executor.setVelocity(ex.getEyeLocation().getDirection().multiply(Math.log(distance)));

    // Up + forward velocity used upon successful grab
    Vector postGrabVelocity = executor.getFlatDir()
            .multiply(forward ? Config.Movement.DASH_FORWARD_MULTIPLIER : -Config.Movement.DASH_FORWARD_MULTIPLIER)
            .add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));

    new BukkitRunnable() {
        @Override
        public void run() {

            double distSq = target.getLocation().subtract(ex.getEyeLocation()).lengthSquared();

            if (distSq < Config.Movement.DASH_GRAB_DISTANCE_SQUARED) {
                handleSuccessfulGrab(executor, ex, target, postGrabVelocity);
            } else {
                handleFailedGrab(ex);
            }
        }
    }.runTaskLater(Sword.getInstance(), Config.Movement.DASH_GRAB_CHECK_DELAY);
}

private static void handleSuccessfulGrab(Combatant executor, LivingEntity ex, ItemDisplay target, Vector velocity) {

    BlockData blockData = ex.getLocation()
            .add(0, Config.Movement.DASH_BLOCK_CHECK_OFFSET_Y, 0)
            .getBlock()
            .getBlockData();

    new ParticleWrapper(
            Particle.BLOCK,
            Config.Movement.DASH_PARTICLE_COUNT,
            Config.Movement.DASH_PARTICLE_SPREAD_X,
            Config.Movement.DASH_PARTICLE_SPREAD_Y,
            Config.Movement.DASH_PARTICLE_SPREAD_Z,
            blockData
    ).display(ex.getLocation());

    SoundUtil.playSound(ex, SoundType.ENTITY_ENDER_DRAGON_FLAP,
            Config.Movement.DASH_FLAP_SOUND_VOLUME,
            Config.Movement.DASH_FLAP_SOUND_PITCH);

    SoundUtil.playSound(ex, SoundType.ENTITY_PLAYER_ATTACK_SWEEP,
            Config.Movement.DASH_SWEEP_SOUND_VOLUME,
            Config.Movement.DASH_SWEEP_SOUND_PITCH);

    executor.setVelocity(velocity);

    // Perform the actual “item pickup / grab”
    InteractiveItemArbiter.onGrab(target, executor);
}

private static void handleFailedGrab(LivingEntity ex) {

    Vector vel = ex.getVelocity();
    double damping = Config.Movement.DASH_VELOCITY_DAMPING;

    ex.setVelocity(new Vector(
            vel.getX() * damping,
            vel.getY() * damping,
            vel.getZ() * damping
    ));

    if (ex instanceof Player p) {
        p.sendMessage("Didn't get there");
    }
}

private static void performNormalDash(Combatant executor, LivingEntity ex, boolean grounded, boolean forward) {

    double base = Config.Movement.DASH_BASE_POWER;
    double power = forward ? base : -base;

    Vector upBoost = Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_BOOST);

    new BukkitRunnable() {
        int i = 0;

        @Override
        public void run() {

            Vector dir = ex.getEyeLocation().getDirection();

            if (grounded && (
                (forward && dir.dot(new Vector(0, 1, 0)) < 0) ||
                (!forward && dir.dot(new Vector(0, 1, 0)) > 0)
            )) {
                dir = executor.getFlatDir();
            }

            if (i == 0) {
                ex.setVelocity(dir.multiply(power).add(upBoost));
            } else if (i == 1) {
                ex.setVelocity(dir.multiply(power));
            } else {
                cancel();
            }

            i++;
        }
    }.runTaskTimer(
            Sword.getInstance(),
            Config.Movement.DASH_VELOCITY_TASK_DELAY,
            Config.Movement.DASH_VELOCITY_TASK_PERIOD
    );

    if (!grounded) {
        executor.increaseAirDashesPerformed();
    }
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
        LivingEntity ex = executor.self();
        LivingEntity t = target.self();

        double baseForce = Config.Movement.TOSS_BASE_FORCE;
        double force = executor.calcValueAdditive(AspectType.MIGHT, Config.Movement.TOSS_MIGHT_MULTIPLIER_BASE, baseForce, Config.Movement.TOSS_MIGHT_MULTIPLIER_INCREMENT);

        for (int i = 0; i < Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    t.setVelocity(new Vector(0, Config.Movement.TOSS_UPWARD_VELOCITY_Y, 0));
                }
            }.runTaskLater(Sword.getInstance(), i);
        }

        for (int i = 0; i < Config.Movement.TOSS_FORWARD_PHASE_ITERATIONS; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    t.setVelocity(ex.getEyeLocation().getDirection().multiply(force));
                }
            }.runTaskLater(Sword.getInstance(), i + Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS);
        }

        boolean[] check = {true};
        for (int i = 0; i < Config.Movement.TOSS_ANIMATION_ITERATIONS; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!check[0]) {
                        cancel();
                        return;
                    }
                    World world = t.getWorld();
                    Location base = t.getLocation();
                    double h = t.getEyeHeight();
                    Vector v = t.getVelocity().normalize();
                    Location l = base.add(new Vector(0, h * Config.Movement.TOSS_LOCATION_OFFSET_MULTIPLIER, 0).add(v));

                    Prefab.Particles.THROW_TRAIl.display(base.add(new Vector(0, h * Config.Movement.TOSS_PARTICLE_HEIGHT_MULTIPLIER, 0)));

                    if (l.isFinite()) {
                        RayTraceResult blockResult = world.rayTraceBlocks(l, v,
                                h * Config.Movement.TOSS_RAY_TRACE_DISTANCE_MULTIPLIER, FluidCollisionMode.NEVER,
                                true,
                                block -> !block.getType().isCollidable());

                        double entityRadius = Config.Movement.TOSS_ENTITY_DETECTION_RADIUS;
                        Collection<LivingEntity> entities = world.getNearbyLivingEntities(
                                l, entityRadius, entityRadius, entityRadius,
                                entity -> !entity.getUniqueId().equals(t.getUniqueId()) && !entity.getUniqueId().equals(ex.getUniqueId()));

                        if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
                            if (!entities.isEmpty()) {
                                Vector knockbackDir = base.toVector().subtract(((LivingEntity) Arrays.stream(entities.toArray()).toList().getFirst()).getLocation().toVector());
                                t.setVelocity(knockbackDir.normalize().multiply(Config.Movement.TOSS_KNOCKBACK_MULTIPLIER * force));
                            }
                            world.createExplosion(l, Config.Movement.TOSS_EXPLOSION_POWER, false, false);
                            target.hit(executor, 5,
                                    Config.Movement.TOSS_HIT_INVULNERABILITY_TICKS,
                                    Config.Movement.TOSS_HIT_SHARD_DAMAGE,
                                    Config.Movement.TOSS_HIT_TOUGHNESS_DAMAGE,
                                    Config.Movement.TOSS_HIT_SOULFIRE_REDUCTION,
                                    new Vector());
                            check[0] = false;
                        }
                    }
                }
            }.runTaskLater(Sword.getInstance(), i);
        }
    }
}
