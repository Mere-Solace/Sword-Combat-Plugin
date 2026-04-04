package btm.sword.system.attack.simulation;

import org.joml.Matrix4f;

/**
 * Evaluates an attack volume's position and shape at a given point in time.
 * <p>
 * Implementations transform local-space geometry into world space using the attacker's
 * world transform, then populate a {@link VolumeOutput} in place for use by the
 * {@link VolumeSimulation} broad and narrow phases.
 * </p>
 *
 * <p>Concrete implementations: {@code KeyframedTrajectory} (OBB volumes) and
 * {@code SweepTrajectory} (Catmull-Rom capsule sweeps).</p>
 */
public interface Trajectory {

    /**
     * Allocates the appropriately typed {@link VolumeOutput} buffer for this trajectory.
     * Called once when an {@link ActiveAttack} is constructed; the result is reused
     * every simulation tick to avoid per-frame allocation.
     *
     * @return a new, empty output buffer compatible with this trajectory type
     */
    VolumeOutput createOutput();

    /**
     * Populates {@code out} with the volume's world-space geometry at normalized time {@code t}.
     * <p>
     * Must set {@code out.aabbMin} and {@code out.aabbMax} for broad-phase insertion into
     * {@link SpatialGrid}, and any shape-specific fields required by
     * {@link VolumeOutput#intersects(org.joml.Vector3f, org.joml.Vector3f)}.
     * </p>
     *
     * @param t              normalized attack time, {@code 0.0} to {@code 1.0}
     * @param worldTransform attacker's world transform (translation + yaw rotation)
     * @param out            mutable output buffer to populate in place
     */
    void sample(float t, Matrix4f worldTransform, VolumeOutput out);
}
