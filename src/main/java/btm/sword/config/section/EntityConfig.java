package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

import btm.sword.config.Config;

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
 * @see btm.sword.entity.base.CombatProfile Combat stat management
 * @see btm.sword.entity.base.SwordEntity Entity wrapper
 */
public final class EntityConfig {

    private EntityConfig() {}

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
