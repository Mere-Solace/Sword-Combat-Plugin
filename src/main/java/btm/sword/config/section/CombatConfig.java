package btm.sword.config.section;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

/**
 * Type-safe accessor for combat-related configuration values.
 * <p>
 * Handles damage, knockback, hitboxes, attack ranges, and combat mechanics.
 * Uses hybrid pattern: Simple 2-3 value groups flattened to direct fields,
 * complex groups with 4+ fields kept nested.
 * </p>
 */
@Getter
public class CombatConfig {
    private final AttacksConfig attacks;
    private final HitboxesConfig hitboxes;
    private final ThrownDamageConfig thrownDamage;
    private final ImpalementConfig impalement;

    public CombatConfig(FileConfiguration config) {
        ConfigurationSection combat = config.getConfigurationSection("combat");
        if (combat != null) {
            this.attacks = new AttacksConfig(combat.getConfigurationSection("attacks"));
            this.hitboxes = new HitboxesConfig(combat.getConfigurationSection("hitboxes"));
            this.thrownDamage = new ThrownDamageConfig(combat.getConfigurationSection("thrown_damage"));
            this.impalement = new ImpalementConfig(combat.getConfigurationSection("impalement"));
        } else {
            this.attacks = new AttacksConfig(null);
            this.hitboxes = new HitboxesConfig(null);
            this.thrownDamage = new ThrownDamageConfig(null);
            this.impalement = new ImpalementConfig(null);
        }
    }

    @Getter
    public static class AttacksConfig {
        private final double baseDamage;
        private final RangeMultipliersConfig rangeMultipliers;
        private final double downAirThreshold;
        private final int durationMultiplier;

        // Flattened cast timing config (3 simple values - no wrapper class needed)
        private final long castTimingMinDuration;
        private final long castTimingMaxDuration;
        private final double castTimingReductionRate;

        public AttacksConfig(ConfigurationSection section) {
            if (section != null) {
                this.baseDamage = section.getDouble("base_damage", 20.0);
                this.rangeMultipliers = new RangeMultipliersConfig(section.getConfigurationSection("range_multipliers"));
                this.downAirThreshold = section.getDouble("down_air_threshold", -0.5);
                this.durationMultiplier = section.getInt("duration_multiplier", 500);

                // Load cast timing values directly
                ConfigurationSection castTiming = section.getConfigurationSection("cast_timing");
                if (castTiming != null) {
                    this.castTimingMinDuration = castTiming.getLong("min_duration", 1L);
                    this.castTimingMaxDuration = castTiming.getLong("max_duration", 3L);
                    this.castTimingReductionRate = castTiming.getDouble("reduction_rate", 0.2);
                } else {
                    this.castTimingMinDuration = 1L;
                    this.castTimingMaxDuration = 3L;
                    this.castTimingReductionRate = 0.2;
                }
            } else {
                this.baseDamage = 20.0;
                this.rangeMultipliers = new RangeMultipliersConfig(null);
                this.downAirThreshold = -0.5;
                this.durationMultiplier = 500;
                this.castTimingMinDuration = 1L;
                this.castTimingMaxDuration = 3L;
                this.castTimingReductionRate = 0.2;
            }
        }
    }

    @Getter
    public static class RangeMultipliersConfig {
        private final double basic1;
        private final double basic2;
        private final double basic3;
        private final double neutralAir;
        private final double downAir;

        public RangeMultipliersConfig(ConfigurationSection section) {
            if (section != null) {
                this.basic1 = section.getDouble("basic_1", 1.4);
                this.basic2 = section.getDouble("basic_2", 1.4);
                this.basic3 = section.getDouble("basic_3", 1.4);
                this.neutralAir = section.getDouble("neutral_air", 1.3);
                this.downAir = section.getDouble("down_air", 1.2);
            } else {
                this.basic1 = 1.4;
                this.basic2 = 1.4;
                this.basic3 = 1.4;
                this.neutralAir = 1.3;
                this.downAir = 1.2;
            }
        }
    }

    @Getter
    public static class HitboxesConfig {
        private final double secantRadius;

        // Flattened thrown item hitbox config (2 simple values - no wrapper class needed)
        private final double thrownItemEntityRadius;
        private final boolean thrownItemCheckFluids;

        public HitboxesConfig(ConfigurationSection section) {
            if (section != null) {
                this.secantRadius = section.getDouble("secant_radius", 0.4);

                // Load thrown item values directly
                ConfigurationSection thrownItem = section.getConfigurationSection("thrown_item");
                if (thrownItem != null) {
                    this.thrownItemEntityRadius = thrownItem.getDouble("entity_radius", 0.5);
                    this.thrownItemCheckFluids = thrownItem.getBoolean("check_fluids", false);
                } else {
                    this.thrownItemEntityRadius = 0.5;
                    this.thrownItemCheckFluids = false;
                }
            } else {
                this.secantRadius = 0.4;
                this.thrownItemEntityRadius = 0.5;
                this.thrownItemCheckFluids = false;
            }
        }
    }

    @Getter
    public static class ThrownDamageConfig {
        private final SwordAxeDamageConfig swordAxe;
        private final OtherDamageConfig other;

        public ThrownDamageConfig(ConfigurationSection section) {
            if (section != null) {
                this.swordAxe = new SwordAxeDamageConfig(section.getConfigurationSection("sword_axe"));
                this.other = new OtherDamageConfig(section.getConfigurationSection("other"));
            } else {
                this.swordAxe = new SwordAxeDamageConfig(null);
                this.other = new OtherDamageConfig(null);
            }
        }
    }

    @Getter
    public static class SwordAxeDamageConfig {
        private final int invulnerabilityTicks;
        private final int baseShards;
        private final float toughnessDamage;
        private final float soulfireReduction;
        private final double knockbackGrounded;
        private final double knockbackAirborne;

        public SwordAxeDamageConfig(ConfigurationSection section) {
            if (section != null) {
                this.invulnerabilityTicks = section.getInt("invulnerability_ticks", 0);
                this.baseShards = section.getInt("base_shards", 2);
                this.toughnessDamage = (float) section.getDouble("toughness_damage", 75.0);
                this.soulfireReduction = (float) section.getDouble("soulfire_reduction", 50.0);
                this.knockbackGrounded = section.getDouble("knockback_grounded", 0.7);
                this.knockbackAirborne = section.getDouble("knockback_airborne", 1.0);
            } else {
                this.invulnerabilityTicks = 0;
                this.baseShards = 2;
                this.toughnessDamage = 75.0f;
                this.soulfireReduction = 50.0f;
                this.knockbackGrounded = 0.7;
                this.knockbackAirborne = 1.0;
            }
        }
    }

    @Getter
    public static class OtherDamageConfig {
        private final int invulnerabilityTicks;
        private final int baseShards;
        private final float toughnessDamage;
        private final float soulfireReduction;
        private final double knockbackMultiplier;
        private final float explosionPower;

        public OtherDamageConfig(ConfigurationSection section) {
            if (section != null) {
                this.invulnerabilityTicks = section.getInt("invulnerability_ticks", 0);
                this.baseShards = section.getInt("base_shards", 2);
                this.toughnessDamage = (float) section.getDouble("toughness_damage", 75.0);
                this.soulfireReduction = (float) section.getDouble("soulfire_reduction", 50.0);
                this.knockbackMultiplier = section.getDouble("knockback_multiplier", 0.7);
                this.explosionPower = (float) section.getDouble("explosion_power", 1.0);
            } else {
                this.invulnerabilityTicks = 0;
                this.baseShards = 2;
                this.toughnessDamage = 75.0f;
                this.soulfireReduction = 50.0f;
                this.knockbackMultiplier = 0.7;
                this.explosionPower = 1.0f;
            }
        }
    }

    @Getter
    public static class ImpalementConfig {
        private final double headZoneRatio;
        private final List<EntityType> headFollowExceptions;
        private final int pinMaxIterations;
        private final int pinCheckInterval;

        public ImpalementConfig(ConfigurationSection section) {
            if (section != null) {
                this.headZoneRatio = section.getDouble("head_zone_ratio", 0.8);
                this.headFollowExceptions = section.getStringList("head_follow_exceptions").stream()
                        .map(s -> {
                            try {
                                return EntityType.valueOf(s);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                this.pinMaxIterations = section.getInt("pin_max_iterations", 50);
                this.pinCheckInterval = section.getInt("pin_check_interval", 2);
            } else {
                this.headZoneRatio = 0.8;
                this.headFollowExceptions = List.of(EntityType.SPIDER);
                this.pinMaxIterations = 50;
                this.pinCheckInterval = 2;
            }
        }
    }
}
