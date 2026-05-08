package btm.sword.config.section;

import static btm.sword.config.Config.register;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import btm.sword.action.attack.AttackAction;
import btm.sword.config.Config;

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
 * @see btm.sword.combat.attack.Attack Attack execution and damage application
 * @see AttackAction Attack state machine
 */
public final class CombatConfig {

    private CombatConfig() {}

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

    /**
     * Default: whether volume attack OBBs are tilted by the attacker's pitch angle.
     * Per-attack overrides are stored in the attack's YAML definition.
     */
    public static boolean ATTACKS_ORIENT_WITH_PITCH = false;
    static { register("combat.attacks_orient_with_pitch",
        ATTACKS_ORIENT_WITH_PITCH, Boolean.class,
        v -> ATTACKS_ORIENT_WITH_PITCH = v,
        ConfigurationSection::getBoolean
    ); }

    /**
     * Default: whether volume attacks lock their origin and direction at fire time.
     * When {@code true} the OBBs move in the direction the player faced when attacking,
     * regardless of where the player moves afterwards.
     * Per-attack overrides are stored in the attack's YAML definition.
     */
    public static boolean ATTACKS_LOCK_ORIGIN_ON_FIRE = true;
    static { register("combat.attacks_lock_origin_on_fire",
        ATTACKS_LOCK_ORIGIN_ON_FIRE, Boolean.class,
        v -> ATTACKS_LOCK_ORIGIN_ON_FIRE = v,
        ConfigurationSection::getBoolean
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

    /** Squared distance filter applied to UmbralBlade secant hit detection. */
    public static double UMBRAL_BLADE_ATTACK_RANGE_SQUARED = 20;
    static { register("combat.umbral_blade_attack_range_squared",
        UMBRAL_BLADE_ATTACK_RANGE_SQUARED, Double.class,
        v -> UMBRAL_BLADE_ATTACK_RANGE_SQUARED = v,
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
        ConfigurationSection::getLong
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
