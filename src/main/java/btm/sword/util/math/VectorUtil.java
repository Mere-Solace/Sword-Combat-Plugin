package btm.sword.util.math;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import btm.sword.config.Config;


/**
 * Utility class providing mathematical operations and geometric transformations for {@link Vector}s.
 * <p>
 * These methods are primarily used for constructing orthogonal bases, performing rotations,
 * and converting between different coordinate frames in 3D space.
 * </p>
 */
public class VectorUtil {
    public static Basis getBasis(Location origin, Vector dir) {
        dir.normalize();
        if (dir.isZero()) // just in case, return a default basis
            return new Basis(
                Config.Direction.UP().crossProduct(Config.Direction.SOUTH()),
                Config.Direction.UP(),
                Config.Direction.SOUTH());

        Vector ref = new Vector(0,1,0);
        Vector right = null;

        double dot = dir.dot(ref);

        if (Math.abs(dot) > 0.999) {
            double yaw = Math.toRadians(origin.getYaw());
            ref = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
            right = dot >= 0 ? ref.getCrossProduct(dir).normalize() : dir.getCrossProduct(ref).normalize();
        }

        if (right == null)
            right = dir.getCrossProduct(ref).normalize();

        Vector up = right.getCrossProduct(dir).normalize();

        return new Basis(right, up, dir);
    }

    public static Basis getBasisWithoutPitch(Entity origin) {
        Vector up = Config.Direction.UP();
        double yaw;
        if (origin instanceof Player player) {
            yaw = Math.toRadians(player.getBodyYaw());
        }
        else {
            yaw = Math.toRadians(origin.getYaw());
        }
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = dir.getCrossProduct(up).normalize();

        return new Basis(right, up, dir);
    }

    public static Basis getBasisWithoutPitch(Location location) {
        Vector up = Config.Direction.UP();
        double yaw = Math.toRadians(location.getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = dir.getCrossProduct(up).normalize();

        return new Basis(right, up, dir);
    }

    /**
     * Rotates an existing basis around its local axes.
     * <p>
     * This method applies a roll rotation around the forward axis,
     * followed by a yaw rotation around the up axis.
     * </p>
     *
     * @param basis The list of basis vectors in order [right, up, forward].
     * @param roll  The roll angle in radians (rotation around the forward vector).
     * @param yaw   The yaw angle in radians (rotation around the up vector).
     */
    public static void rotateBasis(List<Vector> basis, double roll, double yaw) {
        basis.get(1).rotateAroundAxis(basis.getLast(), -roll);
        basis.getFirst().rotateAroundAxis(basis.getLast(), -roll);

        basis.getLast().rotateAroundAxis(basis.get(1), yaw);
        basis.getFirst().rotateAroundAxis(basis.get(1), yaw);
    }

    /**
     * Transforms a vector expressed in local coordinates into world-space coordinates,
     * using a given orthonormal basis.
     * <p>
     * Essentially computes {@code v_world = right*x + up*y + forward*z}.
     * </p>
     *
     * @param basis The basis vectors [right, up, forward].
     * @param v     The local vector to transform.
     * @return The transformed vector in world-space coordinates.
     */
    public static Vector transformWithNewBasis(Basis basis, Vector v) {
        Vector right = basis.right();
        Vector up = basis.up();
        Vector forward = basis.forward();

        return right.clone().multiply(v.getX())
                .add(up.clone().multiply(v.getY()))
                .add(forward.clone().multiply(v.getZ()));
    }

    /**
     * Projects a vector onto a plane defined by its normal vector.
     *
     * @param v     The vector to project.
     * @param norm  The normal vector of the plane (does not need to be normalized).
     * @return The component of {@code v} that lies on the plane.
     */
    public static Vector getProjOntoPlane(Vector v, Vector norm) {
        return v.clone().subtract(norm.clone().multiply(v.dot(norm)/norm.lengthSquared()));
    }

    /**
     * Computes the pitch angle (vertical rotation) of a vector in degrees.
     * <p>
     * The angle is measured relative to the horizontal plane,
     * where 0° is level and -90° is straight up.
     * </p>
     *
     * @param v The vector to measure.
     * @return The pitch angle in degrees.
     */
    public static double getPitch(Vector v) {
        double x = v.getX();
        double y = v.getY();
        double z = v.getZ();

        double horizontalDist = Math.sqrt(x * x + z * z);
        return Math.toDegrees(Math.atan2(-y, horizontalDist));
    }

    /**
     * Computes the yaw angle (horizontal rotation) of a vector in degrees.
     * <p>
     * The angle follows the same convention as Bukkit:
     * 0° faces positive Z, 90° faces negative X.
     * </p>
     *
     * @param v The vector to measure.
     * @return The yaw angle in degrees.
     */
    public static double getYaw(Vector v) {
        return Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
    }
}
