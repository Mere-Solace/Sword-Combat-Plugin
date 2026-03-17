package btm.sword.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import btm.sword.system.action.attack.AttackAction;
import btm.sword.system.action.movement.MovementAction;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.attack.style.AttackType;
import btm.sword.utility.sound.SoundType;
import net.kyori.adventure.text.format.TextColor;

/**
 * Static configuration class for Sword: Combat Evolved.
 * <p>
 * Provides centralized, type-safe access to all configuration values.
 * Values are loaded from config.yaml by {@link ConfigManager} and can be
 * hot-reloaded at runtime using /sword reload.
 * </p>
 * <p>
 * Uses a self-registering ConfigEntry pattern where each field registers itself
 * in a static initializer block. ConfigManager loops through the ENTRIES list
 * for reload/save operations.
 * </p>
 */
public class Config {

    // ==============================================================================
    // CONFIG ENTRY REGISTRATION SYSTEM
    // ==============================================================================

    /**
     * ConfigEntry represents a single configuration value with metadata for loading and saving.
     * <p>
     * Each entry contains:
     * <ul>
     *   <li><b>path</b> - YAML path (e.g., "angles.umbral_blade_idle_period")</li>
     *   <li><b>defaultValue</b> - Default value if not in config.yaml</li>
     *   <li><b>type</b> - Java class type for type safety</li>
     *   <li><b>assign</b> - Consumer lambda to update the static field</li>
     *   <li><b>loader</b> - Custom loader for type-specific YAML parsing</li>
     * </ul>
     * </p>
     */
    public static final class ConfigEntry<T> {
        public final String path;
        public final T defaultValue;
        public final Class<T> type;
        public final Consumer<T> assign;
        public final Loader<T> loader;

        /**
         * Functional interface for custom YAML loading logic.
         * @param <T> The type of value to load
         */
        @FunctionalInterface
        public interface Loader<T> {
            T load(ConfigurationSection section, String path, T defaultValue);
        }

        public ConfigEntry(String path, T defaultValue, Class<T> type, Consumer<T> assign, Loader<T> loader) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.type = type;
            this.assign = assign;
            this.loader = loader;
        }
    }

    /**
     * List of all registered configuration entries.
     * <p>
     * Populated by static initializer blocks in each config section.
     * ConfigManager iterates this list for reload/save operations.
     * </p>
     */
    public static final List<ConfigEntry<?>> ENTRIES = new ArrayList<>();

    /**
     * Register a configuration entry.
     * <p>
     * Called from static initializer blocks to add entries to the ENTRIES list.
     * </p>
     */
    public static <T> void register(String path, T defaultValue, Class<T> type, Consumer<T> assign, ConfigEntry.Loader<T> loader) {
        ENTRIES.add(new ConfigEntry<>(path, defaultValue, type, assign, loader));
    }

    /**
     * Forces all nested static classes in {@link Config} to initialize.
     * <p>
     * Each nested class registers its entries in a static initializer block; those
     * initializers only run when the class is first accessed. Call this method once
     * during plugin startup (before {@code ConfigManager.loadConfig()}) so that every
     * section is present in {@link #ENTRIES} before the first read or menu open.
     * </p>
     */
    public static void forceInitializeAll() {
        for (Class<?> nested : Config.class.getDeclaredClasses()) {
            try {
                Class.forName(nested.getName(), true, nested.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                // unreachable — class is already known to the JVM
            }
        }
    }

    // ==============================================================================
    // HELPER METHODS FOR COMMON TYPES
    // ==============================================================================

    /**
     * Loader for List<String> configuration values.
     */
    public static List<String> loadStringList(ConfigurationSection section, String path, List<String> defaultValue) {
        return section.contains(path) ? section.getStringList(path) : defaultValue;
    }

    public static TextColor loadTextColor(ConfigurationSection section, String path, TextColor defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return TextColor.fromHexString(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static org.bukkit.Color loadColor(ConfigurationSection section, String path, org.bukkit.Color defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null || value.isEmpty()) return defaultValue;

        try {
            if (value.startsWith("#")) value = value.substring(1);

            int rgb = Integer.parseInt(value, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            return org.bukkit.Color.fromRGB(r, g, b);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Loader for List<EntityType> configuration values.
     */
    public static List<EntityType> loadEntityTypeList(ConfigurationSection section, String path, List<EntityType> defaultValue) {
        if (!section.contains(path)) return defaultValue;
        List<String> names = section.getStringList(path);
        return names.stream()
            .map(name -> {
                try {
                    return EntityType.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Loader for Enum configuration values.
     */
    public static <E extends Enum<E>> E loadEnum(ConfigurationSection section, String path, E defaultValue, Class<E> enumClass) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static Float loadFloat(ConfigurationSection section, String path, Float defaultValue) {
        return (float) section.getDouble(path, defaultValue);
    }

    /**
     * Loader for {@link Vector} config values.
     *
     * <p>Expects the config path to point to a section with {@code x}, {@code y}, and {@code z}
     * sub-keys. Missing sub-keys fall back to the corresponding component of {@code defaultValue}.
     */
    public static Vector loadVector(ConfigurationSection section, String path, Vector defaultValue) {
        if (!section.contains(path)) return defaultValue.clone();
        ConfigurationSection vec = section.getConfigurationSection(path);
        if (vec == null) return defaultValue.clone();
        return new Vector(
            vec.getDouble("x", defaultValue.getX()),
            vec.getDouble("y", defaultValue.getY()),
            vec.getDouble("z", defaultValue.getZ())
        );
    }

    /**
     * Loader for SoundType enum values.
     */
    public static SoundType loadSoundType(ConfigurationSection section, String path, SoundType defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return SoundType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Loader for {@link Material} config values. Reads a material name string and resolves it
     * to the matching {@link Material} enum constant, falling back to {@code defaultValue} on
     * missing path, null, or unrecognised name.
     */
    public static Material loadMaterial(ConfigurationSection section, String path, Material defaultValue) {
        return loadEnum(section, path, defaultValue, Material.class);
    }

    public static AttackType loadAttackType(ConfigurationSection section, String path, AttackType defaultValue) {
        if (!section.contains(path)) return defaultValue;
        String value = section.getString(path);
        if (value == null) return defaultValue;
        try {
            return AttackType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    // ==============================================================================
    //region DIRECTION - Directional vector constants (immutable via cloning)
    // ==============================================================================
    /**
     * Directional vector constants for common 3D directions.
     * <p>
     * All vectors are <b>unit vectors</b> (length = 1.0). Methods return clones
     * to prevent external mutation of the cached vectors.
     * </p>
     *
     * <h2>Coordinate System</h2>
     * <ul>
     *   <li><b>+X</b>: East</li>
     *   <li><b>-X</b>: West</li>
     *   <li><b>+Y</b>: Up</li>
     *   <li><b>-Y</b>: Down</li>
     *   <li><b>+Z</b>: South</li>
     *   <li><b>-Z</b>: North</li>
     * </ul>
     *
     * @see org.bukkit.util.Vector Bukkit vector implementation
     */
    public static class Direction {
        private static final Vector UP = new Vector(0, 1, 0);
        public static Vector UP() { return UP.clone(); }

        private static final Vector DOWN = new Vector(0, -1, 0);
        public static Vector DOWN() { return DOWN.clone(); }

        private static final Vector NORTH = new Vector(0, 0, -1);
        public static Vector NORTH() { return NORTH.clone(); }

        private static final Vector SOUTH = new Vector(0, 0, 1);
        public static Vector SOUTH() { return SOUTH.clone(); }

        private static final Vector OUT_UP = new Vector(0, 1, 1);
        public static Vector OUT_UP() { return OUT_UP.clone(); }

        private static final Vector OUT_DOWN = new Vector(0, -1, 1);
        public static Vector OUT_DOWN() { return OUT_DOWN.clone(); }
    }
    //endregion

    // ==============================================================================
    //region COLOR
    // ==============================================================================
    public static class SwordColor {
        public static TextColor TEXT_RESOURCE_COLOR = TextColor.color(222, 222, 222);
        static { register(
            "color.text_resource_color",
            TEXT_RESOURCE_COLOR, TextColor.class,
            v -> TEXT_RESOURCE_COLOR = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_ASPECT_COLOR = TextColor.color(0, 159, 255);
        static { register(
            "color.text_aspect_color",
            TEXT_ASPECT_COLOR, TextColor.class,
            v -> TEXT_ASPECT_COLOR = v,
            Config::loadTextColor
        ); }

        public static TextColor TITLE_INPUT_STRING = TextColor.color(151, 0, 0);
        static { register(
            "color.title_input_string",
            TITLE_INPUT_STRING, TextColor.class,
            v -> TITLE_INPUT_STRING = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_ITEM_NAME = TextColor.color(0, 110, 151);
        static { register(
            "color.text_item_name",
            TEXT_ITEM_NAME, TextColor.class,
            v -> TEXT_ITEM_NAME = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_ITEM_CONTROLS = TextColor.color(173, 109, 255);
        static { register(
            "color.text_item_controls",
            TEXT_ITEM_CONTROLS, TextColor.class,
            v -> TEXT_ITEM_CONTROLS = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_ITEM_HEADER = TextColor.color(225, 225, 225);
        static { register(
            "color.text_item_header",
            TEXT_ITEM_HEADER, TextColor.class,
            v -> TEXT_ITEM_HEADER = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_ITEM_BASE = TextColor.color(121, 142, 168);
        static { register(
            "color.text_item_base",
            TEXT_ITEM_BASE, TextColor.class,
            v -> TEXT_ITEM_BASE = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_COOL = TextColor.color(240, 161, 12);
        static { register(
            "color.text_cool",
            TEXT_COOL, TextColor.class,
            v -> TEXT_COOL = v,
            Config::loadTextColor
        ); }

        public static TextColor TEXT_COOL_DARK = TextColor.color(51, 60, 75);
        static { register(
            "color.text_cool_dark",
            TEXT_COOL_DARK, TextColor.class,
            v -> TEXT_COOL_DARK = v,
            Config::loadTextColor
        ); }

        public static Color UMBRAL_GLOW = Color.fromRGB(36, 8, 8);
        static { register("color.umbral_glow",
            UMBRAL_GLOW, Color.class,
            v -> UMBRAL_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color FEROCIOUS_SWEEP = Color.fromRGB(255, 0, 0);
        static { register("color.ferocious_sweep",
            FEROCIOUS_SWEEP, Color.class,
            v -> FEROCIOUS_SWEEP = v,
            Config::loadColor
        ); }

        public static @Nullable Color STANDBY_GLOW = Color.fromRGB(255, 255, 255);
        static { register("color.standby_glow",
            STANDBY_GLOW, Color.class,
            v -> STANDBY_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color ATTACK_QUICK_GLOW = Color.fromRGB(181, 121, 27);
        static { register("color.attack_quick_glow",
            ATTACK_QUICK_GLOW, Color.class,
            v -> ATTACK_QUICK_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color LUNGE_GLOW = Color.fromRGB(255, 0, 0);
        static { register("color.lunge_glow",
            LUNGE_GLOW, Color.class,
            v -> LUNGE_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color LODGED_GLOW = Color.fromRGB(0, 0, 0);
        static { register("color.lodged_glow",
            LODGED_GLOW, Color.class,
            v -> LODGED_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color GRAB_IMPALE_GLOW = Color.fromRGB(255, 80, 0);
        static { register("color.grab_impale_glow",
            GRAB_IMPALE_GLOW, Color.class,
            v -> GRAB_IMPALE_GLOW = v,
            Config::loadColor
        ); }

        public static @Nullable Color RECALL_GLOW = Color.fromRGB(255, 118, 135);
        static { register("color.recall_glow",
            RECALL_GLOW, Color.class,
            v -> RECALL_GLOW = v,
            Config::loadColor
        ); }
    }
    //endregion

    // ==============================================================================
    //region ANGLE - Common angle constants
    // ==============================================================================
    /**
     * Angle constants used throughout the combat system.
     * <p>
     * All angle values are in <b>radians</b> (π = 180°). Used primarily for
     * entity rotation, attack arcs, and visual effects.
     * </p>
     *
     * @see btm.sword.system.entity.umbral.UmbralBlade Umbral blade rotation behavior
     */
    public static class Angle {
        public static float UMBRAL_BLADE_IDLE_PERIOD = (float) Math.PI / 8; // radians (22.5°)
        static { register(
            "angle.umbral_blade_idle_period",
            UMBRAL_BLADE_IDLE_PERIOD, Float.class,
            v -> UMBRAL_BLADE_IDLE_PERIOD = v,
            Config::loadFloat
        ); }
    }
    //endregion

    // ==============================================================================
    //region PHYSICS - Projectile motion, gravity, and velocity
    // ==============================================================================
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
     * @see btm.sword.system.attack.Attack Attack knockback application
     */
    public static class Physics {
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
    //endregion

    // ==============================================================================
    //region COMBAT - Damage, hitboxes, attack patterns, combat mechanics
    // ==============================================================================
    /**
     * Combat system configuration for damage, hitboxes, and attack mechanics.
     * <p>
     * Defines damage calculations, hitbox dimensions, attack timing, range multipliers,
     * impalement mechanics, and entity exemptions. Distances in <b>blocks</b>, time in
     * <b>ticks</b> (20 ticks/second), damage in <b>health points</b> (1 heart = 2 HP).
     * </p>
     *
     * <h2>Key Subsystems</h2>
     * <ul>
     *   <li><b>Attacks</b> - Base damage, cast timing, duration, range multipliers</li>
     *   <li><b>Hitboxes</b> - 3D collision box dimensions (reach/width/height)</li>
     *   <li><b>Thrown Damage</b> - Projectile damage, knockback, armor interactions</li>
     *   <li><b>Impalement</b> - Damage-over-time, pinning, head detection</li>
     * </ul>
     *
     * @see btm.sword.system.attack.Attack Attack execution and damage application
     * @see AttackAction Attack state machine
     */
    public static class Combat {
        public static double SHARDS_LOST_PERCENT_TOUGHNESS_RESET = 0.3; // Percent of HP
        static { register("combat.shards_lost_percent_toughness_reset",
            SHARDS_LOST_PERCENT_TOUGHNESS_RESET, Double.class,
            v -> SHARDS_LOST_PERCENT_TOUGHNESS_RESET = v,
            ConfigurationSection::getDouble
        ); }

        public static float TOUGHNESS_RECHARGE_PERCENT = 0.75f;
        static { register("combat.toughness_recharge_percent",
            TOUGHNESS_RECHARGE_PERCENT, Float.class,
            v -> TOUGHNESS_RECHARGE_PERCENT = v,
            Config::loadFloat
        ); }

        // Attacks configuration
        public static double ATTACKS_BASE_DAMAGE = 20.0; // HP (1 heart = 2 HP)
        static { register("combat.attacks_base_damage",
                ATTACKS_BASE_DAMAGE, Double.class,
                v -> ATTACKS_BASE_DAMAGE = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACKS_DOWN_AIR_THRESHOLD = -0.4; // dot product (-1 to 1)
        static { register("combat.attacks_down_air_threshold",
                ATTACKS_DOWN_AIR_THRESHOLD, Double.class,
                v -> ATTACKS_DOWN_AIR_THRESHOLD = v,
                ConfigurationSection::getDouble
        ); }

        public static int ATTACKS_CAST_TIMING_MIN_DURATION = 25; // 1/2 tick (1/40th of a second)
        static { register("combat.attacks_cast_timing_min_duration",
                ATTACKS_CAST_TIMING_MIN_DURATION, Integer.class,
                v -> ATTACKS_CAST_TIMING_MIN_DURATION = v,
                ConfigurationSection::getInt
        ); }

        public static int ATTACKS_CAST_TIMING_MAX_DURATION = 200;
        static { register("combat.attacks_cast_timing_max_duration",
                ATTACKS_CAST_TIMING_MAX_DURATION, Integer.class,
                v -> ATTACKS_CAST_TIMING_MAX_DURATION = v,
                ConfigurationSection::getInt
        ); }

        public static double ATTACKS_CAST_TIMING_REDUCTION_RATE = 0.2; // ticks/combo_count
        static { register("combat.attacks_cast_timing_reduction_rate",
                ATTACKS_CAST_TIMING_REDUCTION_RATE, Double.class,
                v -> ATTACKS_CAST_TIMING_REDUCTION_RATE = v,
                ConfigurationSection::getDouble
        ); }

        public static int ATTACKS_DURATION_MULTIPLIER = 500; // milliseconds multiplier
        static { register("combat.attacks_duration_multiplier",
                ATTACKS_DURATION_MULTIPLIER, Integer.class,
                v -> ATTACKS_DURATION_MULTIPLIER = v,
                ConfigurationSection::getInt
        ); }

        public static double ATTACKS_COOLDOWN_MULT = 2.0; // ticks/combo_count
        static { register("combat.attacks_cooldown_mult",
            ATTACKS_COOLDOWN_MULT, Double.class,
            v -> ATTACKS_COOLDOWN_MULT = v,
            ConfigurationSection::getDouble
        ); }

        // Attacks range multipliers configuration
        public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_1 = 1.4;
        static { register("combat.attacks_range_multipliers_basic_1",
                ATTACKS_RANGE_MULTIPLIERS_BASIC_1, Double.class,
                v -> ATTACKS_RANGE_MULTIPLIERS_BASIC_1 = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_2 = 1.4;
        static { register("combat.attacks_range_multipliers_basic_2",
                ATTACKS_RANGE_MULTIPLIERS_BASIC_2, Double.class,
                v -> ATTACKS_RANGE_MULTIPLIERS_BASIC_2 = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_3 = 1.4;
        static { register("combat.attacks_range_multipliers_basic_3",
                ATTACKS_RANGE_MULTIPLIERS_BASIC_3, Double.class,
                v -> ATTACKS_RANGE_MULTIPLIERS_BASIC_3 = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACKS_RANGE_MULTIPLIERS_NEUTRAL_AIR = 1.3;
        static { register("combat.attacks_range_multipliers_neutral_air",
                ATTACKS_RANGE_MULTIPLIERS_NEUTRAL_AIR, Double.class,
                v -> ATTACKS_RANGE_MULTIPLIERS_NEUTRAL_AIR = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACKS_RANGE_MULTIPLIERS_DOWN_AIR = 1.2;
        static { register("combat.attacks_range_multipliers_down_air",
                ATTACKS_RANGE_MULTIPLIERS_DOWN_AIR, Double.class,
                v -> ATTACKS_RANGE_MULTIPLIERS_DOWN_AIR = v,
                ConfigurationSection::getDouble
        ); }

        /** Minimum sweep distance for heavy attack paths. */
        public static double HEAVY_ATTACK_MIN_SWEEP_DISTANCE = 5.0;
        static { register("combat.heavy_attack_min_sweep_distance",
                HEAVY_ATTACK_MIN_SWEEP_DISTANCE, Double.class,
                v -> HEAVY_ATTACK_MIN_SWEEP_DISTANCE = v,
                ConfigurationSection::getDouble
        ); }

        /** Step distance for heavy attack secant particle path. */
        public static double HEAVY_ATTACK_SECANT_STEP = 0.25;
        static { register("combat.heavy_attack_secant_step",
                HEAVY_ATTACK_SECANT_STEP, Double.class,
                v -> HEAVY_ATTACK_SECANT_STEP = v,
                ConfigurationSection::getDouble
        ); }

        public static float SWEEP_ATTACK_X_SCALE = 0.5f;
        static { register(
            "combat.sweep_attack_x_scale",
            SWEEP_ATTACK_X_SCALE, Float.class,
            v -> SWEEP_ATTACK_X_SCALE = v,
            Config::loadFloat
        ); }

        public static float SWEEP_ATTACK_Y_SCALE = 0.25f;
        static { register(
            "combat.sweep_attack_y_scale",
            SWEEP_ATTACK_Y_SCALE, Float.class,
            v -> SWEEP_ATTACK_Y_SCALE = v,
            Config::loadFloat
        ); }

        public static float SWEEP_ATTACK_Z_SCALE = 2.5f;
        static { register(
            "combat.sweep_attack_z_scale",
            SWEEP_ATTACK_Z_SCALE, Float.class,
            v -> SWEEP_ATTACK_Z_SCALE = v,
            Config::loadFloat
        ); }


        // Hitboxes configuration
        public static double HITBOXES_BASIC_REACH = 1.5;
        static { register("combat.hitboxes_basic_reach",
                HITBOXES_BASIC_REACH, Double.class,
                v -> HITBOXES_BASIC_REACH = v,
                ConfigurationSection::getDouble

        ); }

        public static double HITBOXES_BASIC_WIDTH = 1.5;
        static { register("combat.hitboxes_basic_width",
                HITBOXES_BASIC_WIDTH, Double.class,
                v -> HITBOXES_BASIC_WIDTH = v,
                ConfigurationSection::getDouble
        ); }

        public static double HITBOXES_BASIC_HEIGHT = 1.5;
        static { register("combat.hitboxes_basic_height",
                HITBOXES_BASIC_HEIGHT, Double.class,
                v -> HITBOXES_BASIC_HEIGHT = v,
                ConfigurationSection::getDouble
        ); }

        public static double HITBOXES_DOWN_AIR_REACH = 1.6;
        static { register("combat.hitboxes_down_air_reach",
                HITBOXES_DOWN_AIR_REACH, Double.class,
                v -> HITBOXES_DOWN_AIR_REACH = v,
                ConfigurationSection::getDouble
        ); }

        public static double HITBOXES_DOWN_AIR_WIDTH = 1.4;
        static { register("combat.hitboxes_down_air_width",
                HITBOXES_DOWN_AIR_WIDTH, Double.class,
                v -> HITBOXES_DOWN_AIR_WIDTH = v,
                ConfigurationSection::getDouble
        ); }

        public static double HITBOXES_DOWN_AIR_HEIGHT = 2.5;
        static { register("combat.hitboxes_down_air_height",
                HITBOXES_DOWN_AIR_HEIGHT, Double.class,
                v -> HITBOXES_DOWN_AIR_HEIGHT = v,
                ConfigurationSection::getDouble
        ); }

        public static double HITBOXES_SECANT_RADIUS = 1; // Must be above 1
        static { register("combat.hitboxes_secant_radius",
                HITBOXES_SECANT_RADIUS, Double.class,
                v -> HITBOXES_SECANT_RADIUS = v,
                ConfigurationSection::getDouble
        ); }

        // Thrown damage configuration
        public static double THROWN_DAMAGE_SWORD_DAMAGE_MULTIPLIER = 1.0;
        static { register("combat.thrown_damage_sword_damage_multiplier",
                THROWN_DAMAGE_SWORD_DAMAGE_MULTIPLIER, Double.class,
                v -> THROWN_DAMAGE_SWORD_DAMAGE_MULTIPLIER = v,
                ConfigurationSection::getDouble
        ); }

        public static double THROWN_DAMAGE_ITEM_VELOCITY_MULTIPLIER = 1.5;
        static {
            register("combat.thrown_damage_item_velocity_multiplier",
                THROWN_DAMAGE_ITEM_VELOCITY_MULTIPLIER, Double.class,
                v -> THROWN_DAMAGE_ITEM_VELOCITY_MULTIPLIER = v,
                ConfigurationSection::getDouble
        ); }

        public static double THROWN_DAMAGE_BASE_THROWN_DAMAGE = 12.0;
        static { register("combat.thrown_damage_base_thrown_damage",
                THROWN_DAMAGE_BASE_THROWN_DAMAGE, Double.class,
                v -> THROWN_DAMAGE_BASE_THROWN_DAMAGE = v,
                ConfigurationSection::getDouble
        ); }

        // Thrown damage sword/axe configuration
        public static int THROWN_DAMAGE_SWORD_AXE_INVULNERABILITY_TICKS = 0;
        static { register(
                "combat.thrown_damage_sword_axe_invulnerability_ticks",
                THROWN_DAMAGE_SWORD_AXE_INVULNERABILITY_TICKS, Integer.class,
                v -> THROWN_DAMAGE_SWORD_AXE_INVULNERABILITY_TICKS = v,
                ConfigurationSection::getInt
        ); }

        public static int THROWN_DAMAGE_SWORD_AXE_BASE_SHARDS = 2;
        static { register(
                "combat.thrown_damage_sword_axe_base_shards",
                THROWN_DAMAGE_SWORD_AXE_BASE_SHARDS, Integer.class,
                v -> THROWN_DAMAGE_SWORD_AXE_BASE_SHARDS = v,
                ConfigurationSection::getInt
        ); }

        public static float THROWN_DAMAGE_SWORD_AXE_TOUGHNESS_DAMAGE = 75.0f;
        static { register(
                "combat.thrown_damage_sword_axe_toughness_damage",
                THROWN_DAMAGE_SWORD_AXE_TOUGHNESS_DAMAGE, Float.class,
                v -> THROWN_DAMAGE_SWORD_AXE_TOUGHNESS_DAMAGE = v,
                Config::loadFloat
        ); }

        public static float THROWN_DAMAGE_SWORD_AXE_SOULFIRE_REDUCTION = 50.0f;
        static { register(
                "combat.thrown_damage_sword_axe_soulfire_reduction",
                THROWN_DAMAGE_SWORD_AXE_SOULFIRE_REDUCTION, Float.class,
                v -> THROWN_DAMAGE_SWORD_AXE_SOULFIRE_REDUCTION = v,
                Config::loadFloat
        ); }

        public static double THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED = 0.7;
        static { register(
                "combat.thrown_damage_sword_axe_knockback_grounded",
                THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED, Double.class,
                v -> THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED = v,
                ConfigurationSection::getDouble
        ); }

        public static double THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE = 1.0;
        static { register(
                "combat.thrown_damage_sword_axe_knockback_airborne",
                THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE, Double.class,
                v -> THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE = v,
                ConfigurationSection::getDouble
        ); }

        // Thrown damage other items configuration
        public static int THROWN_DAMAGE_OTHER_INVULNERABILITY_TICKS = 0;
        static { register(
                "combat.thrown_damage_other_invulnerability_ticks",
                THROWN_DAMAGE_OTHER_INVULNERABILITY_TICKS, Integer.class,
                v -> THROWN_DAMAGE_OTHER_INVULNERABILITY_TICKS = v,
                ConfigurationSection::getInt
        ); }

        public static int THROWN_DAMAGE_OTHER_BASE_SHARDS = 2;
        static { register(
                "combat.thrown_damage_other_base_shards",
                THROWN_DAMAGE_OTHER_BASE_SHARDS, Integer.class,
                v -> THROWN_DAMAGE_OTHER_BASE_SHARDS = v,
                ConfigurationSection::getInt
        ); }

        public static float THROWN_DAMAGE_OTHER_TOUGHNESS_DAMAGE = 75.0f;
        static { register(
                "combat.thrown_damage_other_toughness_damage",
                THROWN_DAMAGE_OTHER_TOUGHNESS_DAMAGE, Float.class,
                v -> THROWN_DAMAGE_OTHER_TOUGHNESS_DAMAGE = v,
                Config::loadFloat
        ); }

        public static float THROWN_DAMAGE_OTHER_SOULFIRE_REDUCTION = 50.0f;
        static { register(
                "combat.thrown_damage_other_soulfire_reduction",
                THROWN_DAMAGE_OTHER_SOULFIRE_REDUCTION, Float.class,
                v -> THROWN_DAMAGE_OTHER_SOULFIRE_REDUCTION = v,
                Config::loadFloat
        ); }

        public static double THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER = 0.7;
        static { register(
                "combat.thrown_damage_other_knockback_multiplier",
                THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER, Double.class,
                v -> THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER = v,
                ConfigurationSection::getDouble
        ); }

        public static float THROWN_DAMAGE_OTHER_EXPLOSION_POWER = 1.0f;
        static { register(
                "combat.thrown_damage_other_explosion_power",
                THROWN_DAMAGE_OTHER_EXPLOSION_POWER, Float.class,
                v -> THROWN_DAMAGE_OTHER_EXPLOSION_POWER = v,
                Config::loadFloat
        ); }

        // Impalement configuration
        public static double IMPALEMENT_DAMAGE_PER_TICK = 2.0;
        static { register(
                "combat.impalement_damage_per_tick",
                IMPALEMENT_DAMAGE_PER_TICK, Double.class,
                v -> IMPALEMENT_DAMAGE_PER_TICK = v,
                ConfigurationSection::getDouble
        ); }

        public static int IMPALEMENT_TICKS_BETWEEN_DAMAGE = 10;
        static { register(
                "combat.impalement_ticks_between_damage",
                IMPALEMENT_TICKS_BETWEEN_DAMAGE, Integer.class,
                v -> IMPALEMENT_TICKS_BETWEEN_DAMAGE = v,
                ConfigurationSection::getInt
        ); }

        public static int IMPALEMENT_MAX_IMPALEMENTS = 3;
        static { register(
                "combat.impalement_max_impalements",
                IMPALEMENT_MAX_IMPALEMENTS, Integer.class,
                v -> IMPALEMENT_MAX_IMPALEMENTS = v,
                ConfigurationSection::getInt
        ); }

        public static double IMPALEMENT_HEAD_ZONE_RATIO = 0.8; // 0-1 (fraction of entity height)
        static { register(
                "combat.impalement_head_zone_ratio",
                IMPALEMENT_HEAD_ZONE_RATIO, Double.class,
                v -> IMPALEMENT_HEAD_ZONE_RATIO = v,
                ConfigurationSection::getDouble
        ); }

        @SuppressWarnings("unchecked")
        static Class<List<EntityType>> entityListClass() {
            return (Class<List<EntityType>>) (Class<?>) List.class;
        }

        public static List<EntityType> IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS = List.of(EntityType.SPIDER);
        static { register(
                "combat.impalement_head_follow_exceptions",
                IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS, entityListClass(),
                v -> IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS = v,
                Config::loadEntityTypeList
        ); }

        public static int IMPALEMENT_PIN_MAX_ITERATIONS = 200; // in ticks
        static { register(
                "combat.impalement_pin_max_iterations",
                IMPALEMENT_PIN_MAX_ITERATIONS, Integer.class,
                v -> IMPALEMENT_PIN_MAX_ITERATIONS = v,
                ConfigurationSection::getInt
        ); }

        public static int IMPALEMENT_PIN_CHECK_INTERVAL = 100;
        static { register(
                "combat.impalement_pin_check_interval",
                IMPALEMENT_PIN_CHECK_INTERVAL, Integer.class,
                v -> IMPALEMENT_PIN_CHECK_INTERVAL = v,
                ConfigurationSection::getInt
        ); }

        @SuppressWarnings("unchecked")
        static Class<List<String>> stringListClass() {
            return (Class<List<String>>) (Class<?>) List.class;
        }

        // Attack class configuration
        public static List<String> ATTACK_CLASS_EXEMPT_FROM_COMBAT = List.of(
            "ARMOR_STAND", "ITEM_FRAME", "GLOW_ITEM_FRAME", "PAINTING",
            "ITEM_DISPLAY", "BLOCK_DISPLAY", "TEXT_DISPLAY", "INTERACTION"
        );
        static { register(
                "combat.attack_class_exempt_from_combat",
                ATTACK_CLASS_EXEMPT_FROM_COMBAT, stringListClass(),
                v -> ATTACK_CLASS_EXEMPT_FROM_COMBAT = v,
                Config::loadStringList
        ); }

        // Attack class timing configuration
        public static int ATTACK_CLASS_TIMING_ATTACK_DURATION = 750;
        static { register(
                "combat.attack_class_timing_attack_duration",
                ATTACK_CLASS_TIMING_ATTACK_DURATION, Integer.class,
                v -> ATTACK_CLASS_TIMING_ATTACK_DURATION = v,
                ConfigurationSection::getInt
        ); }

        public static int ATTACK_CLASS_TIMING_ATTACK_ITERATIONS = 50;
        static { register(
                "combat.attack_class_timing_attack_iterations",
                ATTACK_CLASS_TIMING_ATTACK_ITERATIONS, Integer.class,
                v -> ATTACK_CLASS_TIMING_ATTACK_ITERATIONS = v,
                ConfigurationSection::getInt
        ); }

        public static double ATTACK_CLASS_TIMING_ATTACK_START_VALUE = 0.0; // progress 0-1
        static { register(
                "combat.attack_class_timing_attack_start_value",
                ATTACK_CLASS_TIMING_ATTACK_START_VALUE, Double.class,
                v -> ATTACK_CLASS_TIMING_ATTACK_START_VALUE = v,
                ConfigurationSection::getDouble
        ); }

        public static double ATTACK_CLASS_TIMING_ATTACK_END_VALUE = 1.0; // progress 0-1
        static { register(
                "combat.attack_class_timing_attack_end_value",
                ATTACK_CLASS_TIMING_ATTACK_END_VALUE, Double.class,
                v -> ATTACK_CLASS_TIMING_ATTACK_END_VALUE = v,
                ConfigurationSection::getDouble
        ); }

        // Attack class modifiers configuration
        public static double ATTACK_CLASS_MODIFIERS_RANGE_MULTIPLIER = 1.0;
        static { register(
                "combat.attack_class_modifiers_range_multiplier",
                ATTACK_CLASS_MODIFIERS_RANGE_MULTIPLIER, Double.class,
                v -> ATTACK_CLASS_MODIFIERS_RANGE_MULTIPLIER = v,
                ConfigurationSection::getDouble
        ); }

        public static int ATTACK_CLASS_HIT_INVULN_TICKS = 5;
        static { register(
            "combat.attack_class_hit_invuln_ticks",
            ATTACK_CLASS_HIT_INVULN_TICKS, Integer.class,
            v -> ATTACK_CLASS_HIT_INVULN_TICKS = v,
            ConfigurationSection::getInt
        ); }

        public static int ATTACK_CLASS_HIT_SHARDS = 1;
        static { register(
            "combat.attack_class_hit_shards",
            ATTACK_CLASS_HIT_SHARDS, Integer.class,
            v -> ATTACK_CLASS_HIT_SHARDS = v,
            ConfigurationSection::getInt
        ); }

        public static float ATTACK_CLASS_HIT_TOUGHNESS = 15;
        static { register(
            "combat.attack_class_hit_toughness",
            ATTACK_CLASS_HIT_TOUGHNESS, Float.class,
            v -> ATTACK_CLASS_HIT_TOUGHNESS = v,
            Config::loadFloat
        ); }

        public static float ATTACK_CLASS_HIT_SOULFIRE = 6;
        static { register(
            "combat.attack_class_hit_soulfire",
            ATTACK_CLASS_HIT_SOULFIRE, Float.class,
            v -> ATTACK_CLASS_HIT_SOULFIRE = v,
            Config::loadFloat
        ); }

        // Hit packets — default mob (mobs that damage the player)
        public static float HIT_DEFAULT_MOB_REAPED_SOULFIRE = 5f;
        static { register("combat.hit_default_mob_reaped_soulfire", HIT_DEFAULT_MOB_REAPED_SOULFIRE,
            Float.class, v -> HIT_DEFAULT_MOB_REAPED_SOULFIRE = v, Config::loadFloat); }

        public static int HIT_DEFAULT_MOB_INVULN_TICKS = 15;
        static { register("combat.hit_default_mob_invuln_ticks", HIT_DEFAULT_MOB_INVULN_TICKS,
            Integer.class, v -> HIT_DEFAULT_MOB_INVULN_TICKS = v, ConfigurationSection::getInt); }

        public static int HIT_DEFAULT_MOB_SHARD_DAMAGE = 1;
        static { register("combat.hit_default_mob_shard_damage", HIT_DEFAULT_MOB_SHARD_DAMAGE,
            Integer.class, v -> HIT_DEFAULT_MOB_SHARD_DAMAGE = v, ConfigurationSection::getInt); }

        public static float HIT_DEFAULT_MOB_TOUGHNESS_DAMAGE = 10f;
        static { register("combat.hit_default_mob_toughness_damage", HIT_DEFAULT_MOB_TOUGHNESS_DAMAGE,
            Float.class, v -> HIT_DEFAULT_MOB_TOUGHNESS_DAMAGE = v, Config::loadFloat); }

        public static float HIT_DEFAULT_MOB_SOULFIRE_LOSS = 1f;
        static { register("combat.hit_default_mob_soulfire_loss", HIT_DEFAULT_MOB_SOULFIRE_LOSS,
            Float.class, v -> HIT_DEFAULT_MOB_SOULFIRE_LOSS = v, Config::loadFloat); }

        // Hit packets — grab
        public static float HIT_GRAB_REAPED_SOULFIRE = 1f;
        static { register("combat.hit_grab_reaped_soulfire", HIT_GRAB_REAPED_SOULFIRE,
            Float.class, v -> HIT_GRAB_REAPED_SOULFIRE = v, Config::loadFloat); }

        public static int HIT_GRAB_INVULN_TICKS = 0;
        static { register("combat.hit_grab_invuln_ticks", HIT_GRAB_INVULN_TICKS,
            Integer.class, v -> HIT_GRAB_INVULN_TICKS = v, ConfigurationSection::getInt); }

        public static int HIT_GRAB_SHARD_DAMAGE = 0;
        static { register("combat.hit_grab_shard_damage", HIT_GRAB_SHARD_DAMAGE,
            Integer.class, v -> HIT_GRAB_SHARD_DAMAGE = v, ConfigurationSection::getInt); }

        public static float HIT_GRAB_TOUGHNESS_DAMAGE = 5f;
        static { register("combat.hit_grab_toughness_damage", HIT_GRAB_TOUGHNESS_DAMAGE,
            Float.class, v -> HIT_GRAB_TOUGHNESS_DAMAGE = v, Config::loadFloat); }

        public static float HIT_GRAB_SOULFIRE_LOSS = 5f;
        static { register("combat.hit_grab_soulfire_loss", HIT_GRAB_SOULFIRE_LOSS,
            Float.class, v -> HIT_GRAB_SOULFIRE_LOSS = v, Config::loadFloat); }

        // Hit packets — umbral item display attack
        public static float HIT_UMBRAL_DISPLAY_REAPED_SOULFIRE = 0f;
        static { register("combat.hit_umbral_display_reaped_soulfire", HIT_UMBRAL_DISPLAY_REAPED_SOULFIRE,
            Float.class, v -> HIT_UMBRAL_DISPLAY_REAPED_SOULFIRE = v, Config::loadFloat); }

        public static int HIT_UMBRAL_DISPLAY_INVULN_TICKS = 5;
        static { register("combat.hit_umbral_display_invuln_ticks", HIT_UMBRAL_DISPLAY_INVULN_TICKS,
            Integer.class, v -> HIT_UMBRAL_DISPLAY_INVULN_TICKS = v, ConfigurationSection::getInt); }

        public static int HIT_UMBRAL_DISPLAY_SHARD_DAMAGE = 1;
        static { register("combat.hit_umbral_display_shard_damage", HIT_UMBRAL_DISPLAY_SHARD_DAMAGE,
            Integer.class, v -> HIT_UMBRAL_DISPLAY_SHARD_DAMAGE = v, ConfigurationSection::getInt); }

        public static float HIT_UMBRAL_DISPLAY_TOUGHNESS_DAMAGE = 15f;
        static { register("combat.hit_umbral_display_toughness_damage", HIT_UMBRAL_DISPLAY_TOUGHNESS_DAMAGE,
            Float.class, v -> HIT_UMBRAL_DISPLAY_TOUGHNESS_DAMAGE = v, Config::loadFloat); }

        public static float HIT_UMBRAL_DISPLAY_SOULFIRE_LOSS = 10f;
        static { register("combat.hit_umbral_display_soulfire_loss", HIT_UMBRAL_DISPLAY_SOULFIRE_LOSS,
            Float.class, v -> HIT_UMBRAL_DISPLAY_SOULFIRE_LOSS = v, Config::loadFloat); }

        // Hit packets — punch
        public static float HIT_PUNCH_REAPED_SOULFIRE = 7.5f;
        static { register("combat.hit_punch_reaped_soulfire", HIT_PUNCH_REAPED_SOULFIRE,
            Float.class, v -> HIT_PUNCH_REAPED_SOULFIRE = v, Config::loadFloat); }

        public static int HIT_PUNCH_INVULN_TICKS = 2;
        static { register("combat.hit_punch_invuln_ticks", HIT_PUNCH_INVULN_TICKS,
            Integer.class, v -> HIT_PUNCH_INVULN_TICKS = v, ConfigurationSection::getInt); }

        public static int HIT_PUNCH_SHARD_DAMAGE = 1;
        static { register("combat.hit_punch_shard_damage", HIT_PUNCH_SHARD_DAMAGE,
            Integer.class, v -> HIT_PUNCH_SHARD_DAMAGE = v, ConfigurationSection::getInt); }

        public static float HIT_PUNCH_TOUGHNESS_DAMAGE = 5f;
        static { register("combat.hit_punch_toughness_damage", HIT_PUNCH_TOUGHNESS_DAMAGE,
            Float.class, v -> HIT_PUNCH_TOUGHNESS_DAMAGE = v, Config::loadFloat); }

        public static float HIT_PUNCH_SOULFIRE_LOSS = 5f;
        static { register("combat.hit_punch_soulfire_loss", HIT_PUNCH_SOULFIRE_LOSS,
            Float.class, v -> HIT_PUNCH_SOULFIRE_LOSS = v, Config::loadFloat); }

        // Block & parry configuration
        public static float BLOCK_SOULFIRE_DRAIN_PER_SECOND = 2.5f;
        static { register("combat.block_soulfire_drain_per_second", BLOCK_SOULFIRE_DRAIN_PER_SECOND,
            Float.class, v -> BLOCK_SOULFIRE_DRAIN_PER_SECOND = v, Config::loadFloat); }

        public static float BLOCK_SOULFIRE_COST_ON_HIT = 20.0f;
        static { register("combat.block_soulfire_cost_on_hit", BLOCK_SOULFIRE_COST_ON_HIT,
            Float.class, v -> BLOCK_SOULFIRE_COST_ON_HIT = v, Config::loadFloat); }

        public static int BLOCK_BREAK_STAGGER_MS = 1000;
        static { register("combat.block_break_stagger_ms", BLOCK_BREAK_STAGGER_MS,
            Integer.class, v -> BLOCK_BREAK_STAGGER_MS = v, ConfigurationSection::getInt); }

        public static int PARRY_AVAILABLE_MS = 400;
        static { register("combat.parry_available_ms", PARRY_AVAILABLE_MS,
            Integer.class, v -> PARRY_AVAILABLE_MS = v, ConfigurationSection::getInt); }

        public static int PARRY_WINDOW_MS = 200;
        static { register("combat.parry_window_ms", PARRY_WINDOW_MS,
            Integer.class, v -> PARRY_WINDOW_MS = v, ConfigurationSection::getInt); }

        public static float PARRY_SOULFIRE_GAIN = 25.0f;
        static { register("combat.parry_soulfire_gain", PARRY_SOULFIRE_GAIN,
            Float.class, v -> PARRY_SOULFIRE_GAIN = v, Config::loadFloat); }

        public static int PARRY_STAGGER_MS = 1000;
        static { register("combat.parry_stagger_ms", PARRY_STAGGER_MS,
            Integer.class, v -> PARRY_STAGGER_MS = v, ConfigurationSection::getInt); }

        /** Ticks the shield is put on cooldown after a successful parry (prevents re-raising immediately). */
        public static int PARRY_SHIELD_COOLDOWN_TICKS = 25;
        static { register("combat.parry_shield_cooldown_ticks", PARRY_SHIELD_COOLDOWN_TICKS,
            Integer.class, v -> PARRY_SHIELD_COOLDOWN_TICKS = v, ConfigurationSection::getInt); }

        public static int EXHAUSTED_BLOCKING_COOLDOWN_TICKS = 30;
        static { register("combat.exhausted_blocking_cooldown_ticks", EXHAUSTED_BLOCKING_COOLDOWN_TICKS,
            Integer.class, v -> EXHAUSTED_BLOCKING_COOLDOWN_TICKS = v, ConfigurationSection::getInt); }

        public static float SHIELD_PASSING_BYPASS_POWER = 0.5f;
        static { register("combat.shield_passing_bypass_power", SHIELD_PASSING_BYPASS_POWER,
            Float.class, v -> SHIELD_PASSING_BYPASS_POWER = v, Config::loadFloat); }

        /** Soulfire cost per tick while channeling. */
        public static double CHANNEL_SOULFIRE_COST = 50.0;
        static { register(
            "combat.channel_soulfire_cost",
            CHANNEL_SOULFIRE_COST, Double.class,
            v -> CHANNEL_SOULFIRE_COST = v,
            ConfigurationSection::getDouble
        ); }

        /** Health restored per tick while channeling. */
        public static int CHANNEL_HEAL_AMOUNT = 1;
        static { register(
            "combat.channel_heal_amount",
            CHANNEL_HEAL_AMOUNT, Integer.class,
            v -> CHANNEL_HEAL_AMOUNT = v,
            ConfigurationSection::getInt
        ); }

        /** Duration in milliseconds for the channel ability. */
        public static long CHANNEL_DURATION_MS = 2000;
        static { register(
            "combat.channel_duration_ms",
            CHANNEL_DURATION_MS, Long.class,
            v -> CHANNEL_DURATION_MS = v,
            (section, path, def) -> section.getLong(path, def)
        ); }

        /** Period in milliseconds between each heal tick during channel. */
        public static int CHANNEL_HEAL_PERIOD = 50;
        static { register(
            "combat.channel_heal_period",
            CHANNEL_HEAL_PERIOD, Integer.class,
            v -> CHANNEL_HEAL_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        /** Slowness potion duration (ticks) applied during heal channel. */
        public static int HEAL_CHANNEL_SLOW_DURATION = 1;
        static { register(
            "combat.heal_channel_slow_duration",
            HEAL_CHANNEL_SLOW_DURATION, Integer.class,
            v -> HEAL_CHANNEL_SLOW_DURATION = v,
            ConfigurationSection::getInt
        ); }

        /** Slowness potion amplifier (0-based level) applied during heal channel. */
        public static int HEAL_CHANNEL_SLOW_AMPLIFIER = 4;
        static { register(
            "combat.heal_channel_slow_amplifier",
            HEAL_CHANNEL_SLOW_AMPLIFIER, Integer.class,
            v -> HEAL_CHANNEL_SLOW_AMPLIFIER = v,
            ConfigurationSection::getInt
        ); }

        /** Duration in milliseconds for the circular reclaim slash attack. */
        public static int CIRCULAR_SLASH_DURATION_MS = 300;
        static { register(
            "combat.circular-slash-duration-ms",
            CIRCULAR_SLASH_DURATION_MS, Integer.class,
            v -> CIRCULAR_SLASH_DURATION_MS = v,
            ConfigurationSection::getInt
        ); }

        /** Number of iterations for the circular reclaim slash attack. */
        public static int CIRCULAR_SLASH_ITERATIONS = 200;
        static { register(
            "combat.circular-slash-iterations",
            CIRCULAR_SLASH_ITERATIONS, Integer.class,
            v -> CIRCULAR_SLASH_ITERATIONS = v,
            ConfigurationSection::getInt
        ); }

        /** Soulfire cost for triggering a basic link attack. */
        public static double LINK_ATTACK_SOULFIRE_COST = 2.5;
        static { register(
            "combat.link-attack-soulfire-cost",
            LINK_ATTACK_SOULFIRE_COST, Double.class,
            v -> LINK_ATTACK_SOULFIRE_COST = v,
            ConfigurationSection::getDouble
        ); }

        /** Number of steps in basic combo. */
        public static int BASIC_COMBO_STEPS = 3;
        static { register(
            "combat.basic-combo-steps",
            BASIC_COMBO_STEPS, Integer.class,
            v -> BASIC_COMBO_STEPS = v,
            ConfigurationSection::getInt
        ); }

        /** Particle count for ground hit effects. */
        public static int GROUND_HIT_PARTICLE_COUNT = 5;
        static { register(
            "combat.ground-hit-particle-count",
            GROUND_HIT_PARTICLE_COUNT, Integer.class,
            v -> GROUND_HIT_PARTICLE_COUNT = v,
            ConfigurationSection::getInt
        ); }

        /** Particle offset for ground hit effects. */
        public static double GROUND_HIT_PARTICLE_OFFSET = 0.5;
        static { register(
            "combat.ground-hit-particle-offset",
            GROUND_HIT_PARTICLE_OFFSET, Double.class,
            v -> GROUND_HIT_PARTICLE_OFFSET = v,
            ConfigurationSection::getDouble
        ); }
    }
    //endregion

    // ==============================================================================
    //region TIMING - Cooldowns, durations, intervals
    // ==============================================================================
    /**
     * Timing configuration for cooldowns, durations, and update intervals.
     * <p>
     * Controls tick-based timing for thrown items, entity updates, combat cleanup,
     * and combo windows. All values in <b>ticks</b> (20 ticks = 1 second).
     * </p>
     *
     * <h2>Common Timing Patterns</h2>
     * <ul>
     *   <li><b>Grace Periods</b> - Short windows for forgiving input timing</li>
     *   <li><b>Disposal Timeouts</b> - Entity cleanup after inactivity</li>
     *   <li><b>Update Intervals</b> - Frequency of background tasks</li>
     * </ul>
     *
     * @see ThrownItem Thrown item lifecycle
     */
    public static class Timing {
        // Thrown items configuration
        public static int THROWN_ITEMS_CATCH_GRACE_PERIOD = 3;
        static { register(
            "timing.thrown_items_catch_grace_period",
            THROWN_ITEMS_CATCH_GRACE_PERIOD, Integer.class,
            v -> THROWN_ITEMS_CATCH_GRACE_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        public static int THROWN_ITEMS_DISPOSAL_TIMEOUT = 30000; // 30 seconds
        static { register(
            "timing.thrown_items_disposal_timeout",
            THROWN_ITEMS_DISPOSAL_TIMEOUT, Integer.class,
            v -> THROWN_ITEMS_DISPOSAL_TIMEOUT = v,
            ConfigurationSection::getInt
        ); }

        public static int THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL = 500;
        static { register(
            "timing.thrown_items_disposal_check_interval",
            THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL, Integer.class,
            v -> THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL = v,
            ConfigurationSection::getInt
        ); }

        public static int THROWN_ITEMS_PIN_DELAY = 2;
        static { register(
            "timing.thrown_items_pin_delay",
            THROWN_ITEMS_PIN_DELAY, Integer.class,
            v -> THROWN_ITEMS_PIN_DELAY = v,
            ConfigurationSection::getInt
        ); }

        public static int THROWN_ITEMS_THROW_COMPLETION_DELAY = 6;
        static { register(
            "timing.thrown_items_throw_completion_delay",
            THROWN_ITEMS_THROW_COMPLETION_DELAY, Integer.class,
            v -> THROWN_ITEMS_THROW_COMPLETION_DELAY = v,
            ConfigurationSection::getInt
        ); }

        // Intervals configuration
        public static int INTERVALS_ENTITY_TICK = 1;
        static { register(
            "timing.intervals_entity_tick",
            INTERVALS_ENTITY_TICK, Integer.class,
            v -> INTERVALS_ENTITY_TICK = v,
            ConfigurationSection::getInt
        ); }

        public static int INTERVALS_STATUS_DISPLAY_UPDATE = 5;
        static { register(
            "timing.intervals_status_display_update",
            INTERVALS_STATUS_DISPLAY_UPDATE, Integer.class,
            v -> INTERVALS_STATUS_DISPLAY_UPDATE = v,
            ConfigurationSection::getInt
        ); }

        public static int INTERVALS_COMBAT_CLEANUP = 20;
        static { register(
            "timing.intervals_combat_cleanup",
            INTERVALS_COMBAT_CLEANUP, Integer.class,
            v -> INTERVALS_COMBAT_CLEANUP = v,
            ConfigurationSection::getInt
        ); }

        // Attacks configuration
        public static int ATTACKS_COMBO_WINDOW_BASE = 3;
        static { register(
            "timing.attacks_combo_window_base",
            ATTACKS_COMBO_WINDOW_BASE, Integer.class,
            v -> ATTACKS_COMBO_WINDOW_BASE = v,
            ConfigurationSection::getInt
        ); }

        /** Delay in milliseconds between right-click inputs. */
        public static int RIGHT_INTERACT_DELAY = 1;
        static { register(
            "timing.right_interact_delay",
            RIGHT_INTERACT_DELAY, Integer.class,
            v -> RIGHT_INTERACT_DELAY = v,
            ConfigurationSection::getInt
        ); }
    }
    //endregion

    // ==============================================================================
    //region DISPLAY - Visual elements, particles, effects
    // ==============================================================================
    /**
     * Visual display configuration for particles, status indicators, and effects.
     * <p>
     * Controls particle effects, status display positioning, item display behavior,
     * and billboard modes. Distances in <b>blocks</b>, intervals in <b>ticks</b>,
     * brightness 0-15 (Minecraft light level).
     * </p>
     *
     * <h2>Key Subsystems</h2>
     * <ul>
     *   <li><b>Status Display</b> - Overhead health/stats text displays</li>
     *   <li><b>Item Display</b> - Floating item entities, billboard modes</li>
     *   <li><b>Particles</b> - Global particle toggles and density</li>
     * </ul>
     *
     * @see org.bukkit.entity.Display.Billboard Billboard rotation modes
     */
    public static class Display {
        public static int DEFAULT_TELEPORT_DURATION = 2;
        static { register(
            "display.default_teleport_duration",
            DEFAULT_TELEPORT_DURATION, Integer.class,
            v -> DEFAULT_TELEPORT_DURATION = v,
            ConfigurationSection::getInt
        ); }

        // Status display configuration
        public static boolean STATUS_DISPLAY_ENABLED = true;
        static { register(
            "display.status_display_enabled",
            STATUS_DISPLAY_ENABLED, Boolean.class,
            v -> STATUS_DISPLAY_ENABLED = v,
            ConfigurationSection::getBoolean
        ); }

        public static double STATUS_DISPLAY_HEIGHT_OFFSET = 2.0;
        static { register(
            "display.status_display_height_offset",
            STATUS_DISPLAY_HEIGHT_OFFSET, Double.class,
            v -> STATUS_DISPLAY_HEIGHT_OFFSET = v,
            ConfigurationSection::getDouble
        ); }

        public static int STATUS_DISPLAY_UPDATE_INTERVAL = 5;
        static { register(
            "display.status_display_update_interval",
            STATUS_DISPLAY_UPDATE_INTERVAL, Integer.class,
            v -> STATUS_DISPLAY_UPDATE_INTERVAL = v,
            ConfigurationSection::getInt
        ); }

        public static int STATUS_DISPLAY_BLOCK_BRIGHTNESS = 15; // 0-15 (light level)
        static { register(
            "display.status_display_block_brightness",
            STATUS_DISPLAY_BLOCK_BRIGHTNESS, Integer.class,
            v -> STATUS_DISPLAY_BLOCK_BRIGHTNESS = v,
            ConfigurationSection::getInt
        ); }

        public static int STATUS_DISPLAY_SKY_BRIGHTNESS = 15; // 0-15 (light level)
        static { register(
            "display.status_display_sky_brightness",
            STATUS_DISPLAY_SKY_BRIGHTNESS, Integer.class,
            v -> STATUS_DISPLAY_SKY_BRIGHTNESS = v,
            ConfigurationSection::getInt
        ); }

        // Item display follow configuration
        public static int ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL = 100;
        static { register(
            "display.item_display_follow_update_interval",
            ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL, Integer.class,
            v -> ITEM_DISPLAY_FOLLOW_UPDATE_INTERVAL = v,
            ConfigurationSection::getInt
        ); }

        public static int ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL = 4;
        static { register(
            "display.item_display_follow_particle_interval",
            ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL, Integer.class,
            v -> ITEM_DISPLAY_FOLLOW_PARTICLE_INTERVAL = v,
            ConfigurationSection::getInt
        ); }

        public static Billboard ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE = Billboard.FIXED;
        static { register(
            "display.item_display_follow_billboard_mode",
            ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE, Billboard.class,
            v -> ITEM_DISPLAY_FOLLOW_BILLBOARD_MODE = v,
            (s, p, d) -> loadEnum(s, p, d, Billboard.class)
        ); }

        /** Number of display steps per attack animation. */
        public static int ATTACK_DISPLAY_STEPS = 10;
        static { register(
            "display.attack_display_steps",
            ATTACK_DISPLAY_STEPS, Integer.class,
            v -> ATTACK_DISPLAY_STEPS = v,
            ConfigurationSection::getInt
        ); }

        // Particles configuration
        public static boolean PARTICLES_ENABLED = true;
        static { register(
            "display.particles_enabled",
            PARTICLES_ENABLED, Boolean.class,
            v -> PARTICLES_ENABLED = v,
            ConfigurationSection::getBoolean
        ); }

        public static int PARTICLES_DENSITY = 10;
        static { register(
            "display.particles_density",
            PARTICLES_DENSITY, Integer.class,
            v -> PARTICLES_DENSITY = v,
            ConfigurationSection::getInt
        ); }
    }
    //endregion

    // ==============================================================================
    //region DETECTION - Hitboxes, range detection, raytracing
    // ==============================================================================
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
    public static class Detection {
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
            "detection.throw_pin_ray_size",
            THROW_PIN_RAY_DISTANCE, Double.class,
            v -> THROW_PIN_RAY_DISTANCE = v,
            ConfigurationSection::getDouble
        ); }

        public static double THROW_GROUND_CHECK_MULTIPLIER = 0.1;
        static { register(
            "detection.throw_pin_ray_size",
            THROW_GROUND_CHECK_MULTIPLIER, Double.class,
            v -> THROW_GROUND_CHECK_MULTIPLIER = v,
            ConfigurationSection::getDouble
        ); }

        public static double THROW_HIT_CHECK_DIST_MULTIPLIER = 0.6;
        static { register(
            "detection.throw_pin_ray_size",
            THROW_HIT_CHECK_DIST_MULTIPLIER, Double.class,
            v -> THROW_HIT_CHECK_DIST_MULTIPLIER = v,
            ConfigurationSection::getDouble
        ); }

        public static double THROW_HIT_CHECK_RAY_SIZE = 1.0;
        static { register(
            "detection.throw_pin_ray_size",
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
    //endregion

    // ==============================================================================
    //region AUDIO - Sound effects and audio feedback
    // ==============================================================================
    public static class Audio {
        // Sounds configuration
        public static boolean SOUNDS_ENABLED = true;
        static { register(
            "audio.sounds_enabled",
            SOUNDS_ENABLED, Boolean.class,
            v -> SOUNDS_ENABLED = v,
            ConfigurationSection::getBoolean
        ); }

        public static float SOUNDS_GLOBAL_VOLUME = 1.0f; // 0.0-1.0
        static { register(
            "audio.sounds_global_volume",
            SOUNDS_GLOBAL_VOLUME, Float.class,
            v -> SOUNDS_GLOBAL_VOLUME = v,
            Config::loadFloat
        ); }

        public static float SOUNDS_GLOBAL_PITCH = 1.0f; // 0.5-2.0
        static { register(
            "audio.sounds_global_pitch",
            SOUNDS_GLOBAL_PITCH, Float.class,
            v -> SOUNDS_GLOBAL_PITCH = v,
            Config::loadFloat
        ); }

        public static SoundType BLOCK_BROKEN_SOUND = SoundType.ITEM_SHIELD_BREAK;
        static { register(
            "audio.block_broken_sound",
            BLOCK_BROKEN_SOUND, SoundType.class,
            v -> BLOCK_BROKEN_SOUND = v,
            Config::loadSoundType
        ); }

        public static float BLOCK_BROKEN_VOLUME = 1.0f; // 0.0-1.0
        static { register(
            "audio.block_broken_volume",
            BLOCK_BROKEN_VOLUME, Float.class,
            v -> BLOCK_BROKEN_VOLUME = v,
            Config::loadFloat
        ); }

        public static float BLOCK_BROKEN_PITCH = 1.0f;
        static { register(
            "audio.block_broken_pitch",
            BLOCK_BROKEN_PITCH, Float.class,
            v -> BLOCK_BROKEN_PITCH = v,
            Config::loadFloat
        ); }

        public static SoundType PARRY_ATTEMPT_SOUND = SoundType.RANDOM_BANE_SLASH;
        static { register(
            "audio.parry_attempt_sound",
            PARRY_ATTEMPT_SOUND, SoundType.class,
            v -> PARRY_ATTEMPT_SOUND = v,
            Config::loadSoundType
        ); }

        public static float PARRY_ATTEMPT_VOLUME = 1.0f; // 0.0-1.0
        static { register(
            "audio.parry_sound_volume",
            PARRY_ATTEMPT_VOLUME, Float.class,
            v -> PARRY_ATTEMPT_VOLUME = v,
            Config::loadFloat
        ); }

        public static float PARRY_ATTEMPT_PITCH = 1.0f;
        static { register(
            "audio.parry_attempt_pitch",
            PARRY_ATTEMPT_PITCH, Float.class,
            v -> PARRY_ATTEMPT_PITCH = v,
            Config::loadFloat
        ); }

        public static SoundType PRE_ATTACK_SOUND = SoundType.ENTITY_EVOKER_FANGS_ATTACK;
        static { register(
            "audio.pre_attack_sound",
            PRE_ATTACK_SOUND, SoundType.class,
            v -> PRE_ATTACK_SOUND = v,
            Config::loadSoundType
        ); }

        public static float PRE_ATTACK_VOLUME = 2.0f; // 0.0-1.0
        static { register(
            "audio.pre_attack_volume",
            PRE_ATTACK_VOLUME, Float.class,
            v -> THROW_VOLUME = v,
            Config::loadFloat
        ); }

        public static float PRE_ATTACK_PITCH = 1.0f;
        static { register(
            "audio.pre_attack_pitch",
            PRE_ATTACK_PITCH, Float.class,
            v -> THROW_PITCH = v,
            Config::loadFloat
        ); }


        // Throw sound configuration
        public static SoundType THROW_SOUND = SoundType.ENTITY_ENDER_DRAGON_FLAP;
        static { register(
            "audio.throw_sound",
            THROW_SOUND, SoundType.class,
            v -> THROW_SOUND = v,
            Config::loadSoundType
        ); }

        public static float THROW_VOLUME = 0.35f; // 0.0-1.0
        static { register(
            "audio.throw_volume",
            THROW_VOLUME, Float.class,
            v -> THROW_VOLUME = v,
            Config::loadFloat
        ); }

        public static float THROW_PITCH = 0.4f; // 0.5-2.0
        static { register(
            "audio.throw_pitch",
            THROW_PITCH, Float.class,
            v -> THROW_PITCH = v,
            Config::loadFloat
        ); }

        // Attack sound configuration
        public static SoundType ATTACK_SOUND = SoundType.ITEM_TRIDENT_THROW;
        static { register(
            "audio.attack_sound",
            ATTACK_SOUND, SoundType.class,
            v -> ATTACK_SOUND = v,
            Config::loadSoundType
        ); }

        public static float ATTACK_VOLUME = 0.6f; // 0.0-1.0
        static { register(
            "audio.attack_volume",
            ATTACK_VOLUME, Float.class,
            v -> ATTACK_VOLUME = v,
            Config::loadFloat
        ); }

        public static float ATTACK_PITCH = 0.7f; // 0.5-2.0
        static { register(
            "audio.attack_pitch",
            ATTACK_PITCH, Float.class,
            v -> ATTACK_PITCH = v,
            Config::loadFloat
        ); }

        public static float ENTITY_HIT_CONNECT_VOLUME = 0.9f;
        static { register(
            "audio.entity_hit_connect",
            ENTITY_HIT_CONNECT_VOLUME, Float.class,
            v -> ENTITY_HIT_CONNECT_VOLUME = v,
            Config::loadFloat
        ); }

        public static float ENTITY_HIT_CONNECT_PITCH = 1.0f;
        static { register(
            "audio.entity_hit_connect",
            ENTITY_HIT_CONNECT_PITCH, Float.class,
            v -> ENTITY_HIT_CONNECT_PITCH = v,
            Config::loadFloat
        ); }

        public static SoundType PUNCH_ATTEMPT = SoundType.ENTITY_PLAYER_ATTACK_SWEEP;
        static { register(
            "audio.punch_attempt",
            PUNCH_ATTEMPT, SoundType.class,
            v -> PUNCH_ATTEMPT = v,
            Config::loadSoundType
        ); }

        public static float PUNCH_ATTEMPT_VOL = 1.5f;
        static { register(
            "audio.punch_attempt_vol",
            PUNCH_ATTEMPT_VOL, Float.class,
            v -> PUNCH_ATTEMPT_VOL = v,
            Config::loadFloat
        ); }

        public static float PUNCH_ATTEMPT_PITCH = 0.5f;
        static { register(
            "audio.punch_attempt_pitch",
            PUNCH_ATTEMPT_PITCH, Float.class,
            v -> PUNCH_ATTEMPT_PITCH = v,
            Config::loadFloat
        ); }

        public static SoundType PUNCH_CONNECT = SoundType.ENTITY_PLAYER_ATTACK_KNOCKBACK;
        static { register(
            "audio.punch_connect",
            PUNCH_CONNECT, SoundType.class,
            v -> PUNCH_CONNECT = v,
            Config::loadSoundType
        ); }

        public static float PUNCH_CONNECT_VOL = 0.9f;
        static { register(
            "audio.punch_connect_vol",
            PUNCH_CONNECT_VOL, Float.class,
            v -> PUNCH_CONNECT_VOL = v,
            Config::loadFloat
        ); }

        public static float PUNCH_CONNECT_PITCH = 1.0f;
        static { register(
            "audio.punch_connect_pitch",
            PUNCH_CONNECT_PITCH, Float.class,
            v -> PUNCH_CONNECT_PITCH = v,
            Config::loadFloat
        ); }
    }
    //endregion

    // ==============================================================================
    //region ENTITY - Entity stats, health, aspects
    // ==============================================================================
    /**
     * Entity attribute configuration for players, hostiles, and combat profiles.
     * <p>
     * Defines base stats (health/toughness/soulfire), regeneration rates, and
     * hostile entity multipliers. Health in <b>HP</b> (1 heart = 2 HP), time in
     * <b>ticks</b> (20 ticks/second).
     * </p>
     *
     * <h2>Combat Profile Aspects</h2>
     * <ul>
     *   <li><b>Shards</b> - Defensive resource depleted by attacks</li>
     *   <li><b>Toughness</b> - Damage reduction layer</li>
     *   <li><b>Soulfire</b> - Special ability resource</li>
     *   <li><b>Form</b> - Combat stance/technique points</li>
     * </ul>
     *
     * @see btm.sword.system.entity.base.CombatProfile Combat stat management
     * @see btm.sword.system.entity.base.SwordEntity Entity wrapper
     */
    public static class Entity {
        // Player configuration
        public static double PLAYER_BASE_HEALTH = 100.0; // HP (1 heart = 2 HP)
        static { register(
            "entity.player_base_health",
            PLAYER_BASE_HEALTH, Double.class,
            v -> PLAYER_BASE_HEALTH = v,
            ConfigurationSection::getDouble
        ); }

        public static double PLAYER_BASE_TOUGHNESS = 100.0; // HP
        static { register(
            "entity.player_base_toughness",
            PLAYER_BASE_TOUGHNESS, Double.class,
            v -> PLAYER_BASE_TOUGHNESS = v,
            ConfigurationSection::getDouble
        ); }

        public static double PLAYER_BASE_SOULFIRE = 100.0; // points
        static { register(
            "entity.player_base_soulfire",
            PLAYER_BASE_SOULFIRE, Double.class,
            v -> PLAYER_BASE_SOULFIRE = v,
            ConfigurationSection::getDouble
        ); }

        // Hostile configuration
        public static double HOSTILE_HEALTH_MULTIPLIER = 1.0;
        static { register(
            "entity.hostile_health_multiplier",
            HOSTILE_HEALTH_MULTIPLIER, Double.class,
            v -> HOSTILE_HEALTH_MULTIPLIER = v,
            ConfigurationSection::getDouble
        ); }

        public static double HOSTILE_DAMAGE_MULTIPLIER = 1.0;
        static { register(
            "entity.hostile_damage_multiplier",
            HOSTILE_DAMAGE_MULTIPLIER, Double.class,
            v -> HOSTILE_DAMAGE_MULTIPLIER = v,
            ConfigurationSection::getDouble
        ); }

        // Combat profile configuration
        public static int COMBAT_PROFILE_MAX_AIR_DODGES = 1;
        static { register(
            "entity.combat_profile_max_air_dodges",
            COMBAT_PROFILE_MAX_AIR_DODGES, Integer.class,
            v -> COMBAT_PROFILE_MAX_AIR_DODGES = v,
            ConfigurationSection::getInt
        ); }

        // Combat profile shards configuration
        public static int COMBAT_PROFILE_SHARDS_CURRENT = 5;
        static { register(
            "entity.combat_profile_shards_current",
            COMBAT_PROFILE_SHARDS_CURRENT, Integer.class,
            v -> COMBAT_PROFILE_SHARDS_CURRENT = v,
            ConfigurationSection::getInt
        ); }

        public static int COMBAT_PROFILE_SHARDS_REGEN_PERIOD = 10000;
        static { register(
            "entity.combat_profile_shards_regen_period",
            COMBAT_PROFILE_SHARDS_REGEN_PERIOD, Integer.class,
            v -> COMBAT_PROFILE_SHARDS_REGEN_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        public static int COMBAT_PROFILE_SHARDS_REGEN_AMOUNT = 1;
        static { register(
            "entity.combat_profile_shards_regen_amount",
            COMBAT_PROFILE_SHARDS_REGEN_AMOUNT, Integer.class,
            v -> COMBAT_PROFILE_SHARDS_REGEN_AMOUNT = v,
            ConfigurationSection::getInt
        ); }

        // Combat profile toughness configuration
        public static float COMBAT_PROFILE_TOUGHNESS_CURRENT = 20.0f;
        static { register(
            "entity.combat_profile_toughness_current",
            COMBAT_PROFILE_TOUGHNESS_CURRENT, Float.class,
            v -> COMBAT_PROFILE_TOUGHNESS_CURRENT = v,
            Config::loadFloat
        ); }

        public static int COMBAT_PROFILE_TOUGHNESS_REGEN_PERIOD = 1000;
        static { register(
            "entity.combat_profile_toughness_regen_period",
            COMBAT_PROFILE_TOUGHNESS_REGEN_PERIOD, Integer.class,
            v -> COMBAT_PROFILE_TOUGHNESS_REGEN_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        public static float COMBAT_PROFILE_TOUGHNESS_REGEN_AMOUNT = 0.5f;
        static { register(
            "entity.combat_profile_toughness_regen_amount",
            COMBAT_PROFILE_TOUGHNESS_REGEN_AMOUNT, Float.class,
            v -> COMBAT_PROFILE_TOUGHNESS_REGEN_AMOUNT = v,
            Config::loadFloat
        ); }

        // Combat profile soulfire configuration
        public static float COMBAT_PROFILE_SOULFIRE_CURRENT = 100.0f;
        static { register(
            "entity.combat_profile_soulfire_current",
            COMBAT_PROFILE_SOULFIRE_CURRENT, Float.class,
            v -> COMBAT_PROFILE_SOULFIRE_CURRENT = v,
            Config::loadFloat
        ); }

        public static int COMBAT_PROFILE_SOULFIRE_REGEN_PERIOD = 250;
        static { register(
            "entity.combat_profile_soulfire_regen_period",
            COMBAT_PROFILE_SOULFIRE_REGEN_PERIOD, Integer.class,
            v -> COMBAT_PROFILE_SOULFIRE_REGEN_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        public static float COMBAT_PROFILE_SOULFIRE_REGEN_AMOUNT = 0.2f;
        static { register(
            "entity.combat_profile_soulfire_regen_amount",
            COMBAT_PROFILE_SOULFIRE_REGEN_AMOUNT, Float.class,
            v -> COMBAT_PROFILE_SOULFIRE_REGEN_AMOUNT = v,
            Config::loadFloat
        ); }

        // Combat profile form configuration
        public static float COMBAT_PROFILE_FORM_CURRENT = 10.0f;
        static { register(
            "entity.combat_profile_form_current",
            COMBAT_PROFILE_FORM_CURRENT, Float.class,
            v -> COMBAT_PROFILE_FORM_CURRENT = v,
            Config::loadFloat
        ); }

        public static int COMBAT_PROFILE_FORM_REGEN_PERIOD = 3000;
        static { register(
            "entity.combat_profile_form_regen_period",
            COMBAT_PROFILE_FORM_REGEN_PERIOD, Integer.class,
            v -> COMBAT_PROFILE_FORM_REGEN_PERIOD = v,
            ConfigurationSection::getInt
        ); }

        public static float COMBAT_PROFILE_FORM_REGEN_AMOUNT = 1.0f;
        static { register(
            "entity.combat_profile_form_regen_amount",
            COMBAT_PROFILE_FORM_REGEN_AMOUNT, Float.class,
            v -> COMBAT_PROFILE_FORM_REGEN_AMOUNT = v,
            Config::loadFloat
        ); }

        public static float HIT_TOUGH_BREAK_RECHARGE_AMOUNT_PERCENT = 2.0f;
        static { register(
            "entity.hit_tough_break_recharge_amount_percent",
            HIT_TOUGH_BREAK_RECHARGE_AMOUNT_PERCENT, Float.class,
            v -> HIT_TOUGH_BREAK_RECHARGE_AMOUNT_PERCENT = v,
            Config::loadFloat
        ); }

        public static float HIT_TOUGH_BREAK_RECHARGE_PERIOD_PERCENT = 0.2f;
        static { register(
            "entity.hit_tough_break_recharge_period_percent",
            HIT_TOUGH_BREAK_RECHARGE_PERIOD_PERCENT, Float.class,
            v -> HIT_TOUGH_BREAK_RECHARGE_PERIOD_PERCENT = v,
            Config::loadFloat
        ); }

        public static float HIT_TOUGH_BREAK_RECHARGE_CUTOFF_PERCENT = 0.6f;
        static { register(
            "entity.hit_tough_break_recharge_cutoff_percent",
            HIT_TOUGH_BREAK_RECHARGE_CUTOFF_PERCENT, Float.class,
            v -> HIT_TOUGH_BREAK_RECHARGE_CUTOFF_PERCENT = v,
            Config::loadFloat
        ); }
    }
    //endregion

    // ==============================================================================
    //region MOVEMENT - Dash, grab, mobility abilities
    // ==============================================================================
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
    public static class Movement {
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
        public static int FLAT_DASH_HEIGHT_BOOST_DELAY_MS = 100;
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
    //endregion

    // ==============================================================================
    //region WORLD - World interaction, block breaking, environment
    // ==============================================================================
    /**
     * World interaction configuration for block breaking and explosions.
     * <p>
     * Controls whether combat actions can modify the environment, including block
     * breaking permissions and explosion behavior (fire/block damage).
     * </p>
     *
     * <h2>Protection Integration</h2>
     * <ul>
     *   <li><b>WorldGuard</b> - Respects region protection flags when enabled</li>
     *   <li><b>Block Breaking</b> - Master toggle for all block modifications</li>
     *   <li><b>Explosions</b> - Separate controls for fire and block damage</li>
     * </ul>
     */
    public static class World {
        // Block interaction configuration
        public static boolean BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING = false;
        static { register(
            "world.block_interaction_allow_block_breaking",
            BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING, Boolean.class,
            v -> BLOCK_INTERACTION_ALLOW_BLOCK_BREAKING = v,
            ConfigurationSection::getBoolean); }

        public static boolean BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = false;
        static { register(
            "world.block_interaction_allow_block_placing",
            BLOCK_INTERACTION_ALLOW_BLOCK_PLACING, Boolean.class,
            v -> BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = v,
            ConfigurationSection::getBoolean); }

        public static boolean BLOCK_INTERACTION_RESPECT_WORLD_GUARD = true;
        static { register(
            "world.block_interaction_respect_world_guard",
            BLOCK_INTERACTION_RESPECT_WORLD_GUARD, Boolean.class,
            v -> BLOCK_INTERACTION_RESPECT_WORLD_GUARD = v,
            ConfigurationSection::getBoolean); }

        // Explosions configuration
        public static boolean EXPLOSIONS_SET_FIRE = false;
        static { register(
            "world.explosions_set_fire",
            EXPLOSIONS_SET_FIRE, Boolean.class,
            v -> EXPLOSIONS_SET_FIRE = v,
            ConfigurationSection::getBoolean); }

        public static boolean EXPLOSIONS_BREAK_BLOCKS = false;
        static { register(
            "world.explosions_break_blocks",
            EXPLOSIONS_BREAK_BLOCKS, Boolean.class,
            v -> EXPLOSIONS_BREAK_BLOCKS = v,
            ConfigurationSection::getBoolean); }
    }
    //endregion

    // ==============================================================================
    //region DEBUG - Development and testing options
    // ==============================================================================
    /**
     * Debug and development configuration for logging and visualization.
     * <p>
     * Enables verbose logging and visual debugging tools. All options default to
     * {@code false} for production. Enable selectively for development/troubleshooting.
     * </p>
     *
     * <h2>Debug Tools</h2>
     * <ul>
     *   <li><b>Verbose Logging</b> - Per-system console/chat output (combat, movement, inventory,
     *       system, umbral, hostile, general debug)</li>
     *   <li><b>Visualization</b> - Particle-based hitbox and raytrace rendering</li>
     * </ul>
     *
     * <p><b>Warning:</b> Visualization features generate many particles and may impact performance.</p>
     */
    public static class Debug {
        // Logging — general
        public static boolean LOGGING_VERBOSE_DEBUG = false;
        static { register(
            "debug.logging_verbose_debug",
            LOGGING_VERBOSE_DEBUG, Boolean.class,
            v -> LOGGING_VERBOSE_DEBUG = v,
            ConfigurationSection::getBoolean); }

        // Logging — per system
        public static boolean LOGGING_VERBOSE_COMBAT = false;
        static { register(
            "debug.logging_verbose_combat",
            LOGGING_VERBOSE_COMBAT, Boolean.class,
            v -> LOGGING_VERBOSE_COMBAT = v,
            ConfigurationSection::getBoolean); }

        public static boolean LOGGING_VERBOSE_MOVEMENT = false;
        static { register(
            "debug.logging_verbose_movement",
            LOGGING_VERBOSE_MOVEMENT, Boolean.class,
            v -> LOGGING_VERBOSE_MOVEMENT = v,
            ConfigurationSection::getBoolean); }

        public static boolean LOGGING_VERBOSE_INVENTORY = false;
        static { register(
            "debug.logging_verbose_inventory",
            LOGGING_VERBOSE_INVENTORY, Boolean.class,
            v -> LOGGING_VERBOSE_INVENTORY = v,
            ConfigurationSection::getBoolean); }

        public static boolean LOGGING_VERBOSE_SYSTEM = false;
        static { register(
            "debug.logging_verbose_system",
            LOGGING_VERBOSE_SYSTEM, Boolean.class,
            v -> LOGGING_VERBOSE_SYSTEM = v,
            ConfigurationSection::getBoolean); }

        public static boolean LOGGING_VERBOSE_UMBRAL = false;
        static { register(
            "debug.logging_verbose_umbral",
            LOGGING_VERBOSE_UMBRAL, Boolean.class,
            v -> LOGGING_VERBOSE_UMBRAL = v,
            ConfigurationSection::getBoolean); }

        public static boolean LOGGING_VERBOSE_HOSTILE = false;
        static { register(
            "debug.logging_verbose_hostile",
            LOGGING_VERBOSE_HOSTILE, Boolean.class,
            v -> LOGGING_VERBOSE_HOSTILE = v,
            ConfigurationSection::getBoolean); }

        // Visualization configuration
        public static boolean VISUALIZATION_SHOW_HITBOXES = false;
        static { register(
            "debug.visualization_show_hitboxes",
            VISUALIZATION_SHOW_HITBOXES, Boolean.class,
            v -> VISUALIZATION_SHOW_HITBOXES = v,
            ConfigurationSection::getBoolean); }

        public static boolean VISUALIZATION_SHOW_RAYTRACES = false;
        static { register(
            "debug.visualization_show_raytraces",
            VISUALIZATION_SHOW_RAYTRACES, Boolean.class,
            v -> VISUALIZATION_SHOW_RAYTRACES = v,
            ConfigurationSection::getBoolean); }
    }
    //endregion

    // ==============================================================================
    //region GRAB
    // ==============================================================================
    public static class Grab {
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
    //endregion

    // ==============================================================================
    //region Hostile AI
    // ==============================================================================
    /**
     * Configuration for Hostile entity AI behavior.
     * <p>
     * Distance thresholds are stored as squared values at load time so all in-tick
     * comparisons use {@code distanceSquared()} — no {@code sqrt} is required during gameplay.
     * </p>
     */
    public static class Hostile {
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
    }
    //endregion

    // ==============================================================================
    //region UmbralBlade States
    // ==============================================================================
    public static class UmbralBlade {
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
    //endregion

    // ==============================================================================
    //region MATERIALS - Item material overrides for inventory buttons and UI
    // ==============================================================================
    /**
     * Material overrides for inventory UI buttons and storage shortcut items.
     */
    public static class Materials {
        /** Material used for the Currency Storage shortcut button in the player's inventory. */
        public static Material CURRENCY_STORAGE_ITEM = Material.NETHERITE_INGOT;
        static { register(
            "materials.currency_storage_item",
            CURRENCY_STORAGE_ITEM, Material.class,
            v -> CURRENCY_STORAGE_ITEM = v,
            Config::loadMaterial); }

        /** Material used for the Material Storage shortcut button in the player's inventory. */
        public static Material MATERIAL_STORAGE_ITEM = Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
        static { register(
            "materials.material_storage_item",
            MATERIAL_STORAGE_ITEM, Material.class,
            v -> MATERIAL_STORAGE_ITEM = v,
            Config::loadMaterial); }

        /** Material used for the Quest Storage shortcut button in the player's inventory. */
        public static Material QUEST_STORAGE_ITEM = Material.WRITABLE_BOOK;
        static { register(
            "materials.quest_storage_item",
            QUEST_STORAGE_ITEM, Material.class,
            v -> QUEST_STORAGE_ITEM = v,
            Config::loadMaterial); }
    }

    /**
     * Bézier control-point vectors for each named {@link btm.sword.system.attack.style.AttackType}.
     *
     * <p>Each attack type exposes four {@link Vector} fields — {@code _START}, {@code _END},
     * {@code _C1}, and {@code _C2} — corresponding to the four cubic Bézier control points.
     * Values are loaded from the {@code attack_curves} YAML section as {@code {x, y, z}} maps
     * and hot-reloaded by {@code /sword reload}.
     *
     * <p>{@link btm.sword.system.attack.style.AttackType} enum constants reference these fields
     * via {@link btm.sword.utility.math.ControlVectors} suppliers so that live config changes
     * take effect on the next attack without a restart.
     */
    public static class AttackCurves {
        // UMBRAL_SLASH1
        public static Vector UMBRAL_SLASH1_START = new Vector(-2, 0, 0);
        static { register("attack_curves.umbral_slash1_start", UMBRAL_SLASH1_START, Vector.class,
            v -> UMBRAL_SLASH1_START = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_END = new Vector(2, 0, -1);
        static { register("attack_curves.umbral_slash1_end", UMBRAL_SLASH1_END, Vector.class,
            v -> UMBRAL_SLASH1_END = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_C1 = new Vector(-1.5, 0, 2);
        static { register("attack_curves.umbral_slash1_c1", UMBRAL_SLASH1_C1, Vector.class,
            v -> UMBRAL_SLASH1_C1 = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_C2 = new Vector(1.5, 0, 3);
        static { register("attack_curves.umbral_slash1_c2", UMBRAL_SLASH1_C2, Vector.class,
            v -> UMBRAL_SLASH1_C2 = v, Config::loadVector); }

        // UMBRAL_SLASH1_WINDUP
        public static Vector UMBRAL_SLASH1_WINDUP_START = new Vector(-1.5, 0.17, 1);
        static { register("attack_curves.umbral_slash1_windup_start", UMBRAL_SLASH1_WINDUP_START, Vector.class,
            v -> UMBRAL_SLASH1_WINDUP_START = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_WINDUP_END = new Vector(-2.1, -0.33, -0.5);
        static { register("attack_curves.umbral_slash1_windup_end", UMBRAL_SLASH1_WINDUP_END, Vector.class,
            v -> UMBRAL_SLASH1_WINDUP_END = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_WINDUP_C1 = new Vector(-2, 0.17, 0.5);
        static { register("attack_curves.umbral_slash1_windup_c1", UMBRAL_SLASH1_WINDUP_C1, Vector.class,
            v -> UMBRAL_SLASH1_WINDUP_C1 = v, Config::loadVector); }
        public static Vector UMBRAL_SLASH1_WINDUP_C2 = new Vector(-2, -0.08, 0);
        static { register("attack_curves.umbral_slash1_windup_c2", UMBRAL_SLASH1_WINDUP_C2, Vector.class,
            v -> UMBRAL_SLASH1_WINDUP_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH1
        public static Vector WIDE_UMBRAL_SLASH1_START = new Vector(-5.7615, 0, 2.171);
        static { register("attack_curves.wide_umbral_slash1_start", WIDE_UMBRAL_SLASH1_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_END = new Vector(5.845, 0, -0.334);
        static { register("attack_curves.wide_umbral_slash1_end", WIDE_UMBRAL_SLASH1_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_C1 = new Vector(-2.505, 0, 3.34);
        static { register("attack_curves.wide_umbral_slash1_c1", WIDE_UMBRAL_SLASH1_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_C2 = new Vector(2.505, 0, 5.01);
        static { register("attack_curves.wide_umbral_slash1_c2", WIDE_UMBRAL_SLASH1_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH1_WINDUP
        public static Vector WIDE_UMBRAL_SLASH1_WINDUP_START = new Vector(-1.66, 0.17, -0.5);
        static { register("attack_curves.wide_umbral_slash1_windup_start", WIDE_UMBRAL_SLASH1_WINDUP_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_WINDUP_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_WINDUP_END = new Vector(-5, 0.27, 0.83);
        static { register("attack_curves.wide_umbral_slash1_windup_end", WIDE_UMBRAL_SLASH1_WINDUP_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_WINDUP_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_WINDUP_C1 = new Vector(-2.5, 1.03, 1.7);
        static { register("attack_curves.wide_umbral_slash1_windup_c1", WIDE_UMBRAL_SLASH1_WINDUP_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_WINDUP_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH1_WINDUP_C2 = new Vector(-3.77, 0.51, 2.26);
        static { register("attack_curves.wide_umbral_slash1_windup_c2", WIDE_UMBRAL_SLASH1_WINDUP_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH1_WINDUP_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH2
        public static Vector WIDE_UMBRAL_SLASH2_START = new Vector(4.008, -1.002, -1.169);
        static { register("attack_curves.wide_umbral_slash2_start", WIDE_UMBRAL_SLASH2_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_END = new Vector(-3.841, 1.67, 4.008);
        static { register("attack_curves.wide_umbral_slash2_end", WIDE_UMBRAL_SLASH2_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_C1 = new Vector(1.67, 0, 2.839);
        static { register("attack_curves.wide_umbral_slash2_c1", WIDE_UMBRAL_SLASH2_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_C2 = new Vector(-1.336, 0, 5.01);
        static { register("attack_curves.wide_umbral_slash2_c2", WIDE_UMBRAL_SLASH2_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH2_WINDUP
        public static Vector WIDE_UMBRAL_SLASH2_WINDUP_START = new Vector(5.344, -0.2171, -1.002);
        static { register("attack_curves.wide_umbral_slash2_windup_start", WIDE_UMBRAL_SLASH2_WINDUP_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_WINDUP_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_WINDUP_END = new Vector(4.509, -1.503, -2.338);
        static { register("attack_curves.wide_umbral_slash2_windup_end", WIDE_UMBRAL_SLASH2_WINDUP_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_WINDUP_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_WINDUP_C1 = new Vector(4.7261, 0, -0.501);
        static { register("attack_curves.wide_umbral_slash2_windup_c1", WIDE_UMBRAL_SLASH2_WINDUP_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_WINDUP_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH2_WINDUP_C2 = new Vector(3.674, -0.4008, -1.5698);
        static { register("attack_curves.wide_umbral_slash2_windup_c2", WIDE_UMBRAL_SLASH2_WINDUP_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH2_WINDUP_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH3
        public static Vector WIDE_UMBRAL_SLASH3_START = new Vector(0, 5.177, -0.334);
        static { register("attack_curves.wide_umbral_slash3_start", WIDE_UMBRAL_SLASH3_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_END = new Vector(0, -3.674, 1.336);
        static { register("attack_curves.wide_umbral_slash3_end", WIDE_UMBRAL_SLASH3_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_C1 = new Vector(0, 1.7368, 1.67);
        static { register("attack_curves.wide_umbral_slash3_c1", WIDE_UMBRAL_SLASH3_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_C2 = new Vector(0, -0.167, 2.9058);
        static { register("attack_curves.wide_umbral_slash3_c2", WIDE_UMBRAL_SLASH3_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_C2 = v, Config::loadVector); }

        // WIDE_UMBRAL_SLASH3_WINDUP
        public static Vector WIDE_UMBRAL_SLASH3_WINDUP_START = new Vector(-3.34, 2.0708, 3.34);
        static { register("attack_curves.wide_umbral_slash3_windup_start", WIDE_UMBRAL_SLASH3_WINDUP_START, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_WINDUP_START = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_WINDUP_END = new Vector(0.334, 4.0915, 0);
        static { register("attack_curves.wide_umbral_slash3_windup_end", WIDE_UMBRAL_SLASH3_WINDUP_END, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_WINDUP_END = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_WINDUP_C1 = new Vector(1.67, 0, 1.67);
        static { register("attack_curves.wide_umbral_slash3_windup_c1", WIDE_UMBRAL_SLASH3_WINDUP_C1, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_WINDUP_C1 = v, Config::loadVector); }
        public static Vector WIDE_UMBRAL_SLASH3_WINDUP_C2 = new Vector(-1.336, 0, 3.841);
        static { register("attack_curves.wide_umbral_slash3_windup_c2", WIDE_UMBRAL_SLASH3_WINDUP_C2, Vector.class,
            v -> WIDE_UMBRAL_SLASH3_WINDUP_C2 = v, Config::loadVector); }

        // SLASH1
        public static Vector SLASH1_START = new Vector(-2.06, -1.26, -0.5);
        static { register("attack_curves.slash1_start", SLASH1_START, Vector.class,
            v -> SLASH1_START = v, Config::loadVector); }
        public static Vector SLASH1_END = new Vector(3.26, 0.79, -0.4);
        static { register("attack_curves.slash1_end", SLASH1_END, Vector.class,
            v -> SLASH1_END = v, Config::loadVector); }
        public static Vector SLASH1_C1 = new Vector(-2.3, -0.16, 3);
        static { register("attack_curves.slash1_c1", SLASH1_C1, Vector.class,
            v -> SLASH1_C1 = v, Config::loadVector); }
        public static Vector SLASH1_C2 = new Vector(1.9, 0.21, 5);
        static { register("attack_curves.slash1_c2", SLASH1_C2, Vector.class,
            v -> SLASH1_C2 = v, Config::loadVector); }

        // SLASH2
        public static Vector SLASH2_START = new Vector(2.6, -1.3, -1.2);
        static { register("attack_curves.slash2_start", SLASH2_START, Vector.class,
            v -> SLASH2_START = v, Config::loadVector); }
        public static Vector SLASH2_END = new Vector(-3, 0.9, 1.3);
        static { register("attack_curves.slash2_end", SLASH2_END, Vector.class,
            v -> SLASH2_END = v, Config::loadVector); }
        public static Vector SLASH2_C1 = new Vector(1.6, -0.7, 7);
        static { register("attack_curves.slash2_c1", SLASH2_C1, Vector.class,
            v -> SLASH2_C1 = v, Config::loadVector); }
        public static Vector SLASH2_C2 = new Vector(-2.6, 1.05, 1.85);
        static { register("attack_curves.slash2_c2", SLASH2_C2, Vector.class,
            v -> SLASH2_C2 = v, Config::loadVector); }

        // SLASH3
        public static Vector SLASH3_START = new Vector(1.2, 2.8, -1.5);
        static { register("attack_curves.slash3_start", SLASH3_START, Vector.class,
            v -> SLASH3_START = v, Config::loadVector); }
        public static Vector SLASH3_END = new Vector(-1.1, -2.2, -0.9);
        static { register("attack_curves.slash3_end", SLASH3_END, Vector.class,
            v -> SLASH3_END = v, Config::loadVector); }
        public static Vector SLASH3_C1 = new Vector(1, 1.96, 4.3);
        static { register("attack_curves.slash3_c1", SLASH3_C1, Vector.class,
            v -> SLASH3_C1 = v, Config::loadVector); }
        public static Vector SLASH3_C2 = new Vector(-1.1, -1.77, 5);
        static { register("attack_curves.slash3_c2", SLASH3_C2, Vector.class,
            v -> SLASH3_C2 = v, Config::loadVector); }

        // UP_SMASH
        public static Vector UP_SMASH_START = new Vector(0.66, -1.53, -0.5);
        static { register("attack_curves.up_smash_start", UP_SMASH_START, Vector.class,
            v -> UP_SMASH_START = v, Config::loadVector); }
        public static Vector UP_SMASH_END = new Vector(-0.4, 0.67, -0.9);
        static { register("attack_curves.up_smash_end", UP_SMASH_END, Vector.class,
            v -> UP_SMASH_END = v, Config::loadVector); }
        public static Vector UP_SMASH_C1 = new Vector(0.56, -0.89, 2.1);
        static { register("attack_curves.up_smash_c1", UP_SMASH_C1, Vector.class,
            v -> UP_SMASH_C1 = v, Config::loadVector); }
        public static Vector UP_SMASH_C2 = new Vector(-0.4, 1.37, 1.65);
        static { register("attack_curves.up_smash_c2", UP_SMASH_C2, Vector.class,
            v -> UP_SMASH_C2 = v, Config::loadVector); }

        // LUNGE1
        public static Vector LUNGE1_START = new Vector(0.37, 0, 2);
        static { register("attack_curves.lunge1_start", LUNGE1_START, Vector.class,
            v -> LUNGE1_START = v, Config::loadVector); }
        public static Vector LUNGE1_END = new Vector(0, 0, 20);
        static { register("attack_curves.lunge1_end", LUNGE1_END, Vector.class,
            v -> LUNGE1_END = v, Config::loadVector); }
        public static Vector LUNGE1_C1 = new Vector(1.1, 0, 3.1);
        static { register("attack_curves.lunge1_c1", LUNGE1_C1, Vector.class,
            v -> LUNGE1_C1 = v, Config::loadVector); }
        public static Vector LUNGE1_C2 = new Vector(0, 0, 2.46);
        static { register("attack_curves.lunge1_c2", LUNGE1_C2, Vector.class,
            v -> LUNGE1_C2 = v, Config::loadVector); }

        // F_DASH_ATTACK
        public static Vector F_DASH_ATTACK_START = new Vector(-1.95, -0.76, 0.9);
        static { register("attack_curves.f_dash_attack_start", F_DASH_ATTACK_START, Vector.class,
            v -> F_DASH_ATTACK_START = v, Config::loadVector); }
        public static Vector F_DASH_ATTACK_END = new Vector(1.2, 1.1, 7.3);
        static { register("attack_curves.f_dash_attack_end", F_DASH_ATTACK_END, Vector.class,
            v -> F_DASH_ATTACK_END = v, Config::loadVector); }
        public static Vector F_DASH_ATTACK_C1 = new Vector(-1.6, -0.57, 2.7);
        static { register("attack_curves.f_dash_attack_c1", F_DASH_ATTACK_C1, Vector.class,
            v -> F_DASH_ATTACK_C1 = v, Config::loadVector); }
        public static Vector F_DASH_ATTACK_C2 = new Vector(-0.93, 0, 4.9);
        static { register("attack_curves.f_dash_attack_c2", F_DASH_ATTACK_C2, Vector.class,
            v -> F_DASH_ATTACK_C2 = v, Config::loadVector); }

        // B_DASH_ATTACK
        public static Vector B_DASH_ATTACK_START = new Vector(0.696, 2.2388, 1.74);
        static { register("attack_curves.b_dash_attack_start", B_DASH_ATTACK_START, Vector.class,
            v -> B_DASH_ATTACK_START = v, Config::loadVector); }
        public static Vector B_DASH_ATTACK_END = new Vector(-0.8932, -3.2016, 0.116);
        static { register("attack_curves.b_dash_attack_end", B_DASH_ATTACK_END, Vector.class,
            v -> B_DASH_ATTACK_END = v, Config::loadVector); }
        public static Vector B_DASH_ATTACK_C1 = new Vector(0.3132, 0.4176, 2.204);
        static { register("attack_curves.b_dash_attack_c1", B_DASH_ATTACK_C1, Vector.class,
            v -> B_DASH_ATTACK_C1 = v, Config::loadVector); }
        public static Vector B_DASH_ATTACK_C2 = new Vector(-0.58, -1.74, 1.3572);
        static { register("attack_curves.b_dash_attack_c2", B_DASH_ATTACK_C2, Vector.class,
            v -> B_DASH_ATTACK_C2 = v, Config::loadVector); }

        // R_STRAFE_ATTACK
        public static Vector R_STRAFE_ATTACK_START = new Vector(-1.503, -0.4008, 0.668);
        static { register("attack_curves.r_strafe_attack_start", R_STRAFE_ATTACK_START, Vector.class,
            v -> R_STRAFE_ATTACK_START = v, Config::loadVector); }
        public static Vector R_STRAFE_ATTACK_END = new Vector(4.676, 0.334, 1.169);
        static { register("attack_curves.r_strafe_attack_end", R_STRAFE_ATTACK_END, Vector.class,
            v -> R_STRAFE_ATTACK_END = v, Config::loadVector); }
        public static Vector R_STRAFE_ATTACK_C1 = new Vector(0.167, 0, 1.4362);
        static { register("attack_curves.r_strafe_attack_c1", R_STRAFE_ATTACK_C1, Vector.class,
            v -> R_STRAFE_ATTACK_C1 = v, Config::loadVector); }
        public static Vector R_STRAFE_ATTACK_C2 = new Vector(2.2211, 0, 1.7702);
        static { register("attack_curves.r_strafe_attack_c2", R_STRAFE_ATTACK_C2, Vector.class,
            v -> R_STRAFE_ATTACK_C2 = v, Config::loadVector); }

        // L_STRAFE_ATTACK
        public static Vector L_STRAFE_ATTACK_START = new Vector(1.503, -0.4008, 0.668);
        static { register("attack_curves.l_strafe_attack_start", L_STRAFE_ATTACK_START, Vector.class,
            v -> L_STRAFE_ATTACK_START = v, Config::loadVector); }
        public static Vector L_STRAFE_ATTACK_END = new Vector(-4.676, 0.334, 1.169);
        static { register("attack_curves.l_strafe_attack_end", L_STRAFE_ATTACK_END, Vector.class,
            v -> L_STRAFE_ATTACK_END = v, Config::loadVector); }
        public static Vector L_STRAFE_ATTACK_C1 = new Vector(-0.167, 0, 1.4362);
        static { register("attack_curves.l_strafe_attack_c1", L_STRAFE_ATTACK_C1, Vector.class,
            v -> L_STRAFE_ATTACK_C1 = v, Config::loadVector); }
        public static Vector L_STRAFE_ATTACK_C2 = new Vector(-2.2211, 0, 1.7702);
        static { register("attack_curves.l_strafe_attack_c2", L_STRAFE_ATTACK_C2, Vector.class,
            v -> L_STRAFE_ATTACK_C2 = v, Config::loadVector); }

        // D_AIR
        public static Vector D_AIR_START = new Vector(-0.35, 2.53, 0.56);
        static { register("attack_curves.d_air_start", D_AIR_START, Vector.class,
            v -> D_AIR_START = v, Config::loadVector); }
        public static Vector D_AIR_END = new Vector(0, -3.42, -0.581);
        static { register("attack_curves.d_air_end", D_AIR_END, Vector.class,
            v -> D_AIR_END = v, Config::loadVector); }
        public static Vector D_AIR_C1 = new Vector(0.329, -0.165, 4.97);
        static { register("attack_curves.d_air_c1", D_AIR_C1, Vector.class,
            v -> D_AIR_C1 = v, Config::loadVector); }
        public static Vector D_AIR_C2 = new Vector(-0.07, -6.15, 0.98);
        static { register("attack_curves.d_air_c2", D_AIR_C2, Vector.class,
            v -> D_AIR_C2 = v, Config::loadVector); }

        // N_AIR
        public static Vector N_AIR_START = new Vector(1.0961, 1.742, -1.13);
        static { register("attack_curves.n_air_start", N_AIR_START, Vector.class,
            v -> N_AIR_START = v, Config::loadVector); }
        public static Vector N_AIR_END = new Vector(0, -1.987, -0.791);
        static { register("attack_curves.n_air_end", N_AIR_END, Vector.class,
            v -> N_AIR_END = v, Config::loadVector); }
        public static Vector N_AIR_C1 = new Vector(-0.2825, 0.951, 9.153);
        static { register("attack_curves.n_air_c1", N_AIR_C1, Vector.class,
            v -> N_AIR_C1 = v, Config::loadVector); }
        public static Vector N_AIR_C2 = new Vector(-0.7458, -5.151, -1.808);
        static { register("attack_curves.n_air_c2", N_AIR_C2, Vector.class,
            v -> N_AIR_C2 = v, Config::loadVector); }
    }
    //endregion
}
