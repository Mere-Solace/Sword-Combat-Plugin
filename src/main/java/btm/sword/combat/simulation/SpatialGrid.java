package btm.sword.combat.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

/**
 * Broad-phase spatial index for the off-thread {@code VolumeSimulation}.
 * <p>
 * Divides world space into 2-block axis-aligned cells, reducing volume-vs-entity
 * collision checks from O(n²) to only entries in overlapping cells. Rebuilt from
 * scratch each 5ms tick before the narrow phase runs.
 * </p>
 *
 * <p>Usage per tick:</p>
 * <ol>
 *   <li>Call {@link #clear()} to reset all cells.</li>
 *   <li>Call {@link #insert(UUID, Vector3f, Vector3f)} for each active attack volume.</li>
 *   <li>Call {@link #query(Vector3f, Vector3f)} for each entity AABB to get candidate volumes.</li>
 *   <li>Pass candidates to {@link CollisionDetector} for narrow-phase testing.</li>
 * </ol>
 */
public final class SpatialGrid {

    private static final float CELL_SIZE = 2f;

    private final HashMap<CellKey, List<VolumeEntry>> cells = new HashMap<>();

    /**
     * Resets all cells. Must be called at the start of each simulation tick.
     */
    public void clear() {
        cells.clear();
    }

    /**
     * Inserts a volume into every cell it overlaps.
     *
     * @param owner UUID of the entity that owns this attack volume
     * @param min   world-space minimum corner of the volume AABB
     * @param max   world-space maximum corner of the volume AABB
     */
    public void insert(UUID owner, Vector3f min, Vector3f max) {
        VolumeEntry entry = new VolumeEntry(owner, min, max);
        int[] range = cellRange(min, max);
        for (int ix = range[0]; ix <= range[3]; ix++) {
            for (int iy = range[1]; iy <= range[4]; iy++) {
                for (int iz = range[2]; iz <= range[5]; iz++) {
                    cells.computeIfAbsent(new CellKey(ix, iy, iz), k -> new ArrayList<>()).add(entry);
                }
            }
        }
    }

    /**
     * Returns all distinct {@link VolumeEntry} instances whose cells overlap the given AABB.
     * Entries that span multiple cells appear only once in the result.
     *
     * @param min world-space minimum corner of the query AABB
     * @param max world-space maximum corner of the query AABB
     * @return deduplicated list of candidate volumes
     */
    public List<VolumeEntry> query(Vector3f min, Vector3f max) {
        int[] range = cellRange(min, max);
        Set<VolumeEntry> result = new LinkedHashSet<>();
        for (int ix = range[0]; ix <= range[3]; ix++) {
            for (int iy = range[1]; iy <= range[4]; iy++) {
                for (int iz = range[2]; iz <= range[5]; iz++) {
                    List<VolumeEntry> cell = cells.get(new CellKey(ix, iy, iz));
                    if (cell != null) result.addAll(cell);
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Computes the inclusive cell-index range for a given AABB.
     * Returns {@code [minIx, minIy, minIz, maxIx, maxIy, maxIz]}.
     */
    private static int[] cellRange(Vector3f min, Vector3f max) {
        return new int[]{
            (int) Math.floor(min.x / CELL_SIZE),
            (int) Math.floor(min.y / CELL_SIZE),
            (int) Math.floor(min.z / CELL_SIZE),
            (int) Math.floor(max.x / CELL_SIZE),
            (int) Math.floor(max.y / CELL_SIZE),
            (int) Math.floor(max.z / CELL_SIZE)
        };
    }

    /**
     * Integer cell coordinates derived from world position divided by {@code CELL_SIZE}.
     *
     * @param ix cell index along the X axis
     * @param iy cell index along the Y axis
     * @param iz cell index along the Z axis
     */
    public record CellKey(int ix, int iy, int iz) {}

    /**
     * An attack volume registered in the grid.
     *
     * @param ownerUuid UUID of the entity that owns this volume
     * @param min       world-space minimum corner
     * @param max       world-space maximum corner
     */
    public record VolumeEntry(UUID ownerUuid, Vector3f min, Vector3f max) {}
}
