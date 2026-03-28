package btm.sword.system.attack.style.shape;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.system.attack.style.AttackShape;
import btm.sword.utility.math.Basis;

/**
 * An {@link AttackShape} that sweeps along the surface of a cone aligned to the attacker's
 * forward axis.
 *
 * <p>The cone's apex is at the attack origin. The surface is defined by a polar angle
 * ({@code halfAngle}) off the forward axis and a reach ({@code range}). The weapon path
 * sweeps azimuthally from {@code startSweepAngle} to {@code endSweepAngle} around the
 * forward axis, tracing the cone's rim at the given range.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Frontal fan swing: {@code halfAngle} of ~{@code π/6} (30°), partial sweep</li>
 *   <li>Full cone burst: {@code halfAngle} of ~{@code π/4} (45°), sweep of {@code 2π}</li>
 *   <li>Overhead crown slam: {@code halfAngle} near {@code π/2}, full azimuthal sweep</li>
 * </ul>
 *
 * <p>All angles are in radians. {@code startSweepAngle = 0} aligns with the attacker's
 * {@code right} vector in the plane perpendicular to forward.
 * The {@code rangeMultiplier} passed to {@link #resolve} scales the {@code range}.
 *
 * @param halfAngle       polar angle from the cone axis to its surface, in radians
 * @param range           reach from the apex to the rim, in blocks
 * @param startSweepAngle azimuthal start angle around the forward axis, in radians
 * @param endSweepAngle   azimuthal end angle around the forward axis, in radians
 */
public record ConeShape(double halfAngle, double range, double startSweepAngle,
                        double endSweepAngle) implements AttackShape {

    /**
     * Creates a {@code ConeShape}.
     *
     * @param halfAngle       polar angle from the forward axis to the cone surface, in radians
     * @param range           reach of the cone — distance from origin to the rim, in blocks
     * @param startSweepAngle azimuthal start angle around the forward axis, in radians
     * @param endSweepAngle   azimuthal end angle around the forward axis, in radians;
     *                        use {@code startSweepAngle + 2π} for a full revolution
     * @return a new {@code ConeShape} with the specified geometry
     */
    public static ConeShape of(double halfAngle, double range,
                               double startSweepAngle, double endSweepAngle) {
        return new ConeShape(halfAngle, range, startSweepAngle, endSweepAngle);
    }

    /**
     * Creates a symmetric {@code ConeShape} centered on the forward axis.
     *
     * <p>The sweep runs from {@code -halfSweep} to {@code +halfSweep} around the forward axis,
     * producing an attack that fans symmetrically left and right.
     *
     * @param halfAngle polar angle from the forward axis to the cone surface, in radians
     * @param range     reach of the cone, in blocks
     * @param halfSweep half the total azimuthal sweep, in radians
     * @return a new {@code ConeShape} sweeping from {@code -halfSweep} to {@code +halfSweep}
     */
    public static ConeShape symmetric(double halfAngle, double range, double halfSweep) {
        return new ConeShape(halfAngle, range, -halfSweep, halfSweep);
    }

    @Override
    public Function<Double, Vector> resolve(Basis basis, double rangeMultiplier) {
        Vector right = basis.right();
        Vector forward = basis.forward();
        Vector up = basis.up();

        double scaledRange = range * rangeMultiplier;
        double sweepRange = endSweepAngle - startSweepAngle;

        // Precompute cone surface components
        double axial = Math.cos(halfAngle);     // component along cone axis (forward)
        double radial = Math.sin(halfAngle);    // component perpendicular to axis (rim radius)

        return t -> {
            double phi = startSweepAngle + sweepRange * t;
            // Point on cone surface: axial component along forward + radial component in right/up plane
            Vector axialComponent = forward.clone().multiply(axial);
            Vector radialComponent = right.clone().multiply(Math.cos(phi))
                .add(up.clone().multiply(Math.sin(phi)));
            return axialComponent.add(radialComponent.multiply(radial)).multiply(scaledRange);
        };
    }
}
