package btm.sword.system.attack.style.shape;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.system.attack.style.AttackShape;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;

/**
 * An {@link AttackShape} that sweeps along a circular arc in the attacker's horizontal plane.
 *
 * <p>The arc is centered on the attack origin and lies in the plane spanned by the attacker's
 * {@code right} and {@code forward} basis vectors, offset vertically by {@code height}.
 * Setting {@code endAngle - startAngle = 2π} produces a full 360° circle sweep.
 *
 * <p>Angles are measured in radians, where {@code 0} aligns with the attacker's {@code forward}
 * direction and positive angles rotate toward {@code right}. This matches the convention used
 * by {@link DrawUtil#circle}.
 *
 * <p>Use the {@link #of} factory for a standard local-space arc. The {@code rangeMultiplier}
 * passed to {@link #resolve} scales the {@code radius}.
 *
 * @param radius     distance from the attack origin to the arc path, in blocks
 * @param startAngle azimuthal start angle in radians ({@code 0} = forward)
 * @param endAngle   azimuthal end angle in radians
 * @param height     vertical offset from the origin to the arc plane, in blocks
 */
public record ArcShape(double radius,
                       double startAngle,
                       double endAngle,
                       double height) implements AttackShape {

    /**
     * Creates an {@code ArcShape}.
     *
     * @param radius     distance from the attack origin to the arc path, in blocks
     * @param startAngle azimuthal start angle in radians ({@code 0} = forward, positive = toward right)
     * @param endAngle   azimuthal end angle in radians; use {@code startAngle + 2π} for a full circle
     * @param height     vertical offset from the origin to the arc plane, in blocks
     * @return a new {@code ArcShape} with the given parameters
     */
    public static ArcShape of(double radius, double startAngle, double endAngle, double height) {
        return new ArcShape(radius, startAngle, endAngle, height);
    }

    /**
     * Creates a flat {@code ArcShape} with no vertical offset.
     *
     * @param radius     distance from the attack origin to the arc path, in blocks
     * @param startAngle azimuthal start angle in radians
     * @param endAngle   azimuthal end angle in radians
     * @return a new {@code ArcShape} at height {@code 0.0}
     */
    public static ArcShape of(double radius, double startAngle, double endAngle) {
        return new ArcShape(radius, startAngle, endAngle, 0.0);
    }

    @Override
    public Function<Double, Vector> resolve(Basis basis, double rangeMultiplier) {
        Vector forward = basis.forward();
        Vector up = basis.up();

        double scaledRadius = radius * rangeMultiplier;
        double angleRange = endAngle - startAngle;

        return t -> {
            double angle = startAngle + angleRange * t;
            // Rotate forward toward right by angle, matching DrawUtil.circle convention
            Vector radial = forward.clone().rotateAroundAxis(up, angle);
            return radial.multiply(scaledRadius).add(up.clone().multiply(height));
        };
    }
}
