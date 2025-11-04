package btm.sword.config.section;

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
    private final CombatProfileConfig combatProfile;

    public EntityConfig(FileConfiguration config) {
        ConfigurationSection entities = config.getConfigurationSection("entities");
        if (entities != null) {
            this.throwPreparation = new ThrowPreparationConfig(entities.getConfigurationSection("throw_preparation"));
            this.pinnedRotation = new PinnedRotationConfig(entities.getConfigurationSection("pinned_rotation"));
            this.combatProfile = new CombatProfileConfig(entities.getConfigurationSection("combat_profile"));
        } else {
            this.throwPreparation = new ThrowPreparationConfig(null);
            this.pinnedRotation = new PinnedRotationConfig(null);
            this.combatProfile = new CombatProfileConfig(null);
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

    @Getter
    public static class CombatProfileConfig {
        private final ResourceValuesConfig shards;
        private final ResourceValuesConfig toughness;
        private final ResourceValuesConfig soulfire;
        private final ResourceValuesConfig form;
        private final int maxAirDodges;

        public CombatProfileConfig(ConfigurationSection section) {
            if (section != null) {
                this.shards = new ResourceValuesConfig(section.getConfigurationSection("shards"), 10, 50, 1f);
                this.toughness = new ResourceValuesConfig(section.getConfigurationSection("toughness"), 20, 20, 0.5f);
                this.soulfire = new ResourceValuesConfig(section.getConfigurationSection("soulfire"), 100, 5, 0.2f);
                this.form = new ResourceValuesConfig(section.getConfigurationSection("form"), 10, 60, 1f);
                this.maxAirDodges = section.getInt("max_air_dodges", 1);
            } else {
                this.shards = new ResourceValuesConfig(null, 10, 50, 1f);
                this.toughness = new ResourceValuesConfig(null, 20, 20, 0.5f);
                this.soulfire = new ResourceValuesConfig(null, 100, 5, 0.2f);
                this.form = new ResourceValuesConfig(null, 10, 60, 1f);
                this.maxAirDodges = 1;
            }
        }
    }

    @Getter
    public static class ResourceValuesConfig {
        private final float current;
        private final float max;
        private final float regen;

        public ResourceValuesConfig(ConfigurationSection section, float defaultCurrent, float defaultMax, float defaultRegen) {
            if (section != null) {
                this.current = (float) section.getDouble("current", defaultCurrent);
                this.max = (float) section.getDouble("max", defaultMax);
                this.regen = (float) section.getDouble("regen", defaultRegen);
            } else {
                this.current = defaultCurrent;
                this.max = defaultMax;
                this.regen = defaultRegen;
            }
        }
    }
}
