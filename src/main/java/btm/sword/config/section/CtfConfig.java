package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for the Capture the Flag gamemode.
 * <p>
 * Controls team spawn locations, flag return timer, respawn delay, win threshold, and
 * the speed debuff applied to flag carriers.
 * All values are hot-reloadable via {@code /sword reload}.
 * </p>
 */
public final class CtfConfig {

    private CtfConfig() {}

    /** World name shared by both team spawns. */
    public static String SPAWN_WORLD = "world";

    /** X coordinate of the RED team spawn (also the RED flag spawn). */
    public static double RED_SPAWN_X = 15.0;
    /** Y coordinate of the RED team spawn. */
    public static double RED_SPAWN_Y = 64.0;
    /** Z coordinate of the RED team spawn. */
    public static double RED_SPAWN_Z = 0.0;

    /** X coordinate of the BLUE team spawn (also the BLUE flag spawn). */
    public static double BLUE_SPAWN_X = -15.0;
    /** Y coordinate of the BLUE team spawn. */
    public static double BLUE_SPAWN_Y = 64.0;
    /** Z coordinate of the BLUE team spawn. */
    public static double BLUE_SPAWN_Z = 0.0;

    /** Seconds before a dropped flag automatically returns to its base. */
    public static int FLAG_RETURN_TIMER_SECONDS = 30;

    /** Seconds before a dead player respawns at their team spawn. */
    public static int RESPAWN_DELAY_SECONDS = 5;

    /**
     * Number of flag captures required to win.
     * Set to 0 for a timer-only match (most captures at expiry wins).
     */
    public static int CAPTURES_TO_WIN = 3;

    /** Radius in blocks within which a flag carrier scores a capture when near their own base. */
    public static double CAPTURE_RADIUS = 3.0;

    /** Duration in ticks of the Slowness effect applied to flag carriers. Use a large value for a persistent effect. */
    public static int FLAG_CARRIER_SLOW_DURATION = 999999;

    /** Amplifier (0-based) of the Slowness effect on flag carriers. 0 = Slowness I, 1 = Slowness II. */
    public static int FLAG_CARRIER_SLOW_AMPLIFIER = 1;

    static {
        register("ctf.spawn_world", SPAWN_WORLD, String.class,
            v -> SPAWN_WORLD = v, ConfigurationSection::getString);
        register("ctf.red_spawn_x", RED_SPAWN_X, Double.class,
            v -> RED_SPAWN_X = v, ConfigurationSection::getDouble);
        register("ctf.red_spawn_y", RED_SPAWN_Y, Double.class,
            v -> RED_SPAWN_Y = v, ConfigurationSection::getDouble);
        register("ctf.red_spawn_z", RED_SPAWN_Z, Double.class,
            v -> RED_SPAWN_Z = v, ConfigurationSection::getDouble);
        register("ctf.blue_spawn_x", BLUE_SPAWN_X, Double.class,
            v -> BLUE_SPAWN_X = v, ConfigurationSection::getDouble);
        register("ctf.blue_spawn_y", BLUE_SPAWN_Y, Double.class,
            v -> BLUE_SPAWN_Y = v, ConfigurationSection::getDouble);
        register("ctf.blue_spawn_z", BLUE_SPAWN_Z, Double.class,
            v -> BLUE_SPAWN_Z = v, ConfigurationSection::getDouble);
        register("ctf.flag_return_timer_seconds", FLAG_RETURN_TIMER_SECONDS, Integer.class,
            v -> FLAG_RETURN_TIMER_SECONDS = v, ConfigurationSection::getInt);
        register("ctf.respawn_delay_seconds", RESPAWN_DELAY_SECONDS, Integer.class,
            v -> RESPAWN_DELAY_SECONDS = v, ConfigurationSection::getInt);
        register("ctf.captures_to_win", CAPTURES_TO_WIN, Integer.class,
            v -> CAPTURES_TO_WIN = v, ConfigurationSection::getInt);
        register("ctf.capture_radius", CAPTURE_RADIUS, Double.class,
            v -> CAPTURE_RADIUS = v, ConfigurationSection::getDouble);
        register("ctf.flag_carrier_slow_duration", FLAG_CARRIER_SLOW_DURATION, Integer.class,
            v -> FLAG_CARRIER_SLOW_DURATION = v, ConfigurationSection::getInt);
        register("ctf.flag_carrier_slow_amplifier", FLAG_CARRIER_SLOW_AMPLIFIER, Integer.class,
            v -> FLAG_CARRIER_SLOW_AMPLIFIER = v, ConfigurationSection::getInt);
    }
}
