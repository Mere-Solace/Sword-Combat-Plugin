package btm.sword.util.sound;

import java.util.function.Function;

import org.bukkit.entity.LivingEntity;

import btm.sword.config.ConfigManager;
import btm.sword.config.section.AudioConfig;

/**
 * Wrapper class for handling sound effects with configuration system integration.
 * <p>
 * Similar to {@link btm.sword.util.display.ParticleWrapper}, this class provides
 * a prefab object pattern for playing sounds. Sound properties (type, volume, pitch)
 * are dynamically loaded from the configuration system at play time, enabling
 * hot-reload functionality.
 * </p>
 * <p>
 * Usage: {@code Prefab.Sounds.ATTACK.play(entity);}
 * </p>
 */
public class SoundWrapper {
    /** Function to extract the sound configuration from AudioConfig */
    private final Function<AudioConfig, AudioConfig.SoundConfig> configExtractor;

    /**
     * Constructs a SoundWrapper with a configuration extractor function.
     * <p>
     * The extractor function is called each time {@link #play(LivingEntity)} is invoked,
     * ensuring hot-reload compatibility by fetching fresh config values.
     * </p>
     *
     * @param configExtractor function that extracts the desired sound config from AudioConfig
     */
    public SoundWrapper(Function<AudioConfig, AudioConfig.SoundConfig> configExtractor) {
        this.configExtractor = configExtractor;
    }

    /**
     * Plays the sound effect at the specified entity's location.
     * <p>
     * Fetches sound properties (type, volume, pitch) from the configuration system
     * at call time, then delegates to {@link SoundUtil#playSound(LivingEntity, SoundType, float, float)}.
     * </p>
     *
     * @param entity the entity to play the sound at
     */
    public void play(LivingEntity entity) {
        AudioConfig.SoundConfig sound = configExtractor.apply(ConfigManager.getInstance().getAudio());
        SoundUtil.playSound(entity, sound.getSound(), sound.getVolume(), sound.getPitch());
    }
}
