package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.config.ConfigManager;
import btm.sword.config.section.MovementConfig;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.display.Prefab;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.sound.SoundType;
import btm.sword.util.sound.SoundUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


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
     */
    public static void dash(Combatant executor, boolean forward) {
        MovementConfig cfg = ConfigManager.getInstance().getMovement();
        double maxDistance = cfg.getDashMaxDistance();

        cast (executor, cfg.getDashCastDuration(), new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity ex = executor.entity();
                Location initial = ex.getLocation().add(new Vector(0, cfg.getDashInitialOffsetY(), 0));
                boolean onGround = executor.isGrounded();
                Location o = ex.getEyeLocation();

                // check for an item that may be the target of the dash
                Entity targetedItem = HitboxUtil.ray(o, o.getDirection(), maxDistance, cfg.getDashRayHitboxRadius(),
                        entity -> (entity.getType() == EntityType.ITEM_DISPLAY &&
                                !entity.isDead() &&
                                entity instanceof ItemDisplay id &&
                                InteractiveItemArbiter.checkIfInteractive(id)) &&
                                !InteractiveItemArbiter.isImaplingEntity(SwordEntityArbiter.get(ex.getUniqueId()), id));
//                executor.message("Targeted: " + targetedItem);

                if (targetedItem instanceof ItemDisplay id &&
                        !id.isDead() &&
                        !id.getItemStack().isEmpty()) {
                    RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
                            ex.getLocation().add(new Vector(0, cfg.getDashImpedanceCheckOffsetY(), 0)),
                            targetedItem.getLocation().subtract(ex.getLocation()).toVector().normalize(),
                            maxDistance/2, FluidCollisionMode.NEVER,
                            true,
                            block -> !block.isCollidable());

                    new BukkitRunnable() {
                        int t = 0;
                        @Override
                        public void run() {
                            DisplayUtil.secant(List.of(Prefab.Particles.TEST_SWORD_BLUE), initial, ex.getLocation(), cfg.getDashSecantRadius());
                            t += cfg.getDashParticleTimerIncrement();
                            if (t > cfg.getDashParticleTimerThreshold()) cancel();
                        }
                    }.runTaskTimer(Sword.getInstance(), cfg.getDashParticleTaskDelay(), cfg.getDashParticleTaskPeriod());


//					if (impedanceCheck != null)
//						executor.message("Hit block: " + impedanceCheck.getHitBlock());

                    if (impedanceCheck == null || impedanceCheck.getHitBlock() == null) {
                        double length = id.getLocation().subtract(ex.getEyeLocation()).length();

                        executor.setVelocity(ex.getEyeLocation().getDirection().multiply(Math.log(length)));

                        Vector u = executor.getFlatDir().multiply(forward ? cfg.getDashForwardMultiplier() : -cfg.getDashForwardMultiplier())
                                .add(Prefab.Direction.UP.clone().multiply(cfg.getDashUpwardMultiplier()));

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (id.getLocation().subtract(ex.getEyeLocation()).lengthSquared() < cfg.getDashGrabDistanceSquared()) {
                                    BlockData blockData = ex.getLocation().add(new Vector(0, cfg.getDashBlockCheckOffsetY(), 0)).getBlock().getBlockData();
                                    new ParticleWrapper(Particle.DUST_PILLAR,
                                            cfg.getDashParticleCount(),
                                            cfg.getDashParticleSpreadX(),
                                            cfg.getDashParticleSpreadY(),
                                            cfg.getDashParticleSpreadZ(),
                                            blockData).display(ex.getLocation());
                                    SoundUtil.playSound(ex, SoundType.ENTITY_ENDER_DRAGON_FLAP, cfg.getDashFlapSoundVolume(), cfg.getDashFlapSoundPitch());
                                    SoundUtil.playSound(ex, SoundType.ENTITY_PLAYER_ATTACK_SWEEP, cfg.getDashSweepSoundVolume(), cfg.getDashSweepSoundPitch());
                                    executor.setVelocity(u);
                                    InteractiveItemArbiter.onGrab(id, executor);
                                }
                                else {
                                    Vector v = ex.getVelocity();
                                    double damping = cfg.getDashVelocityDamping();
                                    ex.setVelocity(new Vector(v.getX() * damping, v.getY() * damping, v.getZ() * damping));
                                    executor.message("Didn't get there");
                                }
                            }
                        }.runTaskLater(Sword.getInstance(), cfg.getDashGrabCheckDelay());
                        return;
                    }
                    else {
                        executor.message("You can't dash to that item...");
                    }
                }

                double dashPower = cfg.getDashBasePower();
                double s = forward ? dashPower : -dashPower;
                Vector up = Prefab.Direction.UP.clone().multiply(cfg.getDashUpwardBoost());
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
                }.runTaskTimer(Sword.getInstance(), cfg.getDashVelocityTaskDelay(), cfg.getDashVelocityTaskPeriod());
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
        MovementConfig cfg = ConfigManager.getInstance().getMovement();
        LivingEntity ex = executor.entity();
        LivingEntity t = target.entity();

        double baseForce = cfg.getTossBaseForce();
        double force = executor.calcValueAdditive(AspectType.MIGHT, cfg.getTossMightMultiplierBase(), baseForce, cfg.getTossMightMultiplierIncrement());

        for (int i = 0; i < cfg.getTossUpwardPhaseIterations(); i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    t.setVelocity(new Vector(0, cfg.getTossUpwardVelocityY(), 0));
                }
            }.runTaskLater(Sword.getInstance(), i);
        }

        for (int i = 0; i < cfg.getTossForwardPhaseIterations(); i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    t.setVelocity(ex.getEyeLocation().getDirection().multiply(force));
                }
            }.runTaskLater(Sword.getInstance(), i + cfg.getTossUpwardPhaseIterations());
        }

        boolean[] check = {true};
        for (int i = 0; i < cfg.getTossAnimationIterations(); i++) {
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
                    Location l = base.add(new Vector(0, h * cfg.getTossLocationOffsetMultiplier(), 0).add(v));

                    Prefab.Particles.THROW_TRAIl.display(base.add(new Vector(0, h * cfg.getTossParticleHeightMultiplier(), 0)));

                    if (l.isFinite()) {
                        RayTraceResult blockResult = world.rayTraceBlocks(l, v,
                                h * cfg.getTossRayTraceDistanceMultiplier(), FluidCollisionMode.NEVER,
                                true,
                                block -> !block.getType().isCollidable());

                        double entityRadius = cfg.getTossEntityDetectionRadius();
                        Collection<LivingEntity> entities = world.getNearbyLivingEntities(
                                l, entityRadius, entityRadius, entityRadius,
                                entity -> !entity.getUniqueId().equals(t.getUniqueId()) && !entity.getUniqueId().equals(ex.getUniqueId()));

                        if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
                            if (!entities.isEmpty()) {
                                Vector knockbackDir = base.toVector().subtract(((LivingEntity) Arrays.stream(entities.toArray()).toList().getFirst()).getLocation().toVector());
                                t.setVelocity(knockbackDir.normalize().multiply(cfg.getTossKnockbackMultiplier() * force));
                            }
                            world.createExplosion(l, cfg.getTossExplosionPower(), false, false);
                            target.hit(executor,
                                    cfg.getTossHitInvulnerabilityTicks(),
                                    cfg.getTossHitShardDamage(),
                                    cfg.getTossHitToughnessDamage(),
                                    cfg.getTossHitSoulfireReduction(),
                                    new Vector());
                            check[0] = false;
                        }
                    }
                }
            }.runTaskLater(Sword.getInstance(), i);
        }
    }
}
