package btm.sword.system.attack.simulation;

import org.joml.Vector3f;

/**
 * Mutable, reusable output buffer populated by {@link Trajectory#sample} each simulation tick.
 * <p>
 * All instances are allocated once per {@link ActiveAttack} via {@link Trajectory#createOutput()}
 * and reused across ticks to avoid per-frame heap allocation in the 200 Hz simulation loop.
 * </p>
 *
 * <p>Subclasses carry the shape-specific fields ({@code ObbVolumeOutput},
 * {@code CapsuleVolumeOutput}) and implement {@link #intersects} to delegate to the
 * appropriate {@link CollisionDetector} method.</p>
 */
public abstract class VolumeOutput {

    /** World-space AABB minimum corner — written by {@link Trajectory#sample}, read by {@link SpatialGrid}. */
    public final Vector3f aabbMin = new Vector3f();

    /** World-space AABB maximum corner — written by {@link Trajectory#sample}, read by {@link SpatialGrid}. */
    public final Vector3f aabbMax = new Vector3f();

    /**
     * Narrow-phase intersection test against an entity bounding box.
     * Delegates to the appropriate {@link CollisionDetector} method based on the volume shape.
     *
     * @param entityMin entity AABB minimum corner
     * @param entityMax entity AABB maximum corner
     * @return {@code true} if the volume overlaps the entity's bounding box
     */
    public abstract boolean intersects(Vector3f entityMin, Vector3f entityMax);
}
