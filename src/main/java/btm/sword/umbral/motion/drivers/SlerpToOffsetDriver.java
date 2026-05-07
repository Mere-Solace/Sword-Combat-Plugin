package btm.sword.umbral.motion.drivers;

import java.util.Objects;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;

/**
 * Moves the blade smoothly toward a basis-rotated offset around an anchor entity, normalising
 * the per-tick velocity so the blade approaches the target at a fixed world-space speed.
 * <p>
 * Replaces {@code DisplayUtil.displaySlerpToOffset}. Used by {@code RecallingState} (anchor =
 * thrower, offset = chest vector) and {@code GrabImpaleState} (anchor = grabbed entity, offset
 * = lock-on point).
 * <p>
 * Each tick:
 * <ol>
 *   <li>Compute the world-space target as {@code anchor.location + (right*x + up*y + fwd*z)}.</li>
 *   <li>Compute the direction vector {@code target - currentBladeLocation}.</li>
 *   <li>If the distance is below {@code endDistance}, report {@link Status#DONE DONE}.</li>
 *   <li>Otherwise teleport the blade by {@code direction.normalized() * speed} along that vector.</li>
 * </ol>
 * <p>
 * Reports {@link Status#DONE DONE} on arrival, on timeout, when the anchor becomes invalid, or
 * when the externally-supplied {@code endCondition} returns {@code true}. On any DONE path, the
 * supplied {@code onArrive} callback (if non-null) is scheduled on the next tick via
 * {@link SwordScheduler#runBukkitTask(Runnable)}.
 */
public record SlerpToOffsetDriver(SwordEntity anchor, Vector localOffset, double speed, double endDistance,
                                  long timeoutTicks, Supplier<Boolean> endCondition, Runnable onArrive,
                                  int teleportDuration) implements BladeMotionDriver {

    /**
     * @param anchor           the entity the offset is computed relative to
     * @param localOffset      the anchor-local offset (right / up / forward components)
     * @param speed            per-tick world-space speed of approach
     * @param endDistance      arrival threshold in world units (squared internally for cheap compare)
     * @param timeoutTicks     maximum ticks before reporting DONE regardless of arrival
     * @param endCondition     external predicate; when it returns {@code true}, report DONE
     *                         (maybe {@code null} to disable)
     * @param onArrive         callback scheduled when the driver reports DONE for any reason
     *                         (maybe {@code null})
     * @param teleportDuration smoothing duration in ticks for the teleport interpolation
     */
    public SlerpToOffsetDriver(SwordEntity anchor, Vector localOffset,
                               double speed, double endDistance, long timeoutTicks,
                               Supplier<Boolean> endCondition, Runnable onArrive,
                               int teleportDuration) {
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.localOffset = localOffset.clone();
        this.speed = speed;
        this.endDistance = endDistance;
        this.timeoutTicks = timeoutTicks;
        this.endCondition = endCondition;
        this.onArrive = onArrive;
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {
    }

    @Override
    public Status tick(BladeMotionContext context) {
        if (anchor.isInvalid()) return finish();
        if (context.tickCount() > timeoutTicks) return finish();
        if (endCondition != null && Boolean.TRUE.equals(endCondition.get())) return finish();

        Location current = context.currentLocation();
        if (current == null) return finish();

        Location target = anchor.self().getLocation().add(
            anchor.rightBasisVector(false).multiply(localOffset.getX())
                .add(anchor.upBasisVector(false).multiply(localOffset.getY()))
                .add(anchor.forwardBasisVector(false).multiply(localOffset.getZ()))
        );
        Vector direction = target.toVector().subtract(current.toVector());
        if (direction.isZero() || direction.lengthSquared() < endDistance * endDistance) {
            return finish();
        }

        Vector step = direction.normalize().multiply(speed);
        Location updated = current.clone().add(step);
        // Lock the body yaw/pitch toward the target on the first tick only. Subsequent ticks
        // pass a null direction so Bukkit's smooth-teleport interpolation finishes the single
        // rotation onto the lock instead of being restarted every tick by a slightly different
        // step direction (which produced visible stutter on long recalls).
        Vector teleportDir = context.tickCount() == 1 ? step : null;
        context.teleportTo(updated, teleportDir, teleportDuration);
        return Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {
    }

    private Status finish() {
        if (onArrive != null) {
            SwordScheduler.runBukkitTask(onArrive);
        }
        return Status.DONE;
    }
}
