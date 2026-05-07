package btm.sword.umbral.motion.drivers;

import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;
import btm.sword.util.math.Basis;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;

/**
 * Drives the blade along a parametric cubic-Bézier path over a fixed number of ticks.
 * <p>
 * Replaces the inherited {@code generateFunctions}/{@code stepFlight} flow used by
 * {@code LungingState} and the existing {@code UmbralBlade.calcBezierTrajectory}. On install,
 * the supplied {@link ControlVectors} are baked into the supplied {@link Basis} via
 * {@link ControlVectors#adjustToBasis} and a position function is materialised via
 * {@link BezierUtil#cubicBezier3D}. Each tick samples {@code positionFunction(t)} with
 * {@code t = tickCount * timeStep}, teleports the blade to {@code origin + sample}, and
 * reports {@link Status#DONE DONE} once {@code tickCount} reaches {@code durationTicks}.
 * <p>
 * The path's origin is captured at install time from the context's current location — the
 * driver does not extrapolate motion if the display is moved externally during flight.
 */
public final class BezierDriver implements BladeMotionDriver {

    private final ControlVectors controlVectors;
    private final Basis basis;
    private final long durationTicks;
    private final double timeStep;
    private final int teleportDuration;
    private final double scale;

    private Location origin;
    private Location previousSample;
    private Function<Double, Vector> positionFunction;

    /**
     * @param controlVectors    the four cubic-Bézier control points
     * @param basis             the basis to project control vectors into world space
     * @param scale             scale factor passed to {@link ControlVectors#adjustToBasis}
     * @param durationTicks     total number of ticks before the driver reports DONE
     * @param timeStep          parametric step size per tick (typical value: {@code 1.0 / durationTicks})
     * @param teleportDuration  smoothing duration in ticks for the teleport interpolation
     */
    public BezierDriver(ControlVectors controlVectors, Basis basis, double scale,
                        long durationTicks, double timeStep, int teleportDuration) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        this.controlVectors = controlVectors;
        this.basis = basis;
        this.scale = scale;
        this.durationTicks = durationTicks;
        this.timeStep = timeStep;
        this.teleportDuration = teleportDuration;
    }

    @Override
    public void onInstall(BladeMotionContext context) {
        ControlVectors adjusted = controlVectors.adjustToBasis(basis, scale);
        positionFunction = BezierUtil.cubicBezier3D(adjusted);
        origin = context.currentLocation();
        previousSample = origin == null ? null : origin.clone();
    }

    @Override
    public Status tick(BladeMotionContext context) {
        if (origin == null || positionFunction == null) return Status.DONE;

        double t = context.tickCount() * timeStep;
        Vector sample = positionFunction.apply(t);
        Location target = origin.clone().add(sample);

        Vector direction;
        if (previousSample == null) {
            direction = basis.forward();
        } else {
            direction = target.toVector().subtract(previousSample.toVector());
            if (direction.lengthSquared() < 1e-9) direction = basis.forward();
        }
        context.teleportTo(target, direction, teleportDuration);
        previousSample = target;

        return context.tickCount() >= durationTicks ? Status.DONE : Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {
        origin = null;
        previousSample = null;
        positionFunction = null;
    }
}
