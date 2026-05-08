package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for Hostile entity AI behavior.
 * <p>
 * Distance thresholds are stored as squared values at load time so all in-tick
 * comparisons use {@code distanceSquared()} — no {@code sqrt} is required during gameplay.
 * </p>
 */
public final class HostileConfig {

    private HostileConfig() {}

    /** Aggro range squared (loaded from raw radius and squared on assignment). */
    public static double AGGRO_RANGE_SQUARED = 256.0;
    static { register(
        "hostile.aggro_range",
        16.0, Double.class,
        v -> AGGRO_RANGE_SQUARED = v * v,
        ConfigurationSection::getDouble
    ); }

    /** Attack initiation distance squared (loaded from raw radius and squared on assignment). */
    public static double APPROACH_DISTANCE_SQUARED = 36.0;
    static { register(
        "hostile.approach_distance",
        6.0, Double.class,
        v -> APPROACH_DISTANCE_SQUARED = v * v,
        ConfigurationSection::getDouble
    ); }

    /** Minimum allied Hostile count targeting the same player to trigger surround behaviour. */
    public static int SURROUND_MIN_ALLIES = 2;
    static { register(
        "hostile.surround_min_allies",
        2, Integer.class,
        v -> SURROUND_MIN_ALLIES = v,
        ConfigurationSection::getInt
    ); }

    /** Wind-up ticks before an attack is executed (~1.2 s at 20 TPS). */
    public static int PRE_ATTACK_TICKS = 24;
    static { register(
        "hostile.pre_attack_ticks",
        24, Integer.class,
        v -> PRE_ATTACK_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Retreat duration in ticks after an attack (~2 s at 20 TPS). */
    public static int RETREAT_TICKS = 40;
    static { register(
        "hostile.retreat_ticks",
        40, Integer.class,
        v -> RETREAT_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Health fraction threshold below which the mob flees (0.0–1.0). */
    public static double FLEE_HEALTH_FRACTION = 0.20;
    static { register(
        "hostile.flee_health_fraction",
        0.20, Double.class,
        v -> FLEE_HEALTH_FRACTION = v,
        ConfigurationSection::getDouble
    ); }

    /** OnGuard duration in ticks after an attack (~2 s at 20 TPS). */
    public static int ON_GUARD_TICKS = 40;
    static { register(
        "hostile.on_guard_ticks",
        40, Integer.class,
        v -> ON_GUARD_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Safe orbit radius squared for OnGuard strafing (loaded from raw distance and squared on assignment). */
    public static double ON_GUARD_SAFE_DISTANCE_SQUARED = 36.0;
    static { register(
        "hostile.on_guard_safe_distance",
        6.0, Double.class,
        v -> ON_GUARD_SAFE_DISTANCE_SQUARED = v * v,
        ConfigurationSection::getDouble
    ); }

    /** AttackReady hold duration in ticks — brief pause before a combo follow-up (~0.8 s at 20 TPS). */
    public static int ATTACK_READY_TICKS = 16;
    static { register(
        "hostile.attack_ready_ticks",
        16, Integer.class,
        v -> ATTACK_READY_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Cooldown in ticks after the mob uses its melee slash ability (1 s at 20 TPS). */
    public static int MOB_SLASH_COOLDOWN_TICKS = 20;
    static { register(
        "hostile.mob_slash_cooldown_ticks",
        20, Integer.class,
        v -> MOB_SLASH_COOLDOWN_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Cooldown in ticks after the mob uses its throw ability (3 s at 20 TPS). */
    public static int MOB_THROW_COOLDOWN_TICKS = 60;
    static { register(
        "hostile.mob_throw_cooldown_ticks",
        60, Integer.class,
        v -> MOB_THROW_COOLDOWN_TICKS = v,
        ConfigurationSection::getInt
    ); }

    /** Parabolic arc height multiplier for the mob throw ability. */
    public static double MOB_THROW_ARC_HEIGHT = 0.4;
    static { register(
        "hostile.mob_throw_arc_height",
        0.4, Double.class,
        v -> MOB_THROW_ARC_HEIGHT = v,
        ConfigurationSection::getDouble
    ); }

    /**
     * Probability (0.0–1.0) that the throw ability passes the {@code canUse} check when
     * off cooldown. Lower values make throws rarer relative to melee.
     */
    public static double MOB_THROW_WEIGHT = 0.3;
    static { register(
        "hostile.mob_throw_weight",
        0.3, Double.class,
        v -> MOB_THROW_WEIGHT = v,
        ConfigurationSection::getDouble
    ); }

    /** Pickup radius squared for weapon retrieval (loaded from raw distance and squared on assignment). */
    public static double MOB_RETRIEVE_PICKUP_RANGE_SQUARED = 4.0;
    static { register(
        "hostile.mob_retrieve_pickup_range",
        2.0, Double.class,
        v -> MOB_RETRIEVE_PICKUP_RANGE_SQUARED = v * v,
        ConfigurationSection::getDouble
    ); }

    /** DEU group tag for the Hostile display rig. Empty string disables the rig. */
    public static String DISPLAY_GROUP = "witha";
    static { register(
        "hostile.display_group",
        "witha", String.class,
        v -> DISPLAY_GROUP = v,
        ConfigurationSection::getString
    ); }

    /** Y offset applied to the display rig's ride position relative to the mob's passenger seat. */
    public static double DISPLAY_RIDE_OFFSET_Y = 0.0;
    static { register(
        "hostile.display_ride_offset_y",
        0.0, Double.class,
        v -> DISPLAY_RIDE_OFFSET_Y = v,
        ConfigurationSection::getDouble
    ); }

    /** Animation tag for the idle loop. Empty string skips registering this state. */
    public static String DISPLAY_ANIM_IDLE = "idle";
    static { register(
        "hostile.display_anim_idle",
        "idle", String.class,
        v -> DISPLAY_ANIM_IDLE = v,
        ConfigurationSection::getString
    ); }

    /** Animation tag for the walk loop. */
    public static String DISPLAY_ANIM_WALK = "walk";
    static { register(
        "hostile.display_anim_walk",
        "walk", String.class,
        v -> DISPLAY_ANIM_WALK = v,
        ConfigurationSection::getString
    ); }

    /** Animation tag for the falling loop. */
    public static String DISPLAY_ANIM_FALL = "fall";
    static { register(
        "hostile.display_anim_fall",
        "fall", String.class,
        v -> DISPLAY_ANIM_FALL = v,
        ConfigurationSection::getString
    ); }

    /** Animation tag for the melee attack (plays once, locked until complete). */
    public static String DISPLAY_ANIM_MELEE = "melee";
    static { register(
        "hostile.display_anim_melee",
        "melee", String.class,
        v -> DISPLAY_ANIM_MELEE = v,
        ConfigurationSection::getString
    ); }

    /**
     * Teleport-duration applied to every display entity in the rig (in ticks).
     * Higher values give smoother movement at the cost of slightly delayed response.
     * 3 is a good starting point; set to 0 to disable smoothing.
     */
    public static int DISPLAY_TELEPORT_DURATION = 3;
    static { register(
        "hostile.display_teleport_duration",
        3, Integer.class,
        v -> DISPLAY_TELEPORT_DURATION = v,
        ConfigurationSection::getInt
    ); }
}
