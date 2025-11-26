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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
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
     * The dash moves the executor c2 or backward, handles velocity adjustments,
     * particle effects, ground checks, and can target {@link ItemDisplay} entities
     * if within range. Airborne dashes increment the executor's air dash count.
     *
     * @param executor The combatant performing the dash.
     * @param forward  True for c2 dash, false for backward dash.
     */ // TODO: #125 - This method is illegible, needs refactoring for readability
    public static void dash(Combatant executor, boolean forward) {
        double maxDistance = Config.Movement.DASH_MAX_DISTANCE;
        cast (executor, Config.Movement.DASH_CAST_DURATION, new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity ex = executor.self();
                final Location dashStartLocation = ex.getLocation().add(new Vector(0, Config.Movement.DASH_INITIAL_OFFSET_Y, 0));
                boolean onGround = executor.isGrounded();
                Location o = ex.getEyeLocation();

                // for use in dash attack method.
                executor.setDashDirection(ex.getEyeLocation().getDirection().multiply(forward ? 1 : -1));

                PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, Config.Movement.SPEED_DURATION, Config.Movement.SPEED_AMPLIFIER);
                ex.addPotionEffect(speed);

                // check for an item that may be the target of the dash
                Entity targetedItem = HitboxUtil.ray(o, o.getDirection(), maxDistance, Config.Movement.DASH_RAY_HITBOX_RADIUS,
                        entity -> (entity.getType() == EntityType.ITEM_DISPLAY &&
                                !entity.isDead() &&
                                entity instanceof ItemDisplay id &&
                                InteractiveItemArbiter.checkIfInteractive(id)) &&
                                !InteractiveItemArbiter.isImpaling(SwordEntityArbiter.get(ex.getUniqueId()), id));

                if (targetedItem instanceof ItemDisplay id &&
                        !id.isDead() &&
                        !id.getItemStack().isEmpty()) {
                    RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
                            ex.getLocation().add(new Vector(0, Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y, 0)),
                            targetedItem.getLocation().subtract(ex.getLocation()).toVector().normalize(),
                            maxDistance/2, FluidCollisionMode.NEVER,
                            true,
                            block -> !block.isCollidable());

                    new BukkitRunnable() {
                        int t = 0;
                        @Override
                        public void run() {
                            DrawUtil.secant(List.of(Prefab.Particles.TEST_SWORD_BLUE), dashStartLocation, ex.getLocation(), Config.Movement.DASH_SECANT_RADIUS);
                            t += Config.Movement.DASH_PARTICLE_TIMER_INCREMENT;
                            if (t > Config.Movement.DASH_PARTICLE_TIMER_THRESHOLD) cancel();
                        }
                    }.runTaskTimer(Sword.getInstance(), Config.Movement.DASH_PARTICLE_TASK_DELAY, Config.Movement.DASH_PARTICLE_TASK_PERIOD);

                    if (impedanceCheck == null || impedanceCheck.getHitBlock() == null) {
                        double length = id.getLocation().subtract(ex.getEyeLocation()).length();

                        executor.setVelocity(ex.getEyeLocation().getDirection().multiply(Math.log(length)));

                        Vector u = executor.getFlatDir().multiply(forward ? Config.Movement.DASH_FORWARD_MULTIPLIER : -Config.Movement.DASH_FORWARD_MULTIPLIER)
                                .add(Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_MULTIPLIER));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (id.getLocation().subtract(ex.getEyeLocation()).lengthSquared() < Config.Movement.DASH_GRAB_DISTANCE_SQUARED) {
                                    BlockData blockData = ex.getLocation().add(new Vector(0, Config.Movement.DASH_BLOCK_CHECK_OFFSET_Y, 0)).getBlock().getBlockData();
                                    new ParticleWrapper(Particle.BLOCK,
                                            Config.Movement.DASH_PARTICLE_COUNT,
                                            Config.Movement.DASH_PARTICLE_SPREAD_X,
                                            Config.Movement.DASH_PARTICLE_SPREAD_Y,
                                            Config.Movement.DASH_PARTICLE_SPREAD_Z,
                                            blockData).display(ex.getLocation());
                                    SoundUtil.playSound(ex, SoundType.ENTITY_ENDER_DRAGON_FLAP, Config.Movement.DASH_FLAP_SOUND_VOLUME, Config.Movement.DASH_FLAP_SOUND_PITCH);
                                    SoundUtil.playSound(ex, SoundType.ENTITY_PLAYER_ATTACK_SWEEP, Config.Movement.DASH_SWEEP_SOUND_VOLUME, Config.Movement.DASH_SWEEP_SOUND_PITCH);
                                    executor.setVelocity(u);

                                    InteractiveItemArbiter.onGrab(id, executor); // here is where the display is taken care of
                                }
                                else {
                                    Vector v = ex.getVelocity();
                                    double damping = Config.Movement.DASH_VELOCITY_DAMPING;
                                    ex.setVelocity(new Vector(v.getX() * damping, v.getY() * damping, v.getZ() * damping));
                                    executor.message("Didn't get there");
                                }
                            }
                        }.runTaskLater(Sword.getInstance(), Config.Movement.DASH_GRAB_CHECK_DELAY);
                        return;
                    }
                    else {
                        executor.message("You can't dash to that item...");
                    }
                }

                double dashPower = Config.Movement.DASH_BASE_POWER;
                double s = forward ? dashPower : -dashPower;
                Vector up = Config.Direction.UP().multiply(Config.Movement.DASH_UPWARD_BOOST);
                new BukkitRunnable() {
                    int i = 0;
                    @Override
                    public void run() {
                        Vector dir = ex.getEyeLocation().getDirection();
                        if (onGround && (
                                (forward && dir.dot(new Vector(0, 1, 0)) < 0)
                                        ||
                                        (!forward && dir.dot(new Vector(0, 1, 0)) > 0))) {
                            dir = executor.getFlatDir();
                        }
                        if (i == 0)
                            ex.setVelocity(dir.multiply(s).add(up));
                        else if (i == 1) {
                            ex.setVelocity(dir.multiply(s));
                        }
                        else {
                            cancel();
                        }
                        i++;
                    }
                }.runTaskTimer(Sword.getInstance(), Config.Movement.DASH_VELOCITY_TASK_DELAY, Config.Movement.DASH_VELOCITY_TASK_PERIOD);
                if (!onGround)
                    executor.increaseAirDashesPerformed();
            }
        });
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
