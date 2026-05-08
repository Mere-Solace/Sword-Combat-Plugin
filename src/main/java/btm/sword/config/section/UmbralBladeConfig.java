package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * All configurable parameters for the UmbralBlade weapon and its state machine:
 * lunge timings, throw/recall physics, lodging thresholds, and blade display properties.
 * Used by {@link btm.sword.umbral.UmbralBlade} and its states.
 */
public final class UmbralBladeConfig {

    private UmbralBladeConfig() {}

    public static double LUNGE_TIME_CUTOFF = 1.1;
    static { register(
        "umbral.time_cutoff",
        LUNGE_TIME_CUTOFF, Double.class,
        v -> LUNGE_TIME_CUTOFF = v,
        ConfigurationSection::getDouble
    ); }

    public static int LUNGE_TIME_SCALING_FACTOR = 9;
    static { register(
        "umbral.time_scaling_factor",
        LUNGE_TIME_SCALING_FACTOR, Integer.class,
        v -> LUNGE_TIME_SCALING_FACTOR = v,
        ConfigurationSection::getInt
    ); }

    public static int LUNGE_ON_RELEASE_VELOCITY = 3;
    static { register(
        "umbral.on_release",
        LUNGE_ON_RELEASE_VELOCITY, Integer.class,
        v -> LUNGE_ON_RELEASE_VELOCITY = v,
        ConfigurationSection::getInt
    ); }

    /** Timeout in milliseconds before Waiting state transitions. */
    public static long WAITING_TIMEOUT_MS = 500000; // 25000 seconds
    static { register(
        "umbral.waiting_timeout_ms",
        WAITING_TIMEOUT_MS, Long.class,
        v -> WAITING_TIMEOUT_MS = v,
        ConfigurationSection::getLong
    ); }

    /** Maximum distance the blade can be from player while in Waiting state. */
    public static double WAITING_MAX_DISTANCE = 35.0;
    static { register(
        "umbral.waiting_max_distance",
        WAITING_MAX_DISTANCE, Double.class,
        v -> WAITING_MAX_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    /** Initial delay in milliseconds before the wield-on-grab timer starts. */
    public static int WIELD_ON_GRAB_DELAY = 0;
    static { register(
        "umbral.wield_on_grab_delay",
        WIELD_ON_GRAB_DELAY, Integer.class,
        v -> WIELD_ON_GRAB_DELAY = v,
        ConfigurationSection::getInt
    ); }

    /** Period in milliseconds between each wield request during grab. */
    public static int WIELD_ON_GRAB_PERIOD = 50;
    static { register(
        "umbral.wield_on_grab_period",
        WIELD_ON_GRAB_PERIOD, Integer.class,
        v -> WIELD_ON_GRAB_PERIOD = v,
        ConfigurationSection::getInt
    ); }

    /** Maximum number of wield request iterations during grab. */
    public static int WIELD_ON_GRAB_ITERATIONS = 10;
    static { register(
        "umbral.wield_on_grab_iterations",
        WIELD_ON_GRAB_ITERATIONS, Integer.class,
        v -> WIELD_ON_GRAB_ITERATIONS = v,
        ConfigurationSection::getInt
    ); }

    /** Duration in milliseconds during which a reclaim attack can be triggered after catching the blade. */
    public static int RECLAIM_WINDOW_MS = 1000;
    static { register(
        "umbral-blade.reclaim-window-ms",
        RECLAIM_WINDOW_MS, Integer.class,
        v -> RECLAIM_WINDOW_MS = v,
        ConfigurationSection::getInt
    ); }

    /** Period in milliseconds of the idle hover oscillation. */
    public static int IDLE_MOVEMENT_PERIOD = 150;
    static { register(
        "umbral-blade.idle-movement-period",
        IDLE_MOVEMENT_PERIOD, Integer.class,
        v -> IDLE_MOVEMENT_PERIOD = v,
        ConfigurationSection::getInt
    ); }

    /** Amplitude of the idle hover oscillation. */
    public static double IDLE_MOVEMENT_AMPLITUDE = 0.25;
    static { register(
        "umbral-blade.idle-movement-amplitude",
        IDLE_MOVEMENT_AMPLITUDE, Double.class,
        v -> IDLE_MOVEMENT_AMPLITUDE = v,
        ConfigurationSection::getDouble
    ); }

    /** X scale of the blade ItemDisplay entity. */
    public static double SCALE_X = 0.85;
    static { register(
        "umbral-blade.scale-x",
        SCALE_X, Double.class,
        v -> SCALE_X = v,
        ConfigurationSection::getDouble
    ); }

    /** Y scale of the blade ItemDisplay entity. */
    public static double SCALE_Y = 1.3;
    static { register(
        "umbral-blade.scale-y",
        SCALE_Y, Double.class,
        v -> SCALE_Y = v,
        ConfigurationSection::getDouble
    ); }

    /** Z scale of the blade ItemDisplay entity. */
    public static double SCALE_Z = 1.0;
    static { register(
        "umbral-blade.scale-z",
        SCALE_Z, Double.class,
        v -> SCALE_Z = v,
        ConfigurationSection::getDouble
    ); }

    /** Radius of the ArcShape for BLADE_RETRIEVAL_CIRCULAR_SLASH. */
    public static double CIRCULAR_SLASH_RADIUS = 3.5;
    static { register(
        "umbral-blade.circular-slash-radius",
        CIRCULAR_SLASH_RADIUS, Double.class,
        v -> CIRCULAR_SLASH_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    /** Start angle in radians for the circular slash arc. */
    public static double CIRCULAR_SLASH_START_ANGLE = 0.157; // Math.PI/20
    static { register(
        "umbral-blade.circular-slash-start-angle",
        CIRCULAR_SLASH_START_ANGLE, Double.class,
        v -> CIRCULAR_SLASH_START_ANGLE = v,
        ConfigurationSection::getDouble
    ); }

    /** End angle in radians for the circular slash arc (full 360° sweep). */
    public static double CIRCULAR_SLASH_END_ANGLE = 6.283; // 2*Math.PI
    static { register(
        "umbral-blade.circular-slash-end-angle",
        CIRCULAR_SLASH_END_ANGLE, Double.class,
        v -> CIRCULAR_SLASH_END_ANGLE = v,
        ConfigurationSection::getDouble
    ); }

    /** Vertical offset of the circular slash arc from the attack origin. */
    public static double CIRCULAR_SLASH_HEIGHT = 0.0;
    static { register(
        "umbral-blade.circular-slash-height",
        CIRCULAR_SLASH_HEIGHT, Double.class,
        v -> CIRCULAR_SLASH_HEIGHT = v,
        ConfigurationSection::getDouble
    ); }

    /** Knockback multiplier applied in the BLADE_RETRIEVAL_CIRCULAR_SLASH knockback function. */
    public static double CIRCULAR_SLASH_KNOCKBACK = 0.5;
    static { register(
        "umbral-blade.circular-slash-knockback",
        CIRCULAR_SLASH_KNOCKBACK, Double.class,
        v -> CIRCULAR_SLASH_KNOCKBACK = v,
        ConfigurationSection::getDouble
    ); }
}
