package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for the roguelike wave-run gamemode.
 * <p>
 * Controls the spawn center location, spawn radius, and per-wave enemy counts.
 * All values are hot-reloadable via {@code /sword reload}.
 * </p>
 */
public final class RoguelikeConfig {

    private RoguelikeConfig() {}

    /** World name for the roguelike spawn center. */
    public static String SPAWN_WORLD = "world";

    /** X coordinate of the spawn center. */
    public static double SPAWN_X = 0.0;

    /** Y coordinate of the spawn center. */
    public static double SPAWN_Y = 64.0;

    /** Z coordinate of the spawn center. */
    public static double SPAWN_Z = 0.0;

    /** Radius within which enemies spawn around the center, in blocks. */
    public static double SPAWN_RADIUS = 8.0;

    /** Number of Pillagers spawned in wave 1. */
    public static int WAVE_1_PILLAGERS = 3;

    /** Number of Wither Skeletons spawned in wave 2. */
    public static int WAVE_2_WITHER_SKELETONS = 3;

    /** Number of Pillagers spawned in wave 3. */
    public static int WAVE_3_PILLAGERS = 2;

    /** Number of Wither Skeletons spawned in wave 3. */
    public static int WAVE_3_WITHER_SKELETONS = 3;

    static {
        register("roguelike.spawn_world", SPAWN_WORLD, String.class,
            v -> SPAWN_WORLD = v, ConfigurationSection::getString);
        register("roguelike.spawn_x", SPAWN_X, Double.class,
            v -> SPAWN_X = v, ConfigurationSection::getDouble);
        register("roguelike.spawn_y", SPAWN_Y, Double.class,
            v -> SPAWN_Y = v, ConfigurationSection::getDouble);
        register("roguelike.spawn_z", SPAWN_Z, Double.class,
            v -> SPAWN_Z = v, ConfigurationSection::getDouble);
        register("roguelike.spawn_radius", SPAWN_RADIUS, Double.class,
            v -> SPAWN_RADIUS = v, ConfigurationSection::getDouble);
        register("roguelike.wave_1_pillagers", WAVE_1_PILLAGERS, Integer.class,
            v -> WAVE_1_PILLAGERS = v, ConfigurationSection::getInt);
        register("roguelike.wave_2_wither_skeletons", WAVE_2_WITHER_SKELETONS, Integer.class,
            v -> WAVE_2_WITHER_SKELETONS = v, ConfigurationSection::getInt);
        register("roguelike.wave_3_pillagers", WAVE_3_PILLAGERS, Integer.class,
            v -> WAVE_3_PILLAGERS = v, ConfigurationSection::getInt);
        register("roguelike.wave_3_wither_skeletons", WAVE_3_WITHER_SKELETONS, Integer.class,
            v -> WAVE_3_WITHER_SKELETONS = v, ConfigurationSection::getInt);
    }
}
