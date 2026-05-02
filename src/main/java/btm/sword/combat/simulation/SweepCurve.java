package btm.sword.combat.simulation;

import java.util.List;

import org.joml.Vector3f;

/**
 * A Catmull-Rom spline path used to define a capsule sweep attack.
 * <p>
 * Control points define the spine of the swept volume. The radius at any point along
 * the curve is linearly interpolated from the {@link RadiusPoint} profile. Used by
 * {@code SweepTrajectory} to evaluate capsule segment endpoints and radius at each
 * simulation tick.
 * </p>
 */
public final class SweepCurve {

    private final List<Vector3f> controlPoints;
    private final List<RadiusPoint> radiusProfile;

    /**
     * Constructs a sweep curve.
     *
     * @param controlPoints  Catmull-Rom spline control points; must contain at least 2
     * @param radiusProfile  per-t radius values for linear interpolation; must not be empty
     * @throws IllegalArgumentException if fewer than 2 control points are provided
     */
    public SweepCurve(List<Vector3f> controlPoints, List<RadiusPoint> radiusProfile) {
        if (controlPoints.size() < 2) {
            throw new IllegalArgumentException("SweepCurve requires at least 2 control points");
        }
        this.controlPoints = List.copyOf(controlPoints);
        this.radiusProfile = List.copyOf(radiusProfile);
    }

    /**
     * Returns the control points defining the Catmull-Rom spline.
     *
     * @return immutable list of control points
     */
    public List<Vector3f> controlPoints() {
        return controlPoints;
    }

    /**
     * Returns the radius profile used for interpolation.
     *
     * @return immutable list of radius points
     */
    public List<RadiusPoint> radiusProfile() {
        return radiusProfile;
    }

    /**
     * Linearly interpolates the capsule radius at normalized time {@code t}.
     * Clamps to the first or last radius value when {@code t} is outside the profile range.
     *
     * @param t normalized time, {@code 0.0} to {@code 1.0}
     * @return interpolated radius at {@code t}
     */
    public float radius(float t) {
        if (radiusProfile.isEmpty()) return 0f;
        if (radiusProfile.size() == 1) return radiusProfile.getFirst().radius();
        if (t <= radiusProfile.getFirst().t()) return radiusProfile.getFirst().radius();
        if (t >= radiusProfile.getLast().t()) return radiusProfile.getLast().radius();

        for (int i = 0; i < radiusProfile.size() - 1; i++) {
            RadiusPoint a = radiusProfile.get(i);
            RadiusPoint b = radiusProfile.get(i + 1);
            if (t >= a.t() && t <= b.t()) {
                float localT = (t - a.t()) / (b.t() - a.t());
                return a.radius() + localT * (b.radius() - a.radius());
            }
        }

        return radiusProfile.getLast().radius();
    }

    /**
     * A single entry in a radius profile, associating a normalized time with a radius value.
     *
     * @param t      normalized time, {@code 0.0} to {@code 1.0}
     * @param radius capsule radius at this time
     */
    public record RadiusPoint(float t, float radius) {}
}
