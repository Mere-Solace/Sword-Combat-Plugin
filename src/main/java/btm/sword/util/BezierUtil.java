package btm.sword.util;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BezierUtil {
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

    public static List<Vector> adjustCtrlToBasis(List<Vector> basis, List<Vector> controlVectors, double multiplier) {
        return controlVectors.stream().map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(multiplier)).toList();
    }

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
