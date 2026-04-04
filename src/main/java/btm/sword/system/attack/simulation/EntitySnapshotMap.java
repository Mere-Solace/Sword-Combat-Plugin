package btm.sword.system.attack.simulation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.util.BoundingBox;
import org.joml.Vector3f;

/**
 * Thread-safe registry of entity bounding box snapshots for off-thread collision detection.
 * <p>
 * The main thread writes snapshots every tick via {@link #snapshot(UUID, BoundingBox, float)},
 * called from {@code SwordEntity.onTick()}. The 5ms {@code VolumeSimulation} thread reads
 * immutable {@link EntityBoundingBoxSnapshot} records via {@link #get(UUID)} and iterates
 * all entries via {@link #entrySet()} — zero Bukkit calls required on the simulation side.
 * </p>
 */
public final class EntitySnapshotMap {

    /** Global singleton instance. */
    public static final EntitySnapshotMap INSTANCE = new EntitySnapshotMap();

    private final ConcurrentHashMap<UUID, EntityBoundingBoxSnapshot> snapshots = new ConcurrentHashMap<>();

    private EntitySnapshotMap() {}

    /**
     * Writes an immutable AABB snapshot for the given entity.
     * Must be called from the main thread.
     *
     * @param uuid entity UUID
     * @param bb   current Bukkit bounding box for the entity
     * @param yaw  entity yaw in degrees (Bukkit convention), used to build world transforms
     */
    public void snapshot(UUID uuid, BoundingBox bb, float yaw) {
        Vector3f min = new Vector3f((float) bb.getMinX(), (float) bb.getMinY(), (float) bb.getMinZ());
        Vector3f max = new Vector3f((float) bb.getMaxX(), (float) bb.getMaxY(), (float) bb.getMaxZ());
        Vector3f center = new Vector3f((float) bb.getCenterX(), (float) bb.getCenterY(), (float) bb.getCenterZ());
        snapshots.put(uuid, new EntityBoundingBoxSnapshot(min, max, center, yaw));
    }

    /**
     * Returns the most recent snapshot for the given entity, or {@code null} if none exists.
     * Safe to call from any thread.
     *
     * @param uuid entity UUID
     * @return immutable snapshot, or {@code null}
     */
    public EntityBoundingBoxSnapshot get(UUID uuid) {
        return snapshots.get(uuid);
    }

    /**
     * Returns a live view of all current snapshots for iteration on the simulation thread.
     * Safe to call from any thread.
     *
     * @return entry set of all registered entity snapshots
     */
    public Set<Map.Entry<UUID, EntityBoundingBoxSnapshot>> entrySet() {
        return snapshots.entrySet();
    }

    /**
     * Immutable AABB snapshot of an entity's bounding box at a point in time.
     *
     * @param min    minimum corner in world space
     * @param max    maximum corner in world space
     * @param center center point in world space
     * @param yaw    entity yaw in degrees (Bukkit convention)
     */
    public record EntityBoundingBoxSnapshot(Vector3f min, Vector3f max, Vector3f center, float yaw) {}
}
