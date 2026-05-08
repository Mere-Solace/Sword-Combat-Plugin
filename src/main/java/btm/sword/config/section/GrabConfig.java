package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * All configurable parameters for the grab action: durations, range, hold physics,
 * pull speed, and aspect-scaling factors. Used by
 * {@link btm.sword.action.utility.GrabAction}.
 */
public final class GrabConfig {

    private GrabConfig() {}

    public static int CAST_DURATION = 750;
    static { register(
        "grab.cast_duration",
        CAST_DURATION, Integer.class,
        v -> CAST_DURATION = v,
        ConfigurationSection::getInt
    ); }

    // Base grab duration (ticks)
    public static int BASE_DURATION = 2000;
    static { register(
        "grab.base_duration",
        BASE_DURATION, Integer.class,
        v -> BASE_DURATION = v,
        ConfigurationSection::getInt
    ); }

    // Base grab raycast range
    public static double BASE_RANGE = 3.0;
    static { register(
        "grab.base_range",
        BASE_RANGE, Double.class,
        v -> BASE_RANGE = v,
        ConfigurationSection::getDouble
    ); }

    // Base grab raycast thickness
    public static double BASE_THICKNESS = 0.6;
    static { register(
        "grab.base_thickness",
        BASE_THICKNESS, Double.class,
        v -> BASE_THICKNESS = v,
        ConfigurationSection::getDouble
    ); }

    // Might → duration scaling
    public static double DURATION_SCALING = 0.2;
    static { register(
        "grab.duration_scaling",
        DURATION_SCALING, Double.class,
        v -> DURATION_SCALING = v,
        ConfigurationSection::getDouble
    ); }

    // Willpower → range scaling
    public static double RANGE_SCALING = 0.1;
    static { register(
        "grab.range_scaling",
        RANGE_SCALING, Double.class,
        v -> RANGE_SCALING = v,
        ConfigurationSection::getDouble
    ); }

    // Willpower → thickness scaling
    public static double THICKNESS_SCALING = 0.1;
    static { register(
        "grab.thickness_scaling",
        THICKNESS_SCALING, Double.class,
        v -> THICKNESS_SCALING = v,
        ConfigurationSection::getDouble
    ); }

    // Distance to hold the target while grabbing
    public static double HOLD_DISTANCE = 2.0;
    static { register(
        "grab.hold_distance",
        HOLD_DISTANCE, Double.class,
        v -> HOLD_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    // Minimum squared distance before we stop pulling
    public static double HOLD_BUFFER = 0.4;
    static { register(
        "grab.hold_buffer",
        HOLD_BUFFER, Double.class,
        v -> HOLD_BUFFER = v,
        ConfigurationSection::getDouble
    ); }

    // Pulling force
    public static double PULL_SPEED = 0.6;
    static { register(
        "grab.pull_speed",
        PULL_SPEED, Double.class,
        v -> PULL_SPEED = v,
        ConfigurationSection::getDouble
    ); }

    // Vertical distance threshold that doubles pull speed
    public static double VERTICAL_FORCE_THRESHOLD = 1.2;
    static { register(
        "grab.vertical_force_threshold",
        VERTICAL_FORCE_THRESHOLD, Double.class,
        v -> VERTICAL_FORCE_THRESHOLD = v,
        ConfigurationSection::getDouble
    ); }

    // Scaling applied when close enough (reduces velocity)
    public static double CLOSE_Y_VELOCITY_SCALE = 0.25;
    static { register(
        "grab.close_y_velocity_scale",
        CLOSE_Y_VELOCITY_SCALE, Double.class,
        v -> CLOSE_Y_VELOCITY_SCALE = v,
        ConfigurationSection::getDouble
    ); }

    // Jump boost effect applied while grabbing
    public static int JUMP_BOOST_AMPLIFIER = 1;
    static { register(
        "grab.jump_boost_amplifier",
        JUMP_BOOST_AMPLIFIER, Integer.class,
        v -> JUMP_BOOST_AMPLIFIER = v,
        ConfigurationSection::getInt
    ); }

    public static int JUMP_BOOST_DURATION = 2; // ticks, correct
    static { register(
        "grab.jump_boost_duration",
        JUMP_BOOST_DURATION, Integer.class,
        v -> JUMP_BOOST_DURATION = v,
        ConfigurationSection::getInt
    ); }

    // Executor velocity dampening while grabbing
    public static double EXECUTOR_HORIZONTAL_DAMPENING = 0.2;
    static { register(
        "grab.executor_horizontal_dampening",
        EXECUTOR_HORIZONTAL_DAMPENING, Double.class,
        v -> EXECUTOR_HORIZONTAL_DAMPENING = v,
        ConfigurationSection::getDouble
    ); }
}
