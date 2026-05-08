package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Detection and collision configuration for raytracing and entity searches.
 * <p>
 * Controls ground checks, raytrace parameters, and entity detection radius.
 * All distance values in <b>blocks</b>. Used for collision detection, target
 * acquisition, and environmental queries.
 * </p>
 *
 * <h2>Detection Methods</h2>
 * <ul>
 *   <li><b>Ground Check</b> - Determines if entity is grounded (affects movement)</li>
 *   <li><b>Raytrace</b> - Line-of-sight checks for blocks and entities</li>
 *   <li><b>Entity Detection</b> - Radius-based entity searches</li>
 * </ul>
 */
public final class DetectionConfig {

    private DetectionConfig() {}

    // Ground check configuration
    public static double GROUND_CHECK_MAX_DISTANCE = 0.3;
    static { register(
        "detection.ground_check_max_distance",
        GROUND_CHECK_MAX_DISTANCE, Double.class,
        v -> GROUND_CHECK_MAX_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    // Raytrace configuration
    public static double RAYTRACE_MAX_DISTANCE = 50.0;
    static { register(
        "detection.raytrace_max_distance",
        RAYTRACE_MAX_DISTANCE, Double.class,
        v -> RAYTRACE_MAX_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    public static double RAYTRACE_STEP_SIZE = 0.1;
    static { register(
        "detection.raytrace_step_size",
        RAYTRACE_STEP_SIZE, Double.class,
        v -> RAYTRACE_STEP_SIZE = v,
        ConfigurationSection::getDouble
    ); }

    public static boolean RAYTRACE_IGNORE_PASSABLE_BLOCKS = true;
    static { register(
        "detection.raytrace_ignore_passable_blocks",
        RAYTRACE_IGNORE_PASSABLE_BLOCKS, Boolean.class,
        v -> RAYTRACE_IGNORE_PASSABLE_BLOCKS = v,
        ConfigurationSection::getBoolean
    ); }

    public static double THROW_PIN_RAY_DISTANCE = 1.0;
    static { register(
        "detection.throw_pin_ray_distance",
        THROW_PIN_RAY_DISTANCE, Double.class,
        v -> THROW_PIN_RAY_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROW_GROUND_CHECK_MULTIPLIER = 0.1;
    static { register(
        "detection.throw_ground_check_multiplier",
        THROW_GROUND_CHECK_MULTIPLIER, Double.class,
        v -> THROW_GROUND_CHECK_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROW_HIT_CHECK_DIST_MULTIPLIER = 0.6;
    static { register(
        "detection.throw_hit_check_dist_multiplier",
        THROW_HIT_CHECK_DIST_MULTIPLIER, Double.class,
        v -> THROW_HIT_CHECK_DIST_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROW_HIT_CHECK_RAY_SIZE = 1.0;
    static { register(
        "detection.throw_hit_check_ray_size",
        THROW_HIT_CHECK_RAY_SIZE, Double.class,
        v -> THROW_HIT_CHECK_RAY_SIZE = v,
        ConfigurationSection::getDouble
    ); }


    // Entity detection configuration
    public static double ENTITY_DETECTION_SEARCH_RADIUS = 10.0;
    static { register(
        "detection.entity_detection_search_radius",
        ENTITY_DETECTION_SEARCH_RADIUS, Double.class,
        v -> ENTITY_DETECTION_SEARCH_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    public static boolean ENTITY_DETECTION_INCLUDE_SPECTATORS = false;
    static { register(
        "detection.entity_detection_include_spectators",
        ENTITY_DETECTION_INCLUDE_SPECTATORS, Boolean.class,
        v -> ENTITY_DETECTION_INCLUDE_SPECTATORS = v,
        ConfigurationSection::getBoolean
    ); }
}
