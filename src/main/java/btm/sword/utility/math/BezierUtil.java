package btm.sword.utility.math;

import java.util.function.Function;

import org.bukkit.util.Vector;
import org.joml.Vector3f;

/**
 * Utility class for working with Bézier curves and control vectors in 3D space.
 * Methods are designed for generating and manipulating {@link Vector} points that
 * follow cubic Bézier curves or adjusted bases for spatial computation or animation.
 * <p>
 * Used to interpolate 3D paths, transitions, or geometric forms within the Sword plugin.
 * </p>
 */
public final class BezierUtil {

    private BezierUtil() {}

    /**
     * Constructs a cubic Bézier curve function in 3D space.
     * Returns a function mapping parameter {@code t} (from 0 to 1) to a point on the curve
     * using the given start/end points and two control points.
     *
     * @param control the control vector points that determine the shape of the curve
     * @return a function from {@code t} to a {@link Vector} point on the curve
     */
    /**
     * Evaluates a Catmull-Rom spline segment at parameter {@code t}.
     * {@code t} in [0, 1] maps to the segment between {@code p1} and {@code p2};
     * {@code p0} and {@code p3} are the surrounding control points that shape the curve.
     *
     * @param p0 point before the segment start (influences tangent at p1)
     * @param p1 segment start
     * @param p2 segment end
     * @param p3 point after the segment end (influences tangent at p2)
     * @param t  normalized parameter in [0, 1]
     * @return the interpolated point on the segment
     */
    public static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vector3f(
            0.5f * ((2 * p1.x) + (-p0.x + p2.x) * t
                + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
            0.5f * ((2 * p1.y) + (-p0.y + p2.y) * t
                + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
            0.5f * ((2 * p1.z) + (-p0.z + p2.z) * t
                + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
                + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }

    /**
     * Constructs a cubic Bézier curve function in 3D space.
     * Returns a function mapping parameter {@code t} (from 0 to 1) to a point on the curve
     * using the given start/end points and two control points.
     *
     * @param control the control vector points that determine the shape of the curve
     * @return a function from {@code t} to a {@link Vector} point on the curve
     */
    public static Function<Double, Vector> cubicBezier3D(ControlVectors control) {
        return t -> {
            double t2 = t * t;
            double t3 = t * t2;
            double mt = 1 - t;
            double mt2 = mt * mt;
            double mt3 = mt * mt2;

            Vector p0 = control.start().multiply(mt3);
            Vector p1 = control.c1().multiply(3 * mt2 * t);
            Vector p2 = control.c2().multiply(3 * mt * t2);
            Vector p3 = control.end().multiply(t3);

            return p0.add(p1).add(p2).add(p3);
        };
    }
}
