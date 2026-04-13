package btm.sword.system.attack.simulation;

import org.joml.Vector3f;

/**
 * A {@link Volume} representing a capsule (swept sphere along a line segment).
 * Used as the output buffer for {@link SweepTrajectory}.
 */
public final class CapsuleVolume extends Volume {

    /** Capsule segment start in world space. */
    public final Vector3f start = new Vector3f();

    /** Capsule segment end in world space. */
    public final Vector3f end = new Vector3f();

    /** Capsule radius. */
    public float radius;

    /**
     * {@inheritDoc}
     * Delegates to {@link CollisionDetector#capsuleVsAabb}.
     */
    @Override
    public boolean intersects(Vector3f entityMin, Vector3f entityMax) {
        return CollisionDetector.capsuleVsAabb(start, end, radius, entityMin, entityMax);
    }
}
