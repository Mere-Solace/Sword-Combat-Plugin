package btm.sword.system.attack.simulation;

import org.joml.Vector3f;

/**
 * Mutable, reusable output buffer populated by {@link VolumeTrajectory#sample} each simulation tick.
 * <p>
 * Instances are allocated once per {@link ActiveAttack} and reused across ticks to avoid
 * per-frame heap allocation in the 200 Hz simulation loop.
 * </p>
 *
 * <p>Concrete subclasses carry shape-specific fields and implement {@link #intersects}
 * to delegate to the appropriate {@link CollisionDetector} method:
 * {@link ObbVolume} for keyframed OBB attacks, {@link CapsuleVolume} for sweep attacks.</p>
 */
public abstract class Volume {

    /** World-space AABB minimum corner — written by {@link VolumeTrajectory#sample}, read by {@link SpatialGrid}. */
    public final Vector3f aabbMin = new Vector3f();

    /** World-space AABB maximum corner — written by {@link VolumeTrajectory#sample}, read by {@link SpatialGrid}. */
    public final Vector3f aabbMax = new Vector3f();

    /**
     * Narrow-phase intersection test against an entity bounding box.
     * Delegates to the appropriate {@link CollisionDetector} method for this volume shape.
     *
     * @param entityMin entity AABB minimum corner
     * @param entityMax entity AABB maximum corner
     * @return {@code true} if this volume overlaps the entity's bounding box
     */
    public abstract boolean intersects(Vector3f entityMin, Vector3f entityMax);
}
