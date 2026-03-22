package btm.sword.system.join;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import btm.sword.config.Config;

/**
 * Manages a matrix of off-screen "staging slots" used during the server join sequence.
 * <p>
 * Each slot is a fixed position in a configured world. When a player joins, they are
 * teleported to the best available slot and held there (invisible, zero velocity) while
 * the menu scene loads. The slot is released when the player leaves or enters gameplay.
 * </p>
 *
 * <h2>Grid layout</h2>
 * The grid grows from {@link Config.MenuGrid#ORIGIN_X}/{@link Config.MenuGrid#ORIGIN_Z}
 * along positive X (rows) and positive Z (columns) with configurable spacing.
 * A single black-concrete block is placed one unit below each slot centre as a platform.
 *
 * <h2>Slot selection</h2>
 * Slots closest to the grid centre are preferred (Manhattan distance), so the occupied
 * region stays compact rather than filling from a corner.
 */
public final class MenuSlotGrid {

    /** UUID → {row, col} for all currently occupied slots. */
    private static final Map<UUID, int[]> OCCUPIED = new HashMap<>();

    private MenuSlotGrid() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Claims the most-central free slot for the given player.
     * <p>
     * Idempotent: if the player already has a slot, that slot's location is returned without
     * acquiring a new one.
     * </p>
     *
     * @param playerUuid the player who will occupy the slot
     * @return the world location of the claimed slot centre, or empty if the grid is full
     *         or the world cannot be resolved
     */
    public static Optional<Location> acquireSlot(UUID playerUuid) {
        int[] existing = OCCUPIED.get(playerUuid);
        if (existing != null) {
            return Optional.ofNullable(slotCenter(existing));
        }

        int rows = Config.MenuGrid.ROWS;
        int cols = Config.MenuGrid.COLS;
        double centerRow = (rows - 1) / 2.0;
        double centerCol = (cols - 1) / 2.0;

        return IntStream.range(0, rows)
            .boxed()
            .flatMap(i -> IntStream.range(0, cols).mapToObj(j -> new int[]{i, j}))
            .filter(rc -> !isOccupied(rc[0], rc[1]))
            .min(Comparator.comparingDouble(rc ->
                Math.abs(rc[0] - centerRow) + Math.abs(rc[1] - centerCol)))
            .map(rc -> {
                OCCUPIED.put(playerUuid, rc);
                return slotCenter(rc);
            });
    }

    /**
     * Returns the location of the slot currently held by this player, or empty if they
     * have no slot.
     *
     * @param playerUuid the player to look up
     * @return the slot's world location, or empty if no slot is held
     */
    public static Optional<Location> getSlot(UUID playerUuid) {
        int[] rc = OCCUPIED.get(playerUuid);
        if (rc == null) return Optional.empty();
        return Optional.ofNullable(slotCenter(rc));
    }

    /**
     * Releases the slot held by the given player. No-op if the player has no slot.
     *
     * @param playerUuid the player whose slot to free
     */
    public static void releaseSlot(UUID playerUuid) {
        OCCUPIED.remove(playerUuid);
    }

    /**
     * Releases all occupied slots. Call on plugin disable.
     */
    public static void releaseAll() {
        OCCUPIED.clear();
    }

    /**
     * Places a single black-concrete platform block one unit below each slot centre in the
     * configured world. No-op if the world cannot be resolved.
     * <p>
     * Safe to call on plugin enable — worlds are loaded before {@code onEnable()} on Paper.
     * </p>
     */
    public static void placeAllBlocks() {
        World world = resolveWorld();
        if (world == null) return;

        int rows = Config.MenuGrid.ROWS;
        int cols = Config.MenuGrid.COLS;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Location floor = slotCenter(new int[]{i, j});
                if (floor == null) return;
                floor.clone().add(0, -1, 0).getBlock().setType(Material.BLACK_CONCRETE);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean isOccupied(int row, int col) {
        for (int[] rc : OCCUPIED.values()) {
            if (rc[0] == row && rc[1] == col) return true;
        }
        return false;
    }

    /**
     * Computes the world-space centre of a grid slot.
     *
     * @param rc {row, col} indices
     * @return the location, or {@code null} if the world cannot be resolved
     */
    static Location slotCenter(int[] rc) {
        World world = resolveWorld();
        if (world == null) return null;
        return new Location(
            world,
            Config.MenuGrid.ORIGIN_X + rc[0] * Config.MenuGrid.SPACING_X,
            Config.MenuGrid.ORIGIN_Y,
            Config.MenuGrid.ORIGIN_Z + rc[1] * Config.MenuGrid.SPACING_Z
        );
    }

    private static World resolveWorld() {
        World world = Bukkit.getWorld(Config.MenuGrid.WORLD);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }
}
