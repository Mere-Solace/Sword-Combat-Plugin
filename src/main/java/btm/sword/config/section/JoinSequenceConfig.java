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


    /** Destination for the {@code Hub} button — the central social area. */
    public static Location DESTINATION_HUB =
        new Location(Bukkit.getWorlds().getFirst(), -271.5, 138, -1242.5);

    /** Destination for the {@code Quick-Join} button — the matchmaking waiting room. */
    public static Location DESTINATION_QUICK_JOIN =
        new Location(Bukkit.getWorlds().getFirst(), 0.5, 100, 0.5);

    /** Destination for the {@code Adventure} button — the adventure-mode world entry point. */
    public static Location DESTINATION_ADVENTURE =
        new Location(Bukkit.getWorlds().getFirst(), 100.5, 100, 100.5);

    /** Destination for the {@code Roguelike} button — the roguelike-mode world entry point. */
    public static Location DESTINATION_ROGUELIKE =
        new Location(Bukkit.getWorlds().getFirst(), -100.5, 100, -100.5);

    /**
     * Total duration (ms) of the routing countdown after the player picks a destination.
     * The player is held in place with the title-countdown overlay for this long before
     * they are teleported and handed their loadout.
     */
    public static int ROUTING_DURATION_MS = 5000;

    /**
     * Tick period (ms) for the routing countdown. The countdown title is refreshed once per
     * period, and the total iteration count is {@code ROUTING_DURATION_MS / ROUTING_TICK_PERIOD_MS}.
     */
    public static int ROUTING_TICK_PERIOD_MS = 1000;

    static {
        register("join_sequence.destination.hub", DESTINATION_HUB, Location.class,
            v -> DESTINATION_HUB = v, Config::loadLocation);
        register("join_sequence.destination.quick_join", DESTINATION_QUICK_JOIN, Location.class,
            v -> DESTINATION_QUICK_JOIN = v, Config::loadLocation);
        register("join_sequence.destination.adventure", DESTINATION_ADVENTURE, Location.class,
            v -> DESTINATION_ADVENTURE = v, Config::loadLocation);
        register("join_sequence.destination.roguelike", DESTINATION_ROGUELIKE, Location.class,
            v -> DESTINATION_ROGUELIKE = v, Config::loadLocation);
        register("join_sequence.routing.duration_ms", ROUTING_DURATION_MS, Integer.class,
            v -> ROUTING_DURATION_MS = v, ConfigurationSection::getInt);
        register("join_sequence.routing.tick_period_ms", ROUTING_TICK_PERIOD_MS, Integer.class,
            v -> ROUTING_TICK_PERIOD_MS = v, ConfigurationSection::getInt);
    }
}
