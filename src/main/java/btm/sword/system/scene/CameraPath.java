package btm.sword.system.scene;

import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import btm.sword.utility.math.BezierUtil;
import btm.sword.utility.math.ControlVectors;

/**
 * Value object representing a cubic Bézier camera trajectory in world space.
 * <p>
 * The path is defined by a {@link ControlVectors} whose suppliers resolve from
 * {@code Config.Scene} so that hot-reloaded values are picked up automatically.
 * </p>
 * <p>
 * Call {@link #evaluate(double, World)} with {@code t} in {@code [0, 1]} to obtain
 * a {@link Location} on the curve with yaw/pitch oriented along the tangent direction.
 * </p>
 */
public class CameraPath {

    private final ControlVectors control;

    /**
     * Constructs a camera path from the given Bézier control vectors.
     *
     * @param control the four world-space control points defining the cubic Bézier curve
     */
    public CameraPath(ControlVectors control) {
        this.control = control;
    }

    /**
     * Evaluates the camera path at parameter {@code t} and returns a world-space
     * {@link Location} with direction set along the curve's tangent.
     * <p>
     * Tangent is computed via central finite difference:
     * {@code (P(t+ε) − P(t−ε)).normalize()}.
     * </p>
     *
     * @param t     curve parameter in {@code [0.0, 1.0]}
     * @param world the world in which the location resides
     * @return a location on the curve facing along the travel direction
     */
    public Location evaluate(double t, World world) {
        Function<Double, Vector> curve = BezierUtil.cubicBezier3D(control);
        Vector pos = curve.apply(t);

        double eps = 0.001;
        double tA = Math.max(0.0, t - eps);
        double tB = Math.min(1.0, t + eps);
        Vector tangent = curve.apply(tB).subtract(curve.apply(tA));

        Location loc = pos.toLocation(world);
        if (tangent.lengthSquared() > 1e-10) {
            loc.setDirection(tangent.normalize());
        }
        return loc;
    }
}
