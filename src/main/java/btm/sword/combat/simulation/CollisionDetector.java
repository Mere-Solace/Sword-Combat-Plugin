package btm.sword.combat.simulation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pure JOML narrow-phase collision detection for the off-thread {@code VolumeSimulation}.
 * <p>
 * All methods are static and stateless — zero Bukkit calls. Three primitives are supported:
 * capsule, OBB (oriented bounding box), and sphere, each tested against an axis-aligned
 * bounding box.
 * </p>
 */
public final class CollisionDetector {

    private CollisionDetector() {}

    /**
     * Tests a capsule (swept sphere along a line segment) against an AABB.
     * <p>
     * Algorithm: project the AABB center onto the segment to find the closest point,
     * then perform a sphere-vs-AABB test at that point.
     * </p>
     *
     * @param start    capsule segment start
     * @param end      capsule segment end
     * @param radius   capsule radius
     * @param aabbMin  AABB minimum corner
     * @param aabbMax  AABB maximum corner
     * @return {@code true} if the capsule overlaps the AABB
     */
    public static boolean capsuleVsAabb(Vector3f start, Vector3f end, float radius,
                                        Vector3f aabbMin, Vector3f aabbMax) {
        Vector3f dir = new Vector3f(end).sub(start);
        float lenSq = dir.lengthSquared();

        Vector3f closest;
        if (lenSq < 1e-6f) {
            closest = new Vector3f(start);
        } else {
            Vector3f aabbCenter = new Vector3f(aabbMin).add(aabbMax).mul(0.5f);
            float t = new Vector3f(aabbCenter).sub(start).dot(dir) / lenSq;
            t = Math.max(0f, Math.min(1f, t));
            closest = new Vector3f(start).add(new Vector3f(dir).mul(t));
        }

        return sphereVsAabb(closest, radius, aabbMin, aabbMax);
    }

    /**
     * Tests an OBB against an AABB using the Separating Axis Theorem (SAT).
     * <p>
     * Tests 15 axes: 3 AABB face normals, 3 OBB face normals, and 9 edge cross-products.
     * If any axis separates the shapes, they do not overlap.
     * </p>
     *
     * @param obbCenter  OBB center in world space
     * @param halfExtents OBB half-extents along its local axes
     * @param obbRot     OBB orientation as a unit quaternion
     * @param aabbMin    AABB minimum corner
     * @param aabbMax    AABB maximum corner
     * @return {@code true} if the OBB overlaps the AABB
     */
    public static boolean obbVsAabb(Vector3f obbCenter, Vector3f halfExtents, Quaternionf obbRot,
                                    Vector3f aabbMin, Vector3f aabbMax) {
        // OBB local axes (rotation matrix columns)
        Vector3f u0 = obbRot.transform(new Vector3f(1, 0, 0));
        Vector3f u1 = obbRot.transform(new Vector3f(0, 1, 0));
        Vector3f u2 = obbRot.transform(new Vector3f(0, 0, 1));
        Vector3f[] u = {u0, u1, u2};

        // AABB half-extents and center
        Vector3f ae = new Vector3f(aabbMax).sub(aabbMin).mul(0.5f);
        Vector3f ac = new Vector3f(aabbMin).add(aabbMax).mul(0.5f);

        // Vector between centers
        Vector3f t = new Vector3f(obbCenter).sub(ac);

        // 3 AABB face normals
        if (separated(new Vector3f(1, 0, 0), t, ae, halfExtents, u)) return false;
        if (separated(new Vector3f(0, 1, 0), t, ae, halfExtents, u)) return false;
        if (separated(new Vector3f(0, 0, 1), t, ae, halfExtents, u)) return false;

        // 3 OBB face normals
        if (separated(u0, t, ae, halfExtents, u)) return false;
        if (separated(u1, t, ae, halfExtents, u)) return false;
        if (separated(u2, t, ae, halfExtents, u)) return false;

        // 9 edge cross-product axes
        Vector3f[] a = {new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)};
        for (Vector3f ai : a) {
            for (Vector3f uj : u) {
                Vector3f cross = new Vector3f(ai).cross(uj);
                if (separated(cross, t, ae, halfExtents, u)) return false;
            }
        }

        return true;
    }

    /**
     * Tests a sphere against an AABB.
     * <p>
     * Algorithm: clamp the sphere center to the AABB and check if the distance
     * squared is within the sphere's radius squared.
     * </p>
     *
     * @param center   sphere center
     * @param radius   sphere radius
     * @param aabbMin  AABB minimum corner
     * @param aabbMax  AABB maximum corner
     * @return {@code true} if the sphere overlaps the AABB
     */
    public static boolean sphereVsAabb(Vector3f center, float radius, Vector3f aabbMin, Vector3f aabbMax) {
        float dx = Math.max(aabbMin.x - center.x, Math.max(0f, center.x - aabbMax.x));
        float dy = Math.max(aabbMin.y - center.y, Math.max(0f, center.y - aabbMax.y));
        float dz = Math.max(aabbMin.z - center.z, Math.max(0f, center.z - aabbMax.z));
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * SAT separating-axis test. Returns {@code true} if the given axis separates the two shapes.
     * Degenerate (near-zero) axes are skipped by returning {@code false}.
     *
     * @param axis        candidate separating axis
     * @param t           vector between AABB center and OBB center
     * @param ae          AABB half-extents
     * @param oe          OBB half-extents
     * @param u           OBB local axes
     * @return {@code true} if this axis proves separation
     */
    private static boolean separated(Vector3f axis, Vector3f t, Vector3f ae, Vector3f oe, Vector3f[] u) {
        if (axis.lengthSquared() < 1e-6f) return false;
        float d = Math.abs(t.dot(axis));
        float pA = Math.abs(ae.x * axis.x) + Math.abs(ae.y * axis.y) + Math.abs(ae.z * axis.z);
        float pB = Math.abs(oe.x * u[0].dot(axis)) + Math.abs(oe.y * u[1].dot(axis)) + Math.abs(oe.z * u[2].dot(axis));
        return d > pA + pB;
    }
}
