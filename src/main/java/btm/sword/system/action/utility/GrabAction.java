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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.control.PredicateRunnablePair;
import btm.sword.control.TimeArbiter;
import btm.sword.system.action.SwordAction;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.impl.ThrowPhase;
import btm.sword.utility.Prefab;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.entity.HitboxUtil;

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
    // TODO: #147 clean this nest up
    public static void grab(Combatant executor) {
        // execute immediately; casting/ability blocking is managed by the InputAction's castDuration
        int baseDuration = Config.Grab.BASE_DURATION;
        double baseGrabRange = Config.Grab.BASE_RANGE;
        double baseGrabThickness = Config.Grab.BASE_THICKNESS;

        if (executor instanceof SwordPlayer sp) {
            sp.resetTree(); // TODO: find out why this is necessary and why the tree doesn't reset on its own...
        }

        long duration = (long) executor.calcValueAdditive(AspectType.MIGHT, 100L, baseDuration,
            Config.Grab.DURATION_SCALING);
        double range = executor.calcValueAdditive(AspectType.WILLPOWER, 4.5, baseGrabRange,
            Config.Grab.RANGE_SCALING);
        double grabThickness = executor.calcValueAdditive(AspectType.WILLPOWER, 0.75, baseGrabThickness,
            Config.Grab.THICKNESS_SCALING);

        LivingEntity ex = executor.self();
        Location o = ex.getEyeLocation();

        boolean holdingAbility = executor instanceof SwordPlayer sp2 && !sp2.notHoldingAbilityItem();

        if (executor.getItemStackInHand(true).isEmpty() || executor.holdingSoulLink() || holdingAbility) {
            Entity grabbedItem = HitboxUtil.ray(o, o.getDirection(), range, grabThickness,
                entity -> entity.getType() == EntityType.ITEM_DISPLAY &&
                    !entity.isDead() &&
                    entity instanceof ItemDisplay id &&
                    InteractiveItemArbiter.checkIfInteractive(id) &&
                    // When holding an ability item, only target ability projectiles
                    (!holdingAbility || InteractiveItemArbiter.isAbilityProjectile(id)));

            if (executor.holdingSoulLink() &&
                grabbedItem instanceof ItemDisplay display &&
                InteractiveItemArbiter.isUmbralBlade(display)) {
                InteractiveItemArbiter.onGrab(display, executor);

                Prefab.Particles.GRAB_ATTEMPT.display(display.getLocation());
            } else if (!executor.holdingSoulLink() &&
                grabbedItem instanceof ItemDisplay display &&
                !display.isDead() &&
                !display.getItemStack().isEmpty()) {
                InteractiveItemArbiter.onGrab(display, executor);

                Prefab.Particles.GRAB_ATTEMPT.display(display.getLocation());
                return;
            }
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
            executor.getChestLocation(),
            target.getLocation().subtract(ex.getLocation()).toVector().normalize(),
            Math.sqrt(target.getLocation().subtract(ex.getLocation()).toVector().lengthSquared()), FluidCollisionMode.NEVER,
            true,
            block -> !block.isCollidable());

        if (impedanceCheck != null &&
            impedanceCheck.getHitBlock() != null &&
            !impedanceCheck.getHitBlock().getType().isAir()) {
            return;
        }

        SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target);
        if (swordTarget == null || swordTarget.isHit()) return;

        if (swordTarget instanceof Combatant c && c.getThrowPhase() == ThrowPhase.THROWING) {
            c.setThrowPhase(ThrowPhase.CANCELLED);
        }

        if (executor instanceof SwordPlayer swordPlayer) {
            swordPlayer.setTargetedEntity(swordTarget);
        }
        executor.onGrab(swordTarget);

        final int[] iterations = {0};
        TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                Vector v = ex.getVelocity();
                ex.addPotionEffect(new PotionEffect(
                    PotionEffectType.JUMP_BOOST,
                    Config.Grab.JUMP_BOOST_DURATION, Config.Grab.JUMP_BOOST_AMPLIFIER));
                ex.setVelocity(new Vector(
                    v.getX() * Config.Grab.EXECUTOR_HORIZONTAL_DAMPENING,
                    v.getY(),
                    v.getZ() * Config.Grab.EXECUTOR_HORIZONTAL_DAMPENING));

                double holdDist = Config.Grab.HOLD_DISTANCE;
                Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(holdDist)).subtract(target.getLocation().toVector());
                double distanceSquared = direction.lengthSquared();
                double bufferDistance = Config.Grab.HOLD_BUFFER;
                double pullSpeed = Config.Grab.PULL_SPEED;

                if (distanceSquared < bufferDistance * bufferDistance) {
                    target.setVelocity(new Vector(0, target.getVelocity().getY() * Config.Grab.CLOSE_Y_VELOCITY_SCALE, 0));
                } else {
                    double force = pullSpeed;
                    if (Math.abs(target.getEyeLocation().getY() - ex.getEyeLocation().getY()) > Config.Grab.VERTICAL_FORCE_THRESHOLD) {
                        force *= 2;
                    }
                    Vector velocity = direction.normalize().multiply(force);
                    if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
                        target.setVelocity(velocity);
                    }
                }
                iterations[0]++;
            },
            null,
            0, SwordTimeUnit.MILLISECONDS_PER_TICK,
            GrabAction.class, "grab",
            new PredicateRunnablePair(
                () -> iterations[0] >= duration - 1 || target.isDead(),
                executor::onGrabLetGo
            ),
            new PredicateRunnablePair(
                () -> !executor.isGrabbing(),
                executor::onGrabThrow
            )
        );
    }
}
