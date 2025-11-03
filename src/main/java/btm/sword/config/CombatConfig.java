package btm.sword.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Type-safe accessor for combat-related configuration values.
 * <p>
 * Handles damage, knockback, hitboxes, attack ranges, and combat mechanics.
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
        private final CastTimingConfig castTiming;
        private final int durationMultiplier;

        public AttacksConfig(ConfigurationSection section) {
            if (section != null) {
                this.baseDamage = section.getDouble("base_damage", 20.0);
                this.rangeMultipliers = new RangeMultipliersConfig(section.getConfigurationSection("range_multipliers"));
                this.downAirThreshold = section.getDouble("down_air_threshold", -0.5);
                this.castTiming = new CastTimingConfig(section.getConfigurationSection("cast_timing"));
                this.durationMultiplier = section.getInt("duration_multiplier", 500);
            } else {
                this.baseDamage = 20.0;
                this.rangeMultipliers = new RangeMultipliersConfig(null);
                this.downAirThreshold = -0.5;
                this.castTiming = new CastTimingConfig(null);
                this.durationMultiplier = 500;
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
    public static class CastTimingConfig {
        private final long minDuration;
        private final long maxDuration;
        private final double reductionRate;

        public CastTimingConfig(ConfigurationSection section) {
            if (section != null) {
                this.minDuration = section.getLong("min_duration", 1L);
                this.maxDuration = section.getLong("max_duration", 3L);
                this.reductionRate = section.getDouble("reduction_rate", 0.2);
            } else {
                this.minDuration = 1L;
                this.maxDuration = 3L;
                this.reductionRate = 0.2;
            }
        }
    }

    @Getter
    public static class HitboxesConfig {
        private final double secantRadius;
        private final ThrownItemHitboxConfig thrownItem;

        public HitboxesConfig(ConfigurationSection section) {
            if (section != null) {
                this.secantRadius = section.getDouble("secant_radius", 0.4);
                this.thrownItem = new ThrownItemHitboxConfig(section.getConfigurationSection("thrown_item"));
            } else {
                this.secantRadius = 0.4;
                this.thrownItem = new ThrownItemHitboxConfig(null);
            }
        }
    }

    @Getter
    public static class ThrownItemHitboxConfig {
        private final double entityRadius;
        private final boolean checkFluids;

        public ThrownItemHitboxConfig(ConfigurationSection section) {
            if (section != null) {
                this.entityRadius = section.getDouble("entity_radius", 0.5);
                this.checkFluids = section.getBoolean("check_fluids", false);
            } else {
                this.entityRadius = 0.5;
                this.checkFluids = false;
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
                        .filter(t -> t != null)
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
