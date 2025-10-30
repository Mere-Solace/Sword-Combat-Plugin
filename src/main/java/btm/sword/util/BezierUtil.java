package btm.sword.util;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for working with Bézier curves and control vectors in 3D space.
 * Methods are designed for generating and manipulating {@link Vector} points that
 * follow cubic Bézier curves or adjusted bases for spatial computation or animation.
 * <p>
 * Used to interpolate 3D paths, transitions, or geometric forms within the Sword plugin.
 * </p>
 */
public class BezierUtil {
    /**
     * Constructs a cubic Bézier curve function in 3D space.
     * Returns a function mapping parameter {@code t} (from 0 to 1) to a point on the curve
     * using the given start/end points and two control points.
     *
     * @param start the starting {@link Vector} of the curve
     * @param end the ending {@link Vector} of the curve
     * @param c1 the first control {@link Vector}
     * @param c2 the second control {@link Vector}
     * @return a function from {@code t} to a {@link Vector} point on the curve
     */
    public static Function<Double, Vector> cubicBezier3d(Vector start, Vector end, Vector c1, Vector c2) {
        return t -> {
            double t2 = t*t;
            double t3 = t*t2;
            double mt = 1-t;
            double mt2 = mt*mt;
            double mt3 = mt*mt2;

            Vector p0 = start.clone().multiply(mt3);
            Vector p1 = c1.clone().multiply(3*mt2*t);
            Vector p2 = c2.clone().multiply(3*mt*t2);
            Vector p3 = end.clone().multiply(t3);

            return p0.add(p1).add(p2).add(p3);
        };
    }

    /**
     * Applies a basis transformation and scaling to each control vector.
     * Relies on {@link VectorUtil#transformWithNewBasis(List, Vector)} for basis transformation,
     * then scales the result.
     *
     * @param basis list of {@link Vector} specifying the new basis
     * @param controlVectors list of control {@link Vector}s to transform
     * @param multiplier scalar value to multiply each transformed vector by
     * @return list of transformed {@link Vector}s with basis and scaling applied
     */
    public static List<Vector> adjustCtrlToBasis(List<Vector> basis, List<Vector> controlVectors, double multiplier) {
        return controlVectors.stream().map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(multiplier)).toList();
    }

    /**
     * Computes discrete sample points along a cubic Bezier curve in 3D space.
     * Returns a list of {@link Vector}s representing the curve points between start and end,
     * using two control points and the given number of steps.
     *
     * @param start the starting {@link Vector}
     * @param end the ending {@link Vector}
     * @param c1 the first control {@link Vector}
     * @param c2 the second control {@link Vector}
     * @param steps the number of steps (points) to interpolate along the curve
     * @return list of {@link Vector} sample points along the curve
     */
	public static List<Vector> cubicBezier3D(Vector start, Vector end, Vector c1, Vector c2, int steps) {
		List<Vector> vectors = new ArrayList<>(steps);

		for (int i = 0; i < steps; i++) {
			double t = (double) i /steps;

			double t2 = t * t;
			double t3 = t2 * t;
			double mt = 1 - t;
			double mt2 = mt * mt;
			double mt3 = mt2 * mt;

			Vector p0 = start.clone().multiply(mt3);
			Vector p1 = c1.clone().multiply(3 * mt2 * t);
			Vector p2 = c2.clone().multiply(3 * mt * t2);
			Vector p3 = end.clone().multiply(t3);

			vectors.add(p0.add(p1).add(p2).add(p3));
		}

		return vectors;
	}

    /**
     * Computes sample points along a rational cubic Bezier curve in 3D space.
     * Each curve point is weighted by the provided rational weights for each control point.
     * Attempts to fill gaps between points by midpoint interpolation if angular difference is small.
     *
     * @param start the starting {@link Vector}
     * @param end the ending {@link Vector}
     * @param c1 the first control {@link Vector}
     * @param c2 the second control {@link Vector}
     * @param r0 weight for the start vector
     * @param r1 weight for the first control vector
     * @param r2 weight for the second control vector
     * @param r3 weight for the end vector
     * @param steps number of sample steps for curve interpolation
     * @return list of weighted sample {@link Vector}s along the curve, including midpoints for visual smoothing
     */
	public static List<Vector> cubicBezierRational3D(Vector start, Vector end, Vector c1, Vector c2, double r0, double r1, double r2, double r3, int steps) {
		List<Vector> vectors = new ArrayList<>(steps);

		for (int i = 0; i < steps; i++) {
			double t = (double) i /steps;
			double t2 = t * t;
			double t3 = t2 * t;
			double mt = 1 - t;
			double mt2 = mt * mt;
			double mt3 = mt2 * mt;

			double f0 = r0 * mt3;
			double f1 = r1 * mt2 * t;
			double f2 = r2 * mt * t2;
			double f3 = r3 * t3;

			double basis = f0 + f1 + f2 + f3;

			Vector weightedSum = start.clone().multiply(f0)
					.add(c1.clone().multiply(f1))
					.add(c2.clone().multiply(f2))
					.add(end.clone().multiply(f3));

			vectors.add(weightedSum.multiply(1/basis));

			// filling in any gaps
			if (i > 0) {
				Vector e1 = vectors.get(i).clone().normalize();
				Vector e2 = vectors.get(i-1).clone().normalize();

				if (e1.dot(e2) > Math.cos(Math.PI/18)) {
					vectors.add(vectors.get(i).getMidpoint(vectors.get(i-1)));
				}
			}
		}
		return vectors;
	}
}
