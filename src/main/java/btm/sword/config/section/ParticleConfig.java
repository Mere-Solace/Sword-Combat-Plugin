package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import btm.sword.config.Config;

/**
 * Per-effect particle configuration for gameplay particle effects.
 * <p>
 * Each effect exposes a {@link Particle} type, an integer count, and a speed.
 * Speed of {@code -1.0} is the sentinel value meaning no speed is passed to the
 * spawn call (uses the Bukkit overload without a speed parameter).
 * </p>
 */
public final class ParticleConfig {

    private ParticleConfig() {}

    /** Bleed hit effect. */
    public static Particle BLEED_TYPE = Particle.BLOCK;
    static { register("particles.bleed_type", BLEED_TYPE, Particle.class,
        v -> BLEED_TYPE = v, Config::loadParticle); }
    public static int BLEED_COUNT = 25;
    static { register("particles.bleed_count", BLEED_COUNT, Integer.class,
        v -> BLEED_COUNT = v, ConfigurationSection::getInt); }
    public static double BLEED_SPEED = -1.0;
    static { register("particles.bleed_speed", BLEED_SPEED, Double.class,
        v -> BLEED_SPEED = v, ConfigurationSection::getDouble); }

    /** Thrown-item impale effect (lodged in entity). */
    public static Particle THROWN_ITEM_IMPALE_TYPE = Particle.TRIAL_SPAWNER_DETECTION;
    static { register("particles.thrown_item_impale_type", THROWN_ITEM_IMPALE_TYPE, Particle.class, v -> THROWN_ITEM_IMPALE_TYPE = v, Config::loadParticle); }
    public static int THROWN_ITEM_IMPALE_COUNT = 4;
    static { register("particles.thrown_item_impale_count", THROWN_ITEM_IMPALE_COUNT, Integer.class, v -> THROWN_ITEM_IMPALE_COUNT = v, ConfigurationSection::getInt); }
    public static double THROWN_ITEM_IMPALE_SPEED = 0.0;
    static { register("particles.thrown_item_impale_speed", THROWN_ITEM_IMPALE_SPEED, Double.class, v -> THROWN_ITEM_IMPALE_SPEED = v, ConfigurationSection::getDouble); }

    /** Thrown-item landing-prediction marker. */
    public static Particle THROWN_ITEM_MARKER_TYPE = Particle.TRIAL_SPAWNER_DETECTION;
    static { register("particles.thrown_item_marker_type", THROWN_ITEM_MARKER_TYPE, Particle.class, v -> THROWN_ITEM_MARKER_TYPE = v, Config::loadParticle); }
    public static int THROWN_ITEM_MARKER_COUNT = 3;
    static { register("particles.thrown_item_marker_count", THROWN_ITEM_MARKER_COUNT, Integer.class, v -> THROWN_ITEM_MARKER_COUNT = v, ConfigurationSection::getInt); }
    public static double THROWN_ITEM_MARKER_SPEED = 0.0;
    static { register("particles.thrown_item_marker_speed", THROWN_ITEM_MARKER_SPEED, Double.class, v -> THROWN_ITEM_MARKER_SPEED = v, ConfigurationSection::getDouble); }

    /** Landing-prediction upward stream. */
    public static Particle LANDING_STREAM_TYPE = Particle.TRIAL_SPAWNER_DETECTION_OMINOUS;
    static { register("particles.landing_stream_type", LANDING_STREAM_TYPE, Particle.class, v -> LANDING_STREAM_TYPE = v, Config::loadParticle); }
    public static int LANDING_STREAM_COUNT = 5;
    static { register("particles.landing_stream_count", LANDING_STREAM_COUNT, Integer.class, v -> LANDING_STREAM_COUNT = v, ConfigurationSection::getInt); }
    public static double LANDING_STREAM_SPEED = 0.0;
    static { register("particles.landing_stream_speed", LANDING_STREAM_SPEED, Double.class, v -> LANDING_STREAM_SPEED = v, ConfigurationSection::getDouble); }

    /** Collision spark on attack sweep impact. */
    public static Particle COLLIDE_TYPE = Particle.CRIT;
    static { register("particles.collide_type", COLLIDE_TYPE, Particle.class, v -> COLLIDE_TYPE = v, Config::loadParticle); }
    public static int COLLIDE_COUNT = 1;
    static { register("particles.collide_count", COLLIDE_COUNT, Integer.class, v -> COLLIDE_COUNT = v, ConfigurationSection::getInt); }
    public static double COLLIDE_SPEED = 0.5;
    static { register("particles.collide_speed", COLLIDE_SPEED, Double.class, v -> COLLIDE_SPEED = v, ConfigurationSection::getDouble); }

    /** Grab cloud burst on successful grab. */
    public static Particle GRAB_CLOUD_TYPE = Particle.POOF;
    static { register("particles.grab_cloud_type", GRAB_CLOUD_TYPE, Particle.class, v -> GRAB_CLOUD_TYPE = v, Config::loadParticle); }
    public static int GRAB_CLOUD_COUNT = 20;
    static { register("particles.grab_cloud_count", GRAB_CLOUD_COUNT, Integer.class, v -> GRAB_CLOUD_COUNT = v, ConfigurationSection::getInt); }
    public static double GRAB_CLOUD_SPEED = 0.1;
    static { register("particles.grab_cloud_speed", GRAB_CLOUD_SPEED, Double.class, v -> GRAB_CLOUD_SPEED = v, ConfigurationSection::getDouble); }

    /** Grab attempt indicator (shows grab hitbox). */
    public static Particle GRAB_ATTEMPT_TYPE = Particle.SONIC_BOOM;
    static { register("particles.grab_attempt_type", GRAB_ATTEMPT_TYPE, Particle.class, v -> GRAB_ATTEMPT_TYPE = v, Config::loadParticle); }
    public static int GRAB_ATTEMPT_COUNT = 2;
    static { register("particles.grab_attempt_count", GRAB_ATTEMPT_COUNT, Integer.class, v -> GRAB_ATTEMPT_COUNT = v, ConfigurationSection::getInt); }
    public static double GRAB_ATTEMPT_SPEED = -1.0;
    static { register("particles.grab_attempt_speed", GRAB_ATTEMPT_SPEED, Double.class, v -> GRAB_ATTEMPT_SPEED = v, ConfigurationSection::getDouble); }

    /** Punch impact effect. */
    public static Particle PUNCH_TYPE = Particle.SMALL_GUST;
    static { register("particles.punch_type", PUNCH_TYPE, Particle.class, v -> PUNCH_TYPE = v, Config::loadParticle); }
    public static int PUNCH_COUNT = 1;
    static { register("particles.punch_count", PUNCH_COUNT, Integer.class, v -> PUNCH_COUNT = v, ConfigurationSection::getInt); }
    public static double PUNCH_SPEED = 0.0;
    static { register("particles.punch_speed", PUNCH_SPEED, Double.class, v -> PUNCH_SPEED = v, ConfigurationSection::getDouble); }

    /** Punch connect (hit confirmation) effect. */
    public static Particle PUNCH_CONNECT_TYPE = Particle.GUST;
    static { register("particles.punch_connect_type", PUNCH_CONNECT_TYPE, Particle.class, v -> PUNCH_CONNECT_TYPE = v, Config::loadParticle); }
    public static int PUNCH_CONNECT_COUNT = 1;
    static { register("particles.punch_connect_count", PUNCH_CONNECT_COUNT, Integer.class, v -> PUNCH_CONNECT_COUNT = v, ConfigurationSection::getInt); }
    public static double PUNCH_CONNECT_SPEED = 0.0;
    static { register("particles.punch_connect_speed", PUNCH_CONNECT_SPEED, Double.class, v -> PUNCH_CONNECT_SPEED = v, ConfigurationSection::getDouble); }

    /** Umbral blade poof on summon/dismiss. */
    public static Particle UMBRAL_BLADE_POOF_TYPE = Particle.LARGE_SMOKE;
    static { register("particles.umbral_blade_poof_type", UMBRAL_BLADE_POOF_TYPE, Particle.class, v -> UMBRAL_BLADE_POOF_TYPE = v, Config::loadParticle); }
    public static int UMBRAL_BLADE_POOF_COUNT = 50;
    static { register("particles.umbral_blade_poof_count", UMBRAL_BLADE_POOF_COUNT, Integer.class, v -> UMBRAL_BLADE_POOF_COUNT = v, ConfigurationSection::getInt); }
    public static double UMBRAL_BLADE_POOF_SPEED = 0.001;
    static { register("particles.umbral_blade_poof_speed", UMBRAL_BLADE_POOF_SPEED, Double.class, v -> UMBRAL_BLADE_POOF_SPEED = v, ConfigurationSection::getDouble); }

    /** Soulfire draining/expending poof. */
    public static Particle SOULFIRE_POOF_TYPE = Particle.SMOKE;
    static { register("particles.soulfire_poof_type", SOULFIRE_POOF_TYPE, Particle.class, v -> SOULFIRE_POOF_TYPE = v, Config::loadParticle); }
    public static int SOULFIRE_POOF_COUNT = 3;
    static { register("particles.soulfire_poof_count", SOULFIRE_POOF_COUNT, Integer.class, v -> SOULFIRE_POOF_COUNT = v, ConfigurationSection::getInt); }
    public static double SOULFIRE_POOF_SPEED = 0.0001;
    static { register("particles.soulfire_poof_speed", SOULFIRE_POOF_SPEED, Double.class, v -> SOULFIRE_POOF_SPEED = v, ConfigurationSection::getDouble); }

    /** General ambient smoke effect. */
    public static Particle SMOKE_TYPE = Particle.SMOKE;
    static { register("particles.smoke_type", SMOKE_TYPE, Particle.class, v -> SMOKE_TYPE = v, Config::loadParticle); }
    public static int SMOKE_COUNT = 1;
    static { register("particles.smoke_count", SMOKE_COUNT, Integer.class, v -> SMOKE_COUNT = v, ConfigurationSection::getInt); }
    public static double SMOKE_SPEED = 0.0;
    static { register("particles.smoke_speed", SMOKE_SPEED, Double.class, v -> SMOKE_SPEED = v, ConfigurationSection::getDouble); }

    /** Umbral Flame trail (dust transition colors are fixed). */
    public static Particle UMBRAL_FLAME_TYPE = Particle.DUST_COLOR_TRANSITION;
    static { register("particles.umbral_flame_type", UMBRAL_FLAME_TYPE, Particle.class, v -> UMBRAL_FLAME_TYPE = v, Config::loadParticle); }
    public static int UMBRAL_FLAME_COUNT = 3;
    static { register("particles.umbral_flame_count", UMBRAL_FLAME_COUNT, Integer.class, v -> UMBRAL_FLAME_COUNT = v, ConfigurationSection::getInt); }

    /** Thrown-item in-flight trail. */
    public static Particle THROW_TRAIL_TYPE = Particle.CRIT;
    static { register("particles.throw_trail_type", THROW_TRAIL_TYPE, Particle.class, v -> THROW_TRAIL_TYPE = v, Config::loadParticle); }
    public static int THROW_TRAIL_COUNT = 1;
    static { register("particles.throw_trail_count", THROW_TRAIL_COUNT, Integer.class, v -> THROW_TRAIL_COUNT = v, ConfigurationSection::getInt); }
    public static double THROW_TRAIL_SPEED = 0.0;
    static { register("particles.throw_trail_speed", THROW_TRAIL_SPEED, Double.class, v -> THROW_TRAIL_SPEED = v, ConfigurationSection::getDouble); }

    /** Item throw break burst on impact. */
    public static Particle ITEM_THROW_BREAK_TYPE = Particle.ENCHANTED_HIT;
    static { register("particles.item_throw_break_type", ITEM_THROW_BREAK_TYPE, Particle.class, v -> ITEM_THROW_BREAK_TYPE = v, Config::loadParticle); }
    public static int ITEM_THROW_BREAK_COUNT = 150;
    static { register("particles.item_throw_break_count", ITEM_THROW_BREAK_COUNT, Integer.class, v -> ITEM_THROW_BREAK_COUNT = v, ConfigurationSection::getInt); }
    public static double ITEM_THROW_BREAK_SPEED = -1.0;
    static { register("particles.item_throw_break_speed", ITEM_THROW_BREAK_SPEED, Double.class, v -> ITEM_THROW_BREAK_SPEED = v, ConfigurationSection::getDouble); }

    /** Toughness bar break burst. */
    public static Particle TOUGH_BREAK_TYPE = Particle.ENCHANTED_HIT;
    static { register("particles.tough_break_type", TOUGH_BREAK_TYPE, Particle.class, v -> TOUGH_BREAK_TYPE = v, Config::loadParticle); }
    public static int TOUGH_BREAK_COUNT = 70;
    static { register("particles.tough_break_count", TOUGH_BREAK_COUNT, Integer.class, v -> TOUGH_BREAK_COUNT = v, ConfigurationSection::getInt); }
    public static double TOUGH_BREAK_SPEED = 0.0;
    static { register("particles.tough_break_speed", TOUGH_BREAK_SPEED, Double.class, v -> TOUGH_BREAK_SPEED = v, ConfigurationSection::getDouble); }

    /** Toughness recharge sparkle (enchant). */
    public static Particle TOUGH_RECHARGE_1_TYPE = Particle.ENCHANT;
    static { register("particles.tough_recharge_1_type", TOUGH_RECHARGE_1_TYPE, Particle.class, v -> TOUGH_RECHARGE_1_TYPE = v, Config::loadParticle); }
    public static int TOUGH_RECHARGE_1_COUNT = 100;
    static { register("particles.tough_recharge_1_count", TOUGH_RECHARGE_1_COUNT, Integer.class, v -> TOUGH_RECHARGE_1_COUNT = v, ConfigurationSection::getInt); }
    public static double TOUGH_RECHARGE_1_SPEED = 0.1;
    static { register("particles.tough_recharge_1_speed", TOUGH_RECHARGE_1_SPEED, Double.class, v -> TOUGH_RECHARGE_1_SPEED = v, ConfigurationSection::getDouble); }

    /** Toughness recharge secondary (soul flame). */
    public static Particle TOUGH_RECHARGE_2_TYPE = Particle.SOUL_FIRE_FLAME;
    static { register("particles.tough_recharge_2_type", TOUGH_RECHARGE_2_TYPE, Particle.class, v -> TOUGH_RECHARGE_2_TYPE = v, Config::loadParticle); }
    public static int TOUGH_RECHARGE_2_COUNT = 40;
    static { register("particles.tough_recharge_2_count", TOUGH_RECHARGE_2_COUNT, Integer.class, v -> TOUGH_RECHARGE_2_COUNT = v, ConfigurationSection::getInt); }
    public static double TOUGH_RECHARGE_2_SPEED = 0.75;
    static { register("particles.tough_recharge_2_speed", TOUGH_RECHARGE_2_SPEED, Double.class, v -> TOUGH_RECHARGE_2_SPEED = v, ConfigurationSection::getDouble); }
}
