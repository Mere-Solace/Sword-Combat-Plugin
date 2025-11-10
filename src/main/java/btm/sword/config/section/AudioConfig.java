package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import btm.sword.util.sound.SoundType;
import lombok.Getter;

/**
 * Type-safe accessor for audio configuration values.
 */
@Getter
public class AudioConfig {
    private final SoundConfig throwSound;
    private final SoundConfig attackSound;

    public AudioConfig(FileConfiguration config) {
        ConfigurationSection audio = config.getConfigurationSection("audio");
        if (audio != null) {
            this.throwSound = new SoundConfig(audio.getConfigurationSection("throw"));
            this.attackSound = new SoundConfig(audio.getConfigurationSection("attack"));
        } else {
            this.throwSound = new SoundConfig(null, "ENTITY_ENDER_DRAGON_FLAP", 0.5f, 0.4f);
            this.attackSound = new SoundConfig(null, "ENTITY_ENDER_DRAGON_FLAP", 0.35f, 0.6f);
        }
    }

    @Getter
    public static class SoundConfig {
        private final SoundType sound;
        private final float volume;
        private final float pitch;

        public SoundConfig(ConfigurationSection section) {
            this(section, "ENTITY_ENDER_DRAGON_FLAP", 1.0f, 1.0f);
        }

        public SoundConfig(ConfigurationSection section, String defaultSound, float defaultVolume, float defaultPitch) {
            if (section != null) {
                String soundName = section.getString("sound", defaultSound);
                this.sound = SoundType.valueOf(soundName);
                this.volume = (float) section.getDouble("volume", defaultVolume);
                this.pitch = (float) section.getDouble("pitch", defaultPitch);
            } else {
                this.sound = SoundType.valueOf(defaultSound);
                this.volume = defaultVolume;
                this.pitch = defaultPitch;
            }
        }
    }
}
