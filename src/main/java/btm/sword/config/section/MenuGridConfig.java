package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for the server-join staging grid.
 * <p>
 * The grid is a matrix of off-screen platform slots. Each player occupies one slot
 * from join until they enter gameplay. The grid is placed at the configured origin and
 * grows along X (rows) and Z (columns) with the given spacing.
 * {@link btm.sword.join.MenuSlotGrid} reads these values and manages occupancy.
 * </p>
 */
public final class MenuGridConfig {

    private MenuGridConfig() {}

    /** World name for the staging grid. Falls back to the default world if not found. */
    public static String WORLD = "world";

    /** X coordinate of slot (0, 0) — grid grows in the positive-X direction. */
    public static double ORIGIN_X = 41.5;

    /** Y level of the grid platform surface (player stands at ORIGIN_Y). */
    public static double ORIGIN_Y = 255.0;

    /** Z coordinate of slot (0, 0) — grid grows in the positive-Z direction. */
    public static double ORIGIN_Z = -834.5;

    /** Number of rows (along X). */
    public static int ROWS = 11;

    /** Number of columns (along Z). */
    public static int COLS = 11;

    /** Distance in blocks between slot centres along X. */
    public static double SPACING_X = 5.0;

    /** Distance in blocks between slot centres along Z. */
    public static double SPACING_Z = 5.0;

    /**
     * Milliseconds to hold the player in their dark-room staging slot before starting
     * the join sequence. Allows the client to finish loading chunks and terrain.
     */
    public static int LOADING_WAIT_MS = 1500;

    static {
        register("menu_grid.world", WORLD, String.class,
            v -> WORLD = v, ConfigurationSection::getString);
        register("menu_grid.origin_x", ORIGIN_X, Double.class,
            v -> ORIGIN_X = v, ConfigurationSection::getDouble);
        register("menu_grid.origin_y", ORIGIN_Y, Double.class,
            v -> ORIGIN_Y = v, ConfigurationSection::getDouble);
        register("menu_grid.origin_z", ORIGIN_Z, Double.class,
            v -> ORIGIN_Z = v, ConfigurationSection::getDouble);
        register("menu_grid.rows", ROWS, Integer.class,
            v -> ROWS = v, ConfigurationSection::getInt);
        register("menu_grid.cols", COLS, Integer.class,
            v -> COLS = v, ConfigurationSection::getInt);
        register("menu_grid.spacing_x", SPACING_X, Double.class,
            v -> SPACING_X = v, ConfigurationSection::getDouble);
        register("menu_grid.spacing_z", SPACING_Z, Double.class,
            v -> SPACING_Z = v, ConfigurationSection::getDouble);
        register("menu_grid.loading_wait_ms", LOADING_WAIT_MS, Integer.class,
            v -> LOADING_WAIT_MS = v, ConfigurationSection::getInt);
    }
}
