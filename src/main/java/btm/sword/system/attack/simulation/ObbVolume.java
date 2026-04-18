package btm.sword.system.attack.simulation;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A {@link Volume} representing an oriented bounding box (OBB) or sphere.
 * Used as the output buffer for {@link KeyframedTrajectory}.
 *
 * <p>When {@link #isSphere} is {@code true}, the volume was produced from a
 * {@link VolumeShape#SPHERE} keyframe. In that case {@link #halfExtents} holds a uniform
 * radius in all three components, and {@link #intersects} delegates to
 * {@link CollisionDetector#sphereVsAabb} using {@code halfExtents.x} as the radius.
 * Otherwise the full OBB path is used.</p>
 */
public final class ObbVolume extends Volume {

    /** OBB center in world space. */
    public final Vector3f center = new Vector3f();

    /** OBB half-extents along its local axes (or uniform radius when {@link #isSphere}). */
    public final Vector3f halfExtents = new Vector3f();

    /** OBB orientation as a unit quaternion. Ignored when {@link #isSphere} is {@code true}. */
    public final Quaternionf rotation = new Quaternionf();

    /**
     * When {@code true} this volume came from a {@link VolumeShape#SPHERE} keyframe.
     * Collision delegates to sphere-vs-AABB rather than the full OBB SAT test.
     * Written each tick by {@link KeyframedTrajectory#sample}.
     */
    public boolean isSphere = false;

    /**
     * World-space ray start for {@link KeyframeType#ORIGIN_RAY} and
     * {@link KeyframeType#RAYCAST} keyframes, written each tick by
     * {@link KeyframedTrajectory#sample}. {@code null} when the keyframe has no stored
     * ray origin — the simulation falls back to the player BB center.
     */
    @Nullable
    public Vector3f rayOrigin = null;

    /**
     * {@inheritDoc}
     * Delegates to {@link CollisionDetector#sphereVsAabb} when {@link #isSphere} is set,
     * otherwise to {@link CollisionDetector#obbVsAabb}.
     */
    @Override
    public boolean intersects(Vector3f entityMin, Vector3f entityMax) {
        if (isSphere) {
            return CollisionDetector.sphereVsAabb(center, halfExtents.x, entityMin, entityMax);
        }
        return CollisionDetector.obbVsAabb(center, halfExtents, rotation, entityMin, entityMax);
    }
}
