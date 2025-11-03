package btm.sword.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

/**
 * Type-safe accessor for entity behavior configuration values.
 */
@Getter
public class EntityConfig {
    private final ThrowPreparationConfig throwPreparation;
    private final PinnedRotationConfig pinnedRotation;

    public EntityConfig(FileConfiguration config) {
        ConfigurationSection entities = config.getConfigurationSection("entities");
        if (entities != null) {
            this.throwPreparation = new ThrowPreparationConfig(entities.getConfigurationSection("throw_preparation"));
            this.pinnedRotation = new PinnedRotationConfig(entities.getConfigurationSection("pinned_rotation"));
        } else {
            this.throwPreparation = new ThrowPreparationConfig(null);
            this.pinnedRotation = new PinnedRotationConfig(null);
        }
    }

    @Getter
    public static class ThrowPreparationConfig {
        private final PotionEffectType effect;
        private final int duration;
        private final int amplifier;

        public ThrowPreparationConfig(ConfigurationSection section) {
            if (section != null) {
                String effectName = section.getString("effect", "SLOWNESS");
                this.effect = PotionEffectType.getByName(effectName);
                this.duration = section.getInt("duration", 1);
                this.amplifier = section.getInt("amplifier", 2);
            } else {
                this.effect = PotionEffectType.SLOWNESS;
                this.duration = 1;
                this.amplifier = 2;
            }
        }
    }

    @Getter
    public static class PinnedRotationConfig {
        private final boolean lockRotation;
        private final boolean resetVelocity;

        public PinnedRotationConfig(ConfigurationSection section) {
            if (section != null) {
                this.lockRotation = section.getBoolean("lock_rotation", true);
                this.resetVelocity = section.getBoolean("reset_velocity", true);
            } else {
                this.lockRotation = true;
                this.resetVelocity = true;
            }
        }
    }
}
