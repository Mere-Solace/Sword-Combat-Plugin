package btm.sword.system.attack.simulation;

import java.util.List;

import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import btm.sword.utility.math.BezierUtil;

/**
 * A {@link VolumeTrajectory} that interpolates an OBB between a small set of
 * {@link ControlPoint}s at runtime, storing only the defining curve points rather
 * than baked keyframe data.
 *
 * <p>Two interpolation modes are supported, selected by {@link ControlMode}:</p>
 * <ul>
 *   <li>{@link ControlMode#LINEAR} (2 points) — linearly interpolates both position and
 *       half-extents between the two endpoints.</li>
 *   <li>{@link ControlMode#BEZIER} (4 points) — cubic Bézier position via
 *       {@link BezierUtil#cubicBezier}; half-extents linearly lerped from the first to
 *       the last control point.</li>
 * </ul>
 *
 * <p>Control points have no rotation field; the output OBB inherits the attacker's world
 * yaw rotation only.</p>
 *
 * <p>Use with an {@link ObbVolume} buffer passed to
 * {@link btm.sword.system.attack.simulation.ActiveAttack}.</p>
 */
public final class ControlPointTrajectory implements VolumeTrajectory {

    private final List<ControlPoint> points;
    private final ControlMode mode;

    /**
     * Creates a control-point trajectory.
     *
     * @param points ordered list of control points; must be 2 for {@link ControlMode#LINEAR}
     *               or 4 for {@link ControlMode#BEZIER}
     * @param mode   interpolation mode
     * @throws IllegalArgumentException if the point count does not match the mode requirement
     */
    public ControlPointTrajectory(List<ControlPoint> points, ControlMode mode) {
        int required = mode == ControlMode.LINEAR ? 2 : 4;
        if (points.size() != required) {
            throw new IllegalArgumentException(
                "ControlPointTrajectory " + mode + " requires " + required + " points, got " + points.size());
        }
        this.points = List.copyOf(points);
        this.mode = mode;
    }

    /**
     * Returns the immutable control point list.
     *
     * @return control points in order
     */
    public List<ControlPoint> getPoints() {
        return points;
    }

    /**
     * Returns the interpolation mode.
     *
     * @return the control mode
     */
    public ControlMode getMode() {
        return mode;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Output rotation is the attacker's world yaw rotation (no per-point local rotation).
     * {@link ObbVolume#isSphere} is always {@code false} for this trajectory type.</p>
     *
     * @param out must be an {@link ObbVolume}
     */
    @Override
    public void sample(float t, Matrix4fc worldTransform, Volume out) {
        ObbVolume obbOut = (ObbVolume) out;
        obbOut.isSphere = false;

        ControlPoint first = points.getFirst();
        ControlPoint last = points.getLast();

        Vector3f localPos;
        if (mode == ControlMode.LINEAR) {
            localPos = new Vector3f(first.position()).lerp(last.position(), t);
        } else {
            localPos = BezierUtil.cubicBezier(
                points.get(0).position(),
                points.get(1).position(),
                points.get(2).position(),
                points.get(3).position(),
                t
            );
        }

        // Lerp half-extents from first to last control point
        Vector3f he = new Vector3f(first.halfExtents()).lerp(last.halfExtents(), t);

        // Transform position to world space; inherit world yaw as the OBB rotation
        worldTransform.transformPosition(localPos, obbOut.center);
        worldTransform.getNormalizedRotation(obbOut.rotation);
        obbOut.halfExtents.set(he);

        // Derive world AABB from OBB
        Matrix3f rot = new Matrix3f().rotation(obbOut.rotation);
        float ex = Math.abs(rot.m00()) * he.x + Math.abs(rot.m10()) * he.y + Math.abs(rot.m20()) * he.z;
        float ey = Math.abs(rot.m01()) * he.x + Math.abs(rot.m11()) * he.y + Math.abs(rot.m21()) * he.z;
        float ez = Math.abs(rot.m02()) * he.x + Math.abs(rot.m12()) * he.y + Math.abs(rot.m22()) * he.z;
        obbOut.aabbMin.set(obbOut.center.x - ex, obbOut.center.y - ey, obbOut.center.z - ez);
        obbOut.aabbMax.set(obbOut.center.x + ex, obbOut.center.y + ey, obbOut.center.z + ez);
    }
}
