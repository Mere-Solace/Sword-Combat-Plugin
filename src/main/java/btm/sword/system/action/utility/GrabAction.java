package btm.sword.system.action.utility;

import java.util.HashSet;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
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
import btm.sword.system.action.SwordAction;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.Prefab;
import btm.sword.util.entity.HitboxUtil;

/**
 * Utility class for performing grab actions.
 * <p>
 * Provides methods to grab either interactive items or living entities within a certain range and
 * maintain the grab for a duration with continuous velocity updates.
 */
public class GrabAction extends SwordAction {
    /**
     * Attempts to grab an interactive item or a living entity in front of the executor.
     * <p>
     * First checks for an interactive item display within range. If found, triggers the grab on the item.
     * Otherwise, checks for a living entity and applies pulling forces each tick until the duration ends,
     * the executor releases the grab, or the target dies.
     *
     * @param executor The {@link Combatant} performing the grab.
     */
    public static void grab(Combatant executor) {
        cast(executor, 12,
        new BukkitRunnable() {
            @Override
            public void run() {
                int baseDuration = 60;
                double baseGrabRange = 3;
                double baseGrabThickness = 0.6;

                long duration = (long) executor.calcValueAdditive(AspectType.MIGHT, 100L, baseDuration, 0.2);
                double range = executor.calcValueAdditive(AspectType.WILLPOWER, 4.5, baseGrabRange, 0.1);
                double grabThickness = executor.calcValueAdditive(AspectType.WILLPOWER, 0.75, baseGrabThickness, 0.1);

                LivingEntity ex = executor.entity();
                Location o = ex.getEyeLocation();

                Entity grabbedItem = HitboxUtil.ray(o, o.getDirection(), range, grabThickness,
                        entity -> entity.getType() == EntityType.ITEM_DISPLAY &&
                                !entity.isDead() &&
                                entity instanceof ItemDisplay id &&
                                InteractiveItemArbiter.checkIfInteractive(id));

                if (grabbedItem instanceof ItemDisplay id &&
                        !id.isDead() &&
                        !id.getItemStack().isEmpty()) {
                    InteractiveItemArbiter.onGrab(id, executor);

                    Prefab.Particles.GRAB_ATTEMPT.display(id.getLocation());
                    return;
                }

                HashSet<LivingEntity> hit = HitboxUtil.line(ex, o, o.getDirection(), range, grabThickness);
                if (hit.isEmpty()) {
                    Prefab.Particles.GRAB_ATTEMPT.display(ex.getEyeLocation().add(ex.getEyeLocation().getDirection().multiply(range)));
                    return;
                }

                LivingEntity target = hit.stream().toList().getFirst();

                if (target == null) {
                    Prefab.Particles.GRAB_ATTEMPT.display(ex.getEyeLocation().add(ex.getEyeLocation().getDirection().multiply(range)));
                    return;
                }

                Prefab.Particles.GRAB_ATTEMPT.display(target.getLocation());

                RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
                        ex.getLocation().add(new Vector(0,0.5,0)),
                        target.getLocation().subtract(ex.getLocation()).toVector().normalize(),
                        Math.sqrt(target.getLocation().subtract(ex.getLocation()).toVector().lengthSquared()), FluidCollisionMode.NEVER,
                        true,
                        block -> !block.isCollidable());

                if (impedanceCheck != null &&
                        impedanceCheck.getHitBlock() != null &&
                        !impedanceCheck.getHitBlock().getType().isAir()) {
                    return;
                }

                SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
                if (swordTarget.isHit()) return;

                if (swordTarget instanceof Combatant c && c.isAttemptingThrow()) c.setThrowCancelled(true);

                executor.onGrab(swordTarget);

                final int[] ticks = {0};
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (ticks[0] >= duration - 1 || target.isDead()) {
                            executor.onGrabLetGo();
                            cancel();
                            return;
                        }
                        if (!executor.isGrabbing()) {
                            executor.onGrabThrow();
                            cancel();
                            return;
                        }

                        Vector v = ex.getVelocity();
                        ex.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 2, 1));
                        ex.setVelocity(new Vector(v.getX() * 0.2, v.getY(),v.getZ() * 0.2));

                        double holdDist = 2;
                        Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(holdDist)).subtract(target.getLocation().toVector());
                        double distanceSquared = direction.lengthSquared();
                        double bufferDistance = 0.4;
                        double pullSpeed = 0.6;

                        if (distanceSquared < bufferDistance*bufferDistance) {
                            target.setVelocity(new Vector(0,target.getVelocity().getY()*0.25,0));
                        }
                        else {
                            double force = pullSpeed;
                            if (Math.abs(target.getEyeLocation().getY() - ex.getEyeLocation().getY()) > 1.2) {
                                force *= 2;
                            }
                            Vector velocity = direction.normalize().multiply(force);
                            if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
                                target.setVelocity(velocity);
                            }
                        }
                        ticks[0]++;
                    }
                }.runTaskTimer(Sword.getInstance(), 0, 1);
            }
        });
    }
}
