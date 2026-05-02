package btm.sword.combat.simulation;

import org.joml.Matrix4fc;

/**
 * Evaluates an attack volume's position and shape at a normalized point in time.
 * <p>
 * Implementations transform local-space geometry into world space using the attacker's
 * world transform and populate a {@link Volume} in place for use by
 * {@link VolumeSimulation}'s broad and narrow phases.
 * </p>
 *
 * <p>Declared as a {@code @FunctionalInterface} so simple one-off attack shapes can be
 * expressed as lambdas. Concrete implementations: {@link KeyframedSequence} (OBB volumes)
 * and {@link SweepSequence} (Catmull-Rom capsule sweeps).</p>
 */
@FunctionalInterface
public interface VolumeSequence {

    /**
     * Populates {@code out} with the volume's world-space geometry at normalized time {@code t}.
     * <p>
     * Must set {@code out.aabbMin} and {@code out.aabbMax} for broad-phase insertion into
     * {@link SpatialGrid}, and any shape-specific fields required by
     * {@link Volume#intersects(org.joml.Vector3f, org.joml.Vector3f)}.
     * </p>
     *
     * @param t              normalized attack time, {@code 0.0} to {@code 1.0}
     * @param worldTransform attacker's world transform (translation + yaw rotation); read-only
     * @param out            mutable volume buffer to populate in place
     */
    void sample(float t, Matrix4fc worldTransform, Volume out);
}
