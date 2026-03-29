package btm.sword.utility.math;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.entity.base.SwordEntity;


/**
 * Utility class providing mathematical operations and geometric transformations for {@link Vector}s.
 * <p>
 * These methods are primarily used for constructing orthogonal bases, performing rotations,
 * and converting between different coordinate frames in 3D space.
 * </p>
 */
public final class VectorUtil {

    private VectorUtil() {}

    public static Basis getBasis(Location origin, Vector dir) {
        dir.normalize();
        if (dir.isZero()) { // just in case, return a default basis
            return new Basis(
                Config.Direction.up().crossProduct(Config.Direction.south()),
                Config.Direction.up(),
                Config.Direction.south());
        }

        Vector upReference = Config.Direction.up();
        Vector right;

        double isDirectionStraightUpOrDown = dir.dot(upReference);

        if (Math.abs(isDirectionStraightUpOrDown) > 0.999) {
            double yaw = Math.toRadians(origin.setDirection(dir).getYaw());
            Vector hzntlReference = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
            right = isDirectionStraightUpOrDown >= 0 ?
                hzntlReference.getCrossProduct(dir).normalize() :
                dir.getCrossProduct(hzntlReference).normalize();
        }
        else {
            right = dir.getCrossProduct(upReference).normalize();
        }

        Vector up = right.getCrossProduct(dir).normalize();

        return new Basis(right, up, dir);
    }

    public static Basis getBasisWithoutPitch(Entity origin) {
        Vector up = Config.Direction.up();
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
        Vector up = Config.Direction.up();
        double yaw = Math.toRadians(location.getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = dir.getCrossProduct(up).normalize();

        return new Basis(right, up, dir);
    }

    public static boolean isBroken(Vector v) {
        return Double.isNaN(v.getX()) || Double.isNaN(v.getY()) || Double.isNaN(v.getZ())
            || Double.isInfinite(v.getX()) || Double.isInfinite(v.getY()) || Double.isInfinite(v.getZ());
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
        return v.clone().subtract(norm.clone().multiply(v.dot(norm) / norm.lengthSquared()));
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

    public static double getAngleBetweenTwoVectors(Vector v, Vector u) {
        double vNorm = v.length();
        double uNorm = u.length();
        if (vNorm == 0 || uNorm == 0) {
            return Math.PI / 2; // Don't want an exception to be thrown, so return a default value of 90 degrees
        }

        double cos = Math.max(-1.0, Math.min(1.0, v.dot(u) / (vNorm * uNorm)));
        return Math.acos(cos); // radians
    }

    public static Vector getVectorTo(SwordEntity from, SwordEntity to, double scalar) {
        return to.getChestLocation().toVector()
            .subtract(from.getChestLocation().toVector())
            .normalize().multiply(scalar);
    }
}
