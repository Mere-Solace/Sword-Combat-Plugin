package btm.sword.config.section;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import lombok.Getter;

/**
 * Type-safe accessor for entity behavior configuration values.
 * <p>
 * Uses hybrid pattern: simple configs (2-3 values) are flattened to direct fields,
 * while complex hierarchical configs retain nested inner classes.
 * </p>
 */
@Getter
public class EntityConfig {
    // Flattened throw preparation config (3 simple values - no wrapper class needed)
    private final PotionEffectType throwPreparationEffect;
    private final int throwPreparationDuration;
    private final int throwPreparationAmplifier;

    // Flattened pinned rotation config (2 simple values - no wrapper class needed)
    private final boolean pinnedRotationLock;
    private final boolean pinnedRotationResetVelocity;

    // Keep nested for complex hierarchical config (resource values need structure)
    private final CombatProfileConfig combatProfile;

    public EntityConfig(FileConfiguration config) {
        ConfigurationSection entities = config.getConfigurationSection("entities");

        // Load throw preparation values directly
        if (entities != null) {
            ConfigurationSection throwPrep = entities.getConfigurationSection("throw_preparation");
            if (throwPrep != null) {
                String effectName = throwPrep.getString("effect", "SLOWNESS");
                this.throwPreparationEffect = Registry.EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase()));
                this.throwPreparationDuration = throwPrep.getInt("duration", 1);
                this.throwPreparationAmplifier = throwPrep.getInt("amplifier", 2);
            } else {
                this.throwPreparationEffect = PotionEffectType.SLOWNESS;
                this.throwPreparationDuration = 1;
                this.throwPreparationAmplifier = 2;
            }

            // Load pinned rotation values directly
            ConfigurationSection pinnedRot = entities.getConfigurationSection("pinned_rotation");
            if (pinnedRot != null) {
                this.pinnedRotationLock = pinnedRot.getBoolean("lock_rotation", true);
                this.pinnedRotationResetVelocity = pinnedRot.getBoolean("reset_velocity", true);
            } else {
                this.pinnedRotationLock = true;
                this.pinnedRotationResetVelocity = true;
            }

            // Load combat profile with nested config
            this.combatProfile = new CombatProfileConfig(entities.getConfigurationSection("combat_profile"));
        } else {
            // Defaults when entities section is missing
            this.throwPreparationEffect = PotionEffectType.SLOWNESS;
            this.throwPreparationDuration = 1;
            this.throwPreparationAmplifier = 2;
            this.pinnedRotationLock = true;
            this.pinnedRotationResetVelocity = true;
            this.combatProfile = new CombatProfileConfig(null);
        }
    }

    /**
     * Combat profile configuration - wrapper for resource values.
     * Matches ResourceValue constructor: (float value, int regenPeriod, float regenAmount)
     */
    @Getter
    public static class CombatProfileConfig {
        private final float shardsCurrent;
        private final int shardsRegenPeriod;
        private final float shardsRegenAmount;

        private final float toughnessCurrent;
        private final int toughnessRegenPeriod;
        private final float toughnessRegenAmount;

        private final float soulfireCurrent;
        private final int soulfireRegenPeriod;
        private final float soulfireRegenAmount;

        private final float formCurrent;
        private final int formRegenPeriod;
        private final float formRegenAmount;

        private final int maxAirDodges;

        public CombatProfileConfig(ConfigurationSection section) {
            if (section != null) {
                // Load shards resource values
                ConfigurationSection shards = section.getConfigurationSection("shards");
                if (shards != null) {
                    this.shardsCurrent = (float) shards.getDouble("current", 10.0);
                    this.shardsRegenPeriod = shards.getInt("regen_period", 50);
                    this.shardsRegenAmount = (float) shards.getDouble("regen_amount", 1.0);
                } else {
                    this.shardsCurrent = 10.0f;
                    this.shardsRegenPeriod = 50;
                    this.shardsRegenAmount = 1.0f;
                }

                // Load toughness resource values
                ConfigurationSection toughness = section.getConfigurationSection("toughness");
                if (toughness != null) {
                    this.toughnessCurrent = (float) toughness.getDouble("current", 20.0);
                    this.toughnessRegenPeriod = toughness.getInt("regen_period", 20);
                    this.toughnessRegenAmount = (float) toughness.getDouble("regen_amount", 0.5);
                } else {
                    this.toughnessCurrent = 20.0f;
                    this.toughnessRegenPeriod = 20;
                    this.toughnessRegenAmount = 0.5f;
                }

                // Load soulfire resource values
                ConfigurationSection soulfire = section.getConfigurationSection("soulfire");
                if (soulfire != null) {
                    this.soulfireCurrent = (float) soulfire.getDouble("current", 100.0);
                    this.soulfireRegenPeriod = soulfire.getInt("regen_period", 5);
                    this.soulfireRegenAmount = (float) soulfire.getDouble("regen_amount", 0.2);
                } else {
                    this.soulfireCurrent = 100.0f;
                    this.soulfireRegenPeriod = 5;
                    this.soulfireRegenAmount = 0.2f;
                }

                // Load form resource values
                ConfigurationSection form = section.getConfigurationSection("form");
                if (form != null) {
                    this.formCurrent = (float) form.getDouble("current", 10.0);
                    this.formRegenPeriod = form.getInt("regen_period", 60);
                    this.formRegenAmount = (float) form.getDouble("regen_amount", 1.0);
                } else {
                    this.formCurrent = 10.0f;
                    this.formRegenPeriod = 60;
                    this.formRegenAmount = 1.0f;
                }

                this.maxAirDodges = section.getInt("max_air_dodges", 1);
            } else {
                // All defaults
                this.shardsCurrent = 10.0f;
                this.shardsRegenPeriod = 50;
                this.shardsRegenAmount = 1.0f;
                this.toughnessCurrent = 20.0f;
                this.toughnessRegenPeriod = 20;
                this.toughnessRegenAmount = 0.5f;
                this.soulfireCurrent = 100.0f;
                this.soulfireRegenPeriod = 5;
                this.soulfireRegenAmount = 0.2f;
                this.formCurrent = 10.0f;
                this.formRegenPeriod = 60;
                this.formRegenAmount = 1.0f;
                this.maxAirDodges = 1;
            }
        }
    }
}
