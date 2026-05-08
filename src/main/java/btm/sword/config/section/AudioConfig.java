package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

import btm.sword.config.Config;
import btm.sword.util.sound.SwordSoundType;

/**
 * All configurable audio properties: global enable toggle, per-event sound keys,
 * volumes, and pitches. Values map to {@link btm.sword.util.sound.SwordSoundType} entries
 * and are used by {@link btm.sword.util.sound.SoundWrapper} and
 * {@link btm.sword.util.prefab.Prefab.Sounds}.
 */
public final class AudioConfig {

    private AudioConfig() {}

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

    public static SwordSoundType BLOCK_BROKEN_SOUND = SwordSoundType.ITEM_SHIELD_BREAK;
    static { register(
        "audio.block_broken_sound",
        BLOCK_BROKEN_SOUND, SwordSoundType.class,
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

    public static SwordSoundType PARRY_ATTEMPT_SOUND = SwordSoundType.RANDOM_BANE_SLASH;
    static { register(
        "audio.parry_attempt_sound",
        PARRY_ATTEMPT_SOUND, SwordSoundType.class,
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

    public static SwordSoundType PRE_ATTACK_SOUND = SwordSoundType.ENTITY_EVOKER_FANGS_ATTACK;
    static { register(
        "audio.pre_attack_sound",
        PRE_ATTACK_SOUND, SwordSoundType.class,
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
    public static SwordSoundType THROW_SOUND = SwordSoundType.ENTITY_ENDER_DRAGON_FLAP;
    static { register(
        "audio.throw_sound",
        THROW_SOUND, SwordSoundType.class,
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
    public static SwordSoundType ATTACK_SOUND = SwordSoundType.ITEM_TRIDENT_THROW;
    static { register(
        "audio.attack_sound",
        ATTACK_SOUND, SwordSoundType.class,
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
        "audio.entity_hit_connect_vol",
        ENTITY_HIT_CONNECT_VOLUME, Float.class,
        v -> ENTITY_HIT_CONNECT_VOLUME = v,
        Config::loadFloat
    ); }

    public static float ENTITY_HIT_CONNECT_PITCH = 1.0f;
    static { register(
        "audio.entity_hit_connect_pitch",
        ENTITY_HIT_CONNECT_PITCH, Float.class,
        v -> ENTITY_HIT_CONNECT_PITCH = v,
        Config::loadFloat
    ); }

    public static SwordSoundType PUNCH_ATTEMPT = SwordSoundType.ENTITY_PLAYER_ATTACK_SWEEP;
    static { register(
        "audio.punch_attempt",
        PUNCH_ATTEMPT, SwordSoundType.class,
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

    public static SwordSoundType PUNCH_CONNECT = SwordSoundType.ENTITY_PLAYER_ATTACK_KNOCKBACK;
    static { register(
        "audio.punch_connect",
        PUNCH_CONNECT, SwordSoundType.class,
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
