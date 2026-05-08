package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

import btm.sword.action.movement.MovementAction;
import btm.sword.action.throwing.types.ThrownItem;
import btm.sword.config.Config;

/**
 * Movement ability configuration for dash, toss, and grab mechanics.
 * <p>
 * Controls player mobility abilities including directional dash (teleport),
 * sword toss (projectile), and entity grab (pull). Distances in <b>blocks</b>,
 * time in <b>ticks</b>, velocities in <b>blocks/tick</b>.
 * </p>
 *
 * <h2>Key Abilities</h2>
 * <ul>
 *   <li><b>Dash</b> - Directional teleport with collision detection, particle trail, grab on contact</li>
 *   <li><b>Toss</b> - Throw sword in arc trajectory with explosion on impact</li>
 *   <li><b>Grab</b> - Pull nearby entities toward player</li>
 * </ul>
 *
 * @see MovementAction Movement ability implementation
 * @see ThrownItem Toss projectile physics
 */
public final class MovementConfig {

    private MovementConfig() {}

    // Dash configuration
    public static double DASH_MAX_DISTANCE = 12.0;
    static { register(
        "movement.dash_max_distance",
        DASH_MAX_DISTANCE, Double.class,
        v -> DASH_MAX_DISTANCE = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_FLAT_ITEM_DASH_UPWARD_SCALER = 0.65;
    static { register(
        "movement.dash_flat_item_dash_upward_scaler",
        DASH_FLAT_ITEM_DASH_UPWARD_SCALER, Double.class,
        v -> DASH_FLAT_ITEM_DASH_UPWARD_SCALER = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_FLAT_ITEM_DASH_DISTANCE_SCALER = 0.4;
    static { register(
        "movement.dash_flat_item_dash_distance_scaler",
        DASH_FLAT_ITEM_DASH_DISTANCE_SCALER, Double.class,
        v -> DASH_FLAT_ITEM_DASH_DISTANCE_SCALER = v,
        ConfigurationSection::getDouble
    ); }

    /** Upper height difference threshold for applying upward boost in flat item dash. */
    public static double DASH_FLAT_HEIGHT_UPPER = 2.0;
    static { register(
        "movement.dash_flat_height_upper",
        DASH_FLAT_HEIGHT_UPPER, Double.class,
        v -> DASH_FLAT_HEIGHT_UPPER = v,
        ConfigurationSection::getDouble
    ); }

    /** Lower height difference threshold for applying upward boost in flat item dash. */
    public static double DASH_FLAT_HEIGHT_LOWER = -3.0;
    static { register(
        "movement.dash_flat_height_lower",
        DASH_FLAT_HEIGHT_LOWER, Double.class,
        v -> DASH_FLAT_HEIGHT_LOWER = v,
        ConfigurationSection::getDouble
    ); }

    public static int SPEED_DURATION = 5;
    static { register(
        "movement.speed_duration",
        SPEED_DURATION, Integer.class,
        v -> SPEED_DURATION = v,
        ConfigurationSection::getInt
    ); }

    public static int SPEED_AMPLIFIER = 3;
    static { register(
        "movement.speed_amplifier",
        SPEED_AMPLIFIER, Integer.class,
        v -> SPEED_AMPLIFIER = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_CAST_DURATION = 5;
    static { register(
        "movement.dash_cast_duration",
        DASH_CAST_DURATION, Integer.class,
        v -> DASH_CAST_DURATION = v,
        ConfigurationSection::getInt
    ); }

    public static double DASH_BASE_POWER = 0.7;
    static { register(
        "movement.dash_base_power",
        DASH_BASE_POWER, Double.class,
        v -> DASH_BASE_POWER = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_INITIAL_OFFSET_Y = 0.3;
    static { register(
        "movement.dash_initial_offset_y",
        DASH_INITIAL_OFFSET_Y, Double.class,
        v -> DASH_INITIAL_OFFSET_Y = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_IMPEDANCE_CHECK_OFFSET_Y = 0.5;
    static { register(
        "movement.dash_impedance_check_offset_y",
        DASH_IMPEDANCE_CHECK_OFFSET_Y, Double.class,
        v -> DASH_IMPEDANCE_CHECK_OFFSET_Y = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_FORWARD_MULTIPLIER = 0.5;
    static { register(
        "movement.dash_forward_multiplier",
        DASH_FORWARD_MULTIPLIER, Double.class,
        v -> DASH_FORWARD_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_UPWARD_MULTIPLIER = 0.15;
    static { register(
        "movement.dash_upward_multiplier",
        DASH_UPWARD_MULTIPLIER, Double.class,
        v -> DASH_UPWARD_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_UPWARD_BOOST = 0.05;
    static { register(
        "movement.dash_upward_boost",
        DASH_UPWARD_BOOST, Double.class,
        v -> DASH_UPWARD_BOOST = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_RAY_HITBOX_RADIUS = 0.7;
    static { register(
        "movement.dash_ray_hitbox_radius",
        DASH_RAY_HITBOX_RADIUS, Double.class,
        v -> DASH_RAY_HITBOX_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_SECANT_RADIUS = 0.3;
    static { register(
        "movement.dash_secant_radius",
        DASH_SECANT_RADIUS, Double.class,
        v -> DASH_SECANT_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_GRAB_DISTANCE_SQUARED = 4.0;
    static { register(
        "movement.dash_grab_distance_squared",
        DASH_GRAB_DISTANCE_SQUARED, Double.class,
        v -> DASH_GRAB_DISTANCE_SQUARED = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_BLOCK_CHECK_OFFSET_Y = -0.75;
    static { register(
        "movement.dash_block_check_offset_y",
        DASH_BLOCK_CHECK_OFFSET_Y, Double.class,
        v -> DASH_BLOCK_CHECK_OFFSET_Y = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_VELOCITY_DAMPING = 0.6; // multiplier (0-1)
    static { register(
        "movement.dash_velocity_damping",
        DASH_VELOCITY_DAMPING, Double.class,
        v -> DASH_VELOCITY_DAMPING = v,
        ConfigurationSection::getDouble
    ); }

    /** Downward flat check multiplier for dash mechanics. */
    public static double DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER = 3.0;
    static { register(
        "movement.dash_downward_flat_check_multiplier",
        DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER, Double.class,
        v -> DASH_DOWNWARD_FLAT_CHECK_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static long DASH_PARTICLE_TASK_DELAY = 0L;
    static { register(
        "movement.dash_particle_task_delay",
        DASH_PARTICLE_TASK_DELAY, Long.class,
        v -> DASH_PARTICLE_TASK_DELAY = v,
        ConfigurationSection::getLong
    ); }

    public static int DASH_PARTICLE_TASK_PERIOD = 2;
    static { register(
        "movement.dash_particle_task_period",
        DASH_PARTICLE_TASK_PERIOD, Integer.class,
        v -> DASH_PARTICLE_TASK_PERIOD = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_PARTICLE_TIMER_INCREMENT = 2;
    static { register(
        "movement.dash_particle_timer_increment",
        DASH_PARTICLE_TIMER_INCREMENT, Integer.class,
        v -> DASH_PARTICLE_TIMER_INCREMENT = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_PARTICLE_TIMER_THRESHOLD = 4;
    static { register(
        "movement.dash_particle_timer_threshold",
        DASH_PARTICLE_TIMER_THRESHOLD, Integer.class,
        v -> DASH_PARTICLE_TIMER_THRESHOLD = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_GRAB_CHECK_DELAY = 200;
    static { register(
        "movement.dash_grab_check_delay",
        DASH_GRAB_CHECK_DELAY, Integer.class,
        v -> DASH_GRAB_CHECK_DELAY = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_VELOCITY_TASK_DELAY = 0;
    static { register(
        "movement.dash_velocity_task_delay",
        DASH_VELOCITY_TASK_DELAY, Integer.class,
        v -> DASH_VELOCITY_TASK_DELAY = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_VELOCITY_TASK_PERIOD = 50;
    static { register(
        "movement.dash_velocity_task_period",
        DASH_VELOCITY_TASK_PERIOD, Integer.class,
        v -> DASH_VELOCITY_TASK_PERIOD = v,
        ConfigurationSection::getInt
    ); }

    public static int DASH_PARTICLE_COUNT = 100;
    static { register(
        "movement.dash_particle_count",
        DASH_PARTICLE_COUNT, Integer.class,
        v -> DASH_PARTICLE_COUNT = v,
        ConfigurationSection::getInt
    ); }

    public static double DASH_PARTICLE_SPREAD_X = 1.25;
    static { register(
        "movement.dash_particle_spread_x",
        DASH_PARTICLE_SPREAD_X, Double.class,
        v -> DASH_PARTICLE_SPREAD_X = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_PARTICLE_SPREAD_Y = 1.25;
    static { register(
        "movement.dash_particle_spread_y",
        DASH_PARTICLE_SPREAD_Y, Double.class,
        v -> DASH_PARTICLE_SPREAD_Y = v,
        ConfigurationSection::getDouble
    ); }

    public static double DASH_PARTICLE_SPREAD_Z = 1.25;
    static { register(
        "movement.dash_particle_spread_z",
        DASH_PARTICLE_SPREAD_Z, Double.class,
        v -> DASH_PARTICLE_SPREAD_Z = v,
        ConfigurationSection::getDouble
    ); }

    public static float DASH_FLAP_SOUND_VOLUME = 0.6f; // 0.0-1.0
    static { register(
        "movement.dash_flap_sound_volume",
        DASH_FLAP_SOUND_VOLUME, Float.class,
        v -> DASH_FLAP_SOUND_VOLUME = v,
        Config::loadFloat
    ); }

    public static float DASH_FLAP_SOUND_PITCH = 1.0f; // 0.5-2.0
    static { register(
        "movement.dash_flap_sound_pitch",
        DASH_FLAP_SOUND_PITCH, Float.class,
        v -> DASH_FLAP_SOUND_PITCH = v,
        Config::loadFloat
    ); }

    public static float DASH_SWEEP_SOUND_VOLUME = 0.3f; // 0.0-1.0
    static { register(
        "movement.dash_sweep_sound_volume",
        DASH_SWEEP_SOUND_VOLUME, Float.class,
        v -> DASH_SWEEP_SOUND_VOLUME = v,
        Config::loadFloat
    ); }

    public static float DASH_SWEEP_SOUND_PITCH = 0.6f; // 0.5-2.0
    static { register(
        "movement.dash_sweep_sound_pitch",
        DASH_SWEEP_SOUND_PITCH, Float.class,
        v -> DASH_SWEEP_SOUND_PITCH = v,
        Config::loadFloat
    ); }

    // Toss configuration
    public static double TOSS_BASE_FORCE = 1.5; // blocks/tick
    static { register(
        "movement.toss_base_force",
        TOSS_BASE_FORCE, Double.class,
        v -> TOSS_BASE_FORCE = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_MIGHT_MULTIPLIER_BASE = 2.5; // multiplier
    static { register(
        "movement.toss_might_multiplier_base",
        TOSS_MIGHT_MULTIPLIER_BASE, Double.class,
        v -> TOSS_MIGHT_MULTIPLIER_BASE = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_MIGHT_MULTIPLIER_INCREMENT = 0.1; // per level
    static { register(
        "movement.toss_might_multiplier_increment",
        TOSS_MIGHT_MULTIPLIER_INCREMENT, Double.class,
        v -> TOSS_MIGHT_MULTIPLIER_INCREMENT = v,
        ConfigurationSection::getDouble
    ); }

    public static int TOSS_UPWARD_PHASE_ITERATIONS = 2;
    static { register(
        "movement.toss_upward_phase_iterations",
        TOSS_UPWARD_PHASE_ITERATIONS, Integer.class,
        v -> TOSS_UPWARD_PHASE_ITERATIONS = v,
        ConfigurationSection::getInt
    ); }

    public static double TOSS_UPWARD_VELOCITY_Y = 0.25; // blocks/tick
    static { register(
        "movement.toss_upward_velocity_y",
        TOSS_UPWARD_VELOCITY_Y, Double.class,
        v -> TOSS_UPWARD_VELOCITY_Y = v,
        ConfigurationSection::getDouble
    ); }

    public static int TOSS_FORWARD_PHASE_ITERATIONS = 3;
    static { register(
        "movement.toss_forward_phase_iterations",
        TOSS_FORWARD_PHASE_ITERATIONS, Integer.class,
        v -> TOSS_FORWARD_PHASE_ITERATIONS = v,
        ConfigurationSection::getInt
    ); }

    public static int TOSS_ANIMATION_ITERATIONS = 15;
    static { register(
        "movement.toss_animation_iterations",
        TOSS_ANIMATION_ITERATIONS, Integer.class,
        v -> TOSS_ANIMATION_ITERATIONS = v,
        ConfigurationSection::getInt
    ); }

    public static double TOSS_LOCATION_OFFSET_MULTIPLIER = 0.3;
    static { register(
        "movement.toss_location_offset_multiplier",
        TOSS_LOCATION_OFFSET_MULTIPLIER, Double.class,
        v -> TOSS_LOCATION_OFFSET_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_PARTICLE_HEIGHT_MULTIPLIER = 0.5;
    static { register(
        "movement.toss_particle_height_multiplier",
        TOSS_PARTICLE_HEIGHT_MULTIPLIER, Double.class,
        v -> TOSS_PARTICLE_HEIGHT_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_RAY_TRACE_DISTANCE_MULTIPLIER = 0.6;
    static { register(
        "movement.toss_ray_trace_distance_multiplier",
        TOSS_RAY_TRACE_DISTANCE_MULTIPLIER, Double.class,
        v -> TOSS_RAY_TRACE_DISTANCE_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_ENTITY_DETECTION_RADIUS = 0.4;
    static { register(
        "movement.toss_entity_detection_radius",
        TOSS_ENTITY_DETECTION_RADIUS, Double.class,
        v -> TOSS_ENTITY_DETECTION_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    public static double TOSS_KNOCKBACK_MULTIPLIER = 0.3;
    static { register(
        "movement.toss_knockback_multiplier",
        TOSS_KNOCKBACK_MULTIPLIER, Double.class,
        v -> TOSS_KNOCKBACK_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    public static float TOSS_EXPLOSION_POWER = 2.0f;
    static { register(
        "movement.toss_explosion_power",
        TOSS_EXPLOSION_POWER, Float.class,
        v -> TOSS_EXPLOSION_POWER = v,
        Config::loadFloat
    ); }

    public static int TOSS_HIT_INVULNERABILITY_TICKS = 3;
    static { register(
        "movement.toss_hit_invulnerability_ticks",
        TOSS_HIT_INVULNERABILITY_TICKS, Integer.class,
        v -> TOSS_HIT_INVULNERABILITY_TICKS = v,
        ConfigurationSection::getInt
    ); }

    public static int TOSS_HIT_SHARD_DAMAGE = 2;
    static { register(
        "movement.toss_hit_shard_damage",
        TOSS_HIT_SHARD_DAMAGE, Integer.class,
        v -> TOSS_HIT_SHARD_DAMAGE = v,
        ConfigurationSection::getInt); }

    public static float TOSS_HIT_TOUGHNESS_DAMAGE = 30.0f;
    static { register(
        "movement.toss_hit_toughness_damage",
        TOSS_HIT_TOUGHNESS_DAMAGE, Float.class,
        v -> TOSS_HIT_TOUGHNESS_DAMAGE = v,
        Config::loadFloat); }

    public static float TOSS_HIT_SOULFIRE_REDUCTION = 5.0f;
    static { register(
        "movement.toss_hit_soulfire_reduction",
        TOSS_HIT_SOULFIRE_REDUCTION, Float.class,
        v -> TOSS_HIT_SOULFIRE_REDUCTION = v,
        Config::loadFloat); }

    // Grab configuration
    public static double GRAB_PULL_STRENGTH = 0.8; // blocks/tick
    static { register(
        "movement.grab_pull_strength",
        GRAB_PULL_STRENGTH, Double.class,
        v -> GRAB_PULL_STRENGTH = v,
        ConfigurationSection::getDouble); }

    public static double GRAB_MAX_RANGE = 3.0;
    static { register(
        "movement.grab_max_range",
        GRAB_MAX_RANGE, Double.class,
        v -> GRAB_MAX_RANGE = v,
        ConfigurationSection::getDouble); }

    public static int GRAB_HOLD_DURATION = 40;
    static { register(
        "movement.grab_hold_duration",
        GRAB_HOLD_DURATION, Integer.class,
        v -> GRAB_HOLD_DURATION = v,
        ConfigurationSection::getInt); }

    /** Umbral dash ray width for targeting. */
    public static double DASH_UMBRAL_RAY_HITBOX_RADIUS = 1.2;
    static { register(
        "movement.dash_umbral_ray_hitbox_radius",
        DASH_UMBRAL_RAY_HITBOX_RADIUS, Double.class,
        v -> DASH_UMBRAL_RAY_HITBOX_RADIUS = v,
        ConfigurationSection::getDouble
    ); }

    /** Pitch threshold for flat dash detection. */
    public static double FLAT_DASH_PITCH_THRESHOLD = 0.30;
    static { register(
        "movement.flat_dash_pitch_threshold",
        FLAT_DASH_PITCH_THRESHOLD, Double.class,
        v -> FLAT_DASH_PITCH_THRESHOLD = v,
        ConfigurationSection::getDouble
    ); }

    /** Power multiplier for flat dash. */
    public static double FLAT_DASH_POWER_MULTIPLIER = 2.0;
    static { register(
        "movement.flat_dash_power_multiplier",
        FLAT_DASH_POWER_MULTIPLIER, Double.class,
        v -> FLAT_DASH_POWER_MULTIPLIER = v,
        ConfigurationSection::getDouble
    ); }

    /** Distance divisor for normal dash to item. */
    public static double DASH_NORMAL_ITEM_DISTANCE_DIVISOR = 2.0;
    static { register(
        "movement.dash_normal_item_distance_divisor",
        DASH_NORMAL_ITEM_DISTANCE_DIVISOR, Double.class,
        v -> DASH_NORMAL_ITEM_DISTANCE_DIVISOR = v,
        ConfigurationSection::getDouble
    ); }

    /** Delay for flat dash height boost in milliseconds. */
    public static int FLAT_DASH_HEIGHT_BOOST_DELAY_MS = 75;
    static { register(
        "movement.flat_dash_height_boost_delay_ms",
        FLAT_DASH_HEIGHT_BOOST_DELAY_MS, Integer.class,
        v -> FLAT_DASH_HEIGHT_BOOST_DELAY_MS = v,
        ConfigurationSection::getInt
    ); }

    /** Period for dash item check in milliseconds. */
    public static int DASH_ITEM_CHECK_PERIOD_MS = 50;
    static { register(
        "movement.dash_item_check_period_ms",
        DASH_ITEM_CHECK_PERIOD_MS, Integer.class,
        v -> DASH_ITEM_CHECK_PERIOD_MS = v,
        ConfigurationSection::getInt
    ); }

    /** Duration for dash item check in milliseconds. */
    public static int DASH_ITEM_CHECK_DURATION_MS = 1500;
    static { register(
        "movement.dash_item_check_duration_ms",
        DASH_ITEM_CHECK_DURATION_MS, Integer.class,
        v -> DASH_ITEM_CHECK_DURATION_MS = v,
        ConfigurationSection::getInt
    ); }
}
