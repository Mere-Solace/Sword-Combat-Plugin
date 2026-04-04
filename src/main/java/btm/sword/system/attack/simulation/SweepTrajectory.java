package btm.sword.system.attack.simulation;

import java.util.List;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

import btm.sword.utility.math.BezierUtil;

/**
 * A {@link VolumeTrajectory} that sweeps a capsule along a Catmull-Rom spline defined
 * by a {@link SweepCurve}.
 *
 * <p>At each sample, the curve is evaluated at {@code t} and {@code t + dt} to produce
 * the capsule's local-space start and end points. The radius is interpolated from the
 * curve's radius profile at {@code t}. Both endpoints are then transformed to world space
 * via the supplied {@code worldTransform}.</p>
 *
 * <p>Use with a {@link CapsuleVolume} buffer passed to {@link ActiveAttack}.</p>
 */
public final class SweepTrajectory implements VolumeTrajectory {

    /** Step size used to derive the capsule's second endpoint from the curve. */
    private static final float DT = 0.01f;

    private final SweepCurve curve;

    /**
     * Creates a sweep trajectory from the given curve.
     *
     * @param curve the Catmull-Rom path and radius profile for this attack
     */
    public SweepTrajectory(SweepCurve curve) {
        this.curve = curve;
    }

    /**
     * Returns the sweep curve this trajectory evaluates.
     *
     * @return the sweep curve
     */
    public SweepCurve getCurve() {
        return curve;
    }

    /**
     * {@inheritDoc}
     *
     * @param out must be a {@link CapsuleVolume}
     */
    @Override
    public void sample(float t, Matrix4fc worldTransform, Volume out) {
        CapsuleVolume capOut = (CapsuleVolume) out;

        Vector3f localStart = evaluateCurve(t);
        Vector3f localEnd = evaluateCurve(Math.min(1f, t + DT));
        capOut.radius = curve.radius(t);

        worldTransform.transformPosition(localStart, capOut.start);
        worldTransform.transformPosition(localEnd, capOut.end);

        capOut.aabbMin.set(
            Math.min(capOut.start.x, capOut.end.x) - capOut.radius,
            Math.min(capOut.start.y, capOut.end.y) - capOut.radius,
            Math.min(capOut.start.z, capOut.end.z) - capOut.radius
        );
        capOut.aabbMax.set(
            Math.max(capOut.start.x, capOut.end.x) + capOut.radius,
            Math.max(capOut.start.y, capOut.end.y) + capOut.radius,
            Math.max(capOut.start.z, capOut.end.z) + capOut.radius
        );
    }

    private Vector3f evaluateCurve(float t) {
        List<Vector3f> pts = curve.controlPoints();
        int n = pts.size();

        float scaled = t * (n - 1);
        int i = Math.max(0, Math.min(n - 2, (int) Math.floor(scaled)));
        float localT = scaled - i;

        Vector3f p0 = pts.get(Math.max(0, i - 1));
        Vector3f p1 = pts.get(i);
        Vector3f p2 = pts.get(Math.min(n - 1, i + 1));
        Vector3f p3 = pts.get(Math.min(n - 1, i + 2));

        return BezierUtil.catmullRom(p0, p1, p2, p3, localT);
    }
}
