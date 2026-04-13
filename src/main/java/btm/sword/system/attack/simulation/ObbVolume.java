package btm.sword.system.attack.simulation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A {@link Volume} representing an oriented bounding box (OBB).
 * Used as the output buffer for {@link KeyframedTrajectory}.
 */
public final class ObbVolume extends Volume {

    /** OBB center in world space. */
    public final Vector3f center = new Vector3f();

    /** OBB half-extents along its local axes. */
    public final Vector3f halfExtents = new Vector3f();

    /** OBB orientation as a unit quaternion. */
    public final Quaternionf rotation = new Quaternionf();

    /**
     * {@inheritDoc}
     * Delegates to {@link CollisionDetector#obbVsAabb}.
     */
    @Override
    public boolean intersects(Vector3f entityMin, Vector3f entityMax) {
        return CollisionDetector.obbVsAabb(center, halfExtents, rotation, entityMin, entityMax);
    }
}
