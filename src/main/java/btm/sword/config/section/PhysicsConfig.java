package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

import btm.sword.action.throwing.types.ThrownItem;
import btm.sword.config.Config;

/**
 * Physics simulation parameters for projectiles and combat movement.
 * <p>
 * Controls thrown item behavior (gravity, rotation, offsets) and attack knockback
 * physics. All distance values are in <b>blocks</b>, angles in <b>radians</b>.
 * </p>
 *
 * <h2>Key Subsystems</h2>
 * <ul>
 *   <li><b>Thrown Items</b> - Gravity damping, rotation speeds, visual offsets</li>
 *   <li><b>Attack Velocity</b> - Knockback vectors, grounded damping, vertical boost</li>
 * </ul>
 *
 * @see ThrownItem Thrown item physics implementation
 * @see btm.sword.combat.attack.Attack Attack knockback application
 */
public final class PhysicsConfig {

    private PhysicsConfig() {}

    // Thrown items configuration
    public static double THROWN_ITEMS_GRAVITY_DAMPER = 46.0; // damping factor (higher = less gravity effect)
    static { register(
        "physics.thrown_items_gravity_damper",
        THROWN_ITEMS_GRAVITY_DAMPER, Double.class,
        v -> THROWN_ITEMS_GRAVITY_DAMPER = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_TRAJECTORY_ROTATION = 0.03696; // radians/tick
    static { register(
        "physics.thrown_items_trajectory_rotation",
        THROWN_ITEMS_TRAJECTORY_ROTATION, Double.class,
        v -> THROWN_ITEMS_TRAJECTORY_ROTATION = v,
        ConfigurationSection::getDouble); }

    public static float THROWN_ITEMS_DISPLAY_OFFSET_X = -0.5f;
    static { register(
        "physics.thrown_items_display_offset_x",
        THROWN_ITEMS_DISPLAY_OFFSET_X, Float.class,
        v -> THROWN_ITEMS_DISPLAY_OFFSET_X = v,
        Config::loadFloat); }

    public static float THROWN_ITEMS_DISPLAY_OFFSET_Y = 0.1f;
    static { register(
        "physics.thrown_items_display_offset_y",
        THROWN_ITEMS_DISPLAY_OFFSET_Y, Float.class,
        v -> THROWN_ITEMS_DISPLAY_OFFSET_Y = v,
        Config::loadFloat); }

    public static float THROWN_ITEMS_DISPLAY_OFFSET_Z = 0.5f;
    static { register(
        "physics.thrown_items_display_offset_z",
        THROWN_ITEMS_DISPLAY_OFFSET_Z, Float.class,
        v -> THROWN_ITEMS_DISPLAY_OFFSET_Z = v,
        Config::loadFloat); }

    public static double THROWN_ITEMS_ORIGIN_OFFSET_FORWARD = 0.5;
    static { register(
        "physics.thrown_items_origin_offset_forward",
        THROWN_ITEMS_ORIGIN_OFFSET_FORWARD, Double.class,
        v -> THROWN_ITEMS_ORIGIN_OFFSET_FORWARD = v,
        ConfigurationSection::getDouble); }

    public static double THROWN_ITEMS_ORIGIN_OFFSET_UP = 0.1;
    static { register(
        "physics.thrown_items_origin_offset_up",
        THROWN_ITEMS_ORIGIN_OFFSET_UP, Double.class,
        v -> THROWN_ITEMS_ORIGIN_OFFSET_UP = v,
        ConfigurationSection::getDouble); }

    public static double THROWN_ITEMS_ORIGIN_OFFSET_BACK = -0.25;
    static { register(
        "physics.thrown_items_origin_offset_back",
        THROWN_ITEMS_ORIGIN_OFFSET_BACK, Double.class,
        v -> THROWN_ITEMS_ORIGIN_OFFSET_BACK = v,
        ConfigurationSection::getDouble
    ); }

    // Thrown items rotation speed configuration
    public static double THROWN_ITEMS_ROTATION_SPEED_SWORD = 0.0; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_sword",
        THROWN_ITEMS_ROTATION_SPEED_SWORD, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_SWORD = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_AXE = -Math.PI / 8; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_axe",
        THROWN_ITEMS_ROTATION_SPEED_AXE, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_AXE = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_HOE = -Math.PI / 8; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_hoe",
        THROWN_ITEMS_ROTATION_SPEED_HOE, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_HOE = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_PICKAXE = -Math.PI / 8; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_pickaxe",
        THROWN_ITEMS_ROTATION_SPEED_PICKAXE, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_PICKAXE = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_SHOVEL = -Math.PI / 8; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_shovel",
        THROWN_ITEMS_ROTATION_SPEED_SHOVEL, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_SHOVEL = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_SHIELD = -Math.PI / 8; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_shield",
        THROWN_ITEMS_ROTATION_SPEED_SHIELD, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_SHIELD = v,
        ConfigurationSection::getDouble
    ); }

    public static double THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED = Math.PI / 32; // radians/tick
    static { register(
        "physics.thrown_items_rotation_speed_default_speed",
        THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED, Double.class,
        v -> THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED = v,
        ConfigurationSection::getDouble
    ); }

    // Attack velocity configuration
    public static double ATTACK_VELOCITY_GROUNDED_DAMPING_HORIZONTAL = 0.3; // multiplier (0-1)
    static { register(
        "physics.attack_velocity_grounded_damping_horizontal",
        ATTACK_VELOCITY_GROUNDED_DAMPING_HORIZONTAL, Double.class,
        v -> ATTACK_VELOCITY_GROUNDED_DAMPING_HORIZONTAL = v,
        ConfigurationSection::getDouble
    ); }

    public static double ATTACK_VELOCITY_GROUNDED_DAMPING_VERTICAL = 0.4; // multiplier (0-1)
    static { register(
        "physics.attack_velocity_grounded_damping_vertical",
        ATTACK_VELOCITY_GROUNDED_DAMPING_VERTICAL, Double.class,
        v -> ATTACK_VELOCITY_GROUNDED_DAMPING_VERTICAL = v,
        ConfigurationSection::getDouble
    ); }

    public static double ATTACK_VELOCITY_KNOCKBACK_VERTICAL_BASE = 0.25; // blocks/tick
    static { register(
        "physics.attack_velocity_knockback_vertical_base",
        ATTACK_VELOCITY_KNOCKBACK_VERTICAL_BASE, Double.class,
        v -> ATTACK_VELOCITY_KNOCKBACK_VERTICAL_BASE = v,
        ConfigurationSection::getDouble
    ); }

    public static double ATTACK_VELOCITY_KNOCKBACK_HORIZONTAL_MODIFIER = 0.1; // multiplier
    static { register(
        "physics.attack_velocity_knockback_horizontal_modifier",
        ATTACK_VELOCITY_KNOCKBACK_HORIZONTAL_MODIFIER, Double.class,
        v -> ATTACK_VELOCITY_KNOCKBACK_HORIZONTAL_MODIFIER = v,
        ConfigurationSection::getDouble); }

    public static double ATTACK_VELOCITY_KNOCKBACK_NORMAL_MULTIPLIER = 0.7; // multiplier
    static { register(
        "physics.attack_velocity_knockback_normal_multiplier",
        ATTACK_VELOCITY_KNOCKBACK_NORMAL_MULTIPLIER, Double.class,
        v -> ATTACK_VELOCITY_KNOCKBACK_NORMAL_MULTIPLIER = v,
        ConfigurationSection::getDouble); }
}
