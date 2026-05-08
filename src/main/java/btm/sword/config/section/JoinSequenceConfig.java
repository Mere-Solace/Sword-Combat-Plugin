package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import btm.sword.config.Config;

/**
 * Configuration for the server join routing pipeline.
 * <p>
 * Two world locations are used:
 * </p>
 * <ul>
 *   <li><b>Spawnpoint</b> — where returning players (join sequence complete) are placed
 *       before the main menu character scene opens.</li>
 *   <li><b>Opening</b> — where first-time players are teleported for the intro cutscene.</li>
 * </ul>
 */
public final class JoinSequenceConfig {

    private JoinSequenceConfig() {}

    /** World for both the spawnpoint and the opening animation location. */
    public static String WORLD = "world";

    /** X coordinate of the returning-player spawnpoint. */
    public static double SPAWNPOINT_X = -132.0;

    /** Y coordinate of the returning-player spawnpoint. */
    public static double SPAWNPOINT_Y = 215.0;

    /** Z coordinate of the returning-player spawnpoint. */
    public static double SPAWNPOINT_Z = -783.0;

    /** Yaw of the returning-player spawnpoint (degrees). */
    public static float SPAWNPOINT_YAW = 0.0f;

    /** X coordinate of the intro cutscene location. */
    public static double OPENING_X = -186.0;

    /** Y coordinate of the intro cutscene location. */
    public static double OPENING_Y = 256.0;

    /** Z coordinate of the intro cutscene location. */
    public static double OPENING_Z = -1058.0;

    /** Yaw of the opening cutscene location (degrees, controls NPC + camera facing). */
    public static float OPENING_YAW = 0.0f;

    /** Animation key used for the intro cutscene (must match an entry in animations.yml). */
    public static String OPENING_ANIMATION_KEY = "slash_test_default";

    /** Duration (ms) of the first animation phase before the lightning strike. */
    public static int PHASE1_DURATION_MS = 5000;

    /** Duration (ms) of the second animation phase after the lightning strike. */
    public static int PHASE2_DURATION_MS = 2000;

    /** Duration (ms) of the fake-player static camera phase. */
    public static int PHASE3_DURATION_MS = 5000;

    static {
        register("join_sequence.world", WORLD, String.class,
            v -> WORLD = v, ConfigurationSection::getString);
        register("join_sequence.spawnpoint_x", SPAWNPOINT_X, Double.class,
            v -> SPAWNPOINT_X = v, ConfigurationSection::getDouble);
        register("join_sequence.spawnpoint_y", SPAWNPOINT_Y, Double.class,
            v -> SPAWNPOINT_Y = v, ConfigurationSection::getDouble);
        register("join_sequence.spawnpoint_z", SPAWNPOINT_Z, Double.class,
            v -> SPAWNPOINT_Z = v, ConfigurationSection::getDouble);
        register("join_sequence.spawnpoint_yaw", (double) SPAWNPOINT_YAW, Double.class,
            v -> SPAWNPOINT_YAW = v.floatValue(), ConfigurationSection::getDouble);
        register("join_sequence.opening_x", OPENING_X, Double.class,
            v -> OPENING_X = v, ConfigurationSection::getDouble);
        register("join_sequence.opening_y", OPENING_Y, Double.class,
            v -> OPENING_Y = v, ConfigurationSection::getDouble);
        register("join_sequence.opening_z", OPENING_Z, Double.class,
            v -> OPENING_Z = v, ConfigurationSection::getDouble);
        register("join_sequence.opening_yaw", (double) OPENING_YAW, Double.class,
            v -> OPENING_YAW = v.floatValue(), ConfigurationSection::getDouble);
        register("join_sequence.opening_animation_key", OPENING_ANIMATION_KEY, String.class,
            v -> OPENING_ANIMATION_KEY = v, ConfigurationSection::getString);
        register("join_sequence.phase1_duration_ms", PHASE1_DURATION_MS, Integer.class,
            v -> PHASE1_DURATION_MS = v, ConfigurationSection::getInt);
        register("join_sequence.phase2_duration_ms", PHASE2_DURATION_MS, Integer.class,
            v -> PHASE2_DURATION_MS = v, ConfigurationSection::getInt);
        register("join_sequence.phase3_duration_ms", PHASE3_DURATION_MS, Integer.class,
            v -> PHASE3_DURATION_MS = v, ConfigurationSection::getInt);
    }


    public static Location HUB_SPAWN = new Location(Bukkit.getWorlds().getFirst(), -271.5, 138, -1242.5);
    static { register("join_sequence.hub_spawn", HUB_SPAWN, Location.class,
        v -> HUB_SPAWN = v, Config::loadLocation); }
}
