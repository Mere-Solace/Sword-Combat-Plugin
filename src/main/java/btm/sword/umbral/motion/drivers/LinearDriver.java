package btm.sword.umbral.motion.drivers;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;

/**
 * Moves the blade in a straight line at constant speed for a fixed number of ticks.
 * <p>
 * Used as a simple flight-path fallback when the more elaborate {@link BezierDriver} is not
 * appropriate. The direction vector is normalized at construction; the per-tick displacement is
 * {@code direction * speed}. The driver reports {@link Status#DONE DONE} once the configured
 * number of ticks has elapsed.
 */
public final class LinearDriver implements BladeMotionDriver {

    private final Vector unitDirection;
    private final double speed;
    private final long durationTicks;
    private final int teleportDuration;

    /**
     * @param direction        the direction of motion; must be non-zero (normalized internally)
     * @param speed            the per-tick world-space speed
     * @param durationTicks    the number of ticks to run before reporting DONE
     * @param teleportDuration smoothing duration in ticks for the teleport interpolation
     * @throws IllegalArgumentException if {@code direction} is the zero vector
     */
    public LinearDriver(Vector direction, double speed, long durationTicks, int teleportDuration) {
        if (direction.lengthSquared() < 1e-9) {
            throw new IllegalArgumentException("direction must be a non-zero vector");
        }
        this.unitDirection = direction.clone().normalize();
        this.speed = speed;
        this.durationTicks = durationTicks;
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {}

    @Override
    public Status tick(BladeMotionContext context) {
        Location current = context.currentLocation();
        if (current == null) return Status.DONE;

        Vector step = unitDirection.clone().multiply(speed);
        Location next = current.clone().add(step);
        context.teleportTo(next, unitDirection.clone(), teleportDuration);

        return context.tickCount() >= durationTicks ? Status.DONE : Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {}
}
