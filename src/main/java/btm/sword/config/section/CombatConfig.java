package btm.sword.config.section;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import lombok.Getter;

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
    private final AttackClassConfig attackClass;

    public CombatConfig(FileConfiguration config) {
        ConfigurationSection combat = config.getConfigurationSection("combat");
        if (combat != null) {
            this.attacks = new AttacksConfig(combat.getConfigurationSection("attacks"));
            this.hitboxes = new HitboxesConfig(combat.getConfigurationSection("hitboxes"));
            this.thrownDamage = new ThrownDamageConfig(combat.getConfigurationSection("thrown_damage"));
            this.impalement = new ImpalementConfig(combat.getConfigurationSection("impalement"));
            this.attackClass = new AttackClassConfig(combat.getConfigurationSection("attack_class"));
        } else {
            this.attacks = new AttacksConfig(null);
            this.hitboxes = new HitboxesConfig(null);
            this.thrownDamage = new ThrownDamageConfig(null);
            this.impalement = new ImpalementConfig(null);
            this.attackClass = new AttackClassConfig(null);
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

    @Getter
    public static class AttackClassConfig {
        private final ModifiersConfig modifiers;
        private final TimingConfig timing;
        private final DisplayConfig display;
        private final MotionConfig motion;
        private final EffectsConfig effects;

        public AttackClassConfig(ConfigurationSection section) {
            if (section != null) {
                this.timing = new TimingConfig(section.getConfigurationSection("timing"));
                this.display = new DisplayConfig(section.getConfigurationSection("display"));
                this.motion = new MotionConfig(section.getConfigurationSection("motion"));
                this.effects = new EffectsConfig(section.getConfigurationSection("effects"));
                this.modifiers = new ModifiersConfig(section.getConfigurationSection("modifiers"));
            } else {
                this.timing = new TimingConfig(null);
                this.display = new DisplayConfig(null);
                this.motion = new MotionConfig(null);
                this.effects = new EffectsConfig(null);
                this.modifiers = new ModifiersConfig(null);
            }
        }

        @Getter
        public static class ModifiersConfig {
            private final double rangeMultiplier;

            public ModifiersConfig(ConfigurationSection section) {
                if (section != null) {
                    this.rangeMultiplier = section.getDouble("range_multiplier");
                } else {
                  this.rangeMultiplier = 2.0;
                }
            }
        }

        /**
         * Timing configuration for each attack phase (windup, attack, recovery).
         * Controls how long animations and active hitboxes last.
         * <p>
         * Each field corresponds directly to a timing value in milliseconds or a Bezier t-parameter.
         */
        @Getter
        public static class TimingConfig {
            private final int windupDuration;
            private final int windupIterations;
            private final double windupStartValue;
            private final double windupEndValue;
            private final int attackDuration;
            private final int attackIterations;
            private final double attackStartValue;
            private final double attackEndValue;
            private final int recoveryDuration;

            public TimingConfig(ConfigurationSection section) {
                if (section != null) {
                    this.windupDuration = section.getInt("windup_duration", 1000);
                    this.windupIterations = section.getInt("windup_iterations", 5);
                    this.windupStartValue = section.getDouble("windup_start_value", 0.25);
                    this.windupEndValue = section.getDouble("windup_end_value", -0.25);
                    this.attackDuration = section.getInt("attack_duration", 750);
                    this.attackIterations = section.getInt("attack_iterations", 5);
                    this.attackStartValue = section.getDouble("attack_start_value", 0.0);
                    this.attackEndValue = section.getDouble("attack_end_value", 1.0);
                    this.recoveryDuration = section.getInt("recovery_duration", 250);
                } else {
                    this.windupDuration = 1000;
                    this.windupIterations = 5;
                    this.windupStartValue = 0.25;
                    this.windupEndValue = -0.25;
                    this.attackDuration = 750;
                    this.attackIterations = 5;
                    this.attackStartValue = 0.0;
                    this.attackEndValue = 1.0;
                    this.recoveryDuration = 250;
                }
            }
        }

        /**
         * Visual transformation configuration for weapon display entities.
         * Defines scaling, rotation, and glow color of the weapon during attacks.
         */
        @Getter
        public static class DisplayConfig {
            private final float offsetX;
            private final float offsetY;
            private final float offsetZ;
            private final float scaleX;
            private final float scaleY;
            private final float scaleZ;
            private final float rotationY;
            private final float rotationZ;
            private final float rotationX;
            private final String glowColor;
            private final String attackGlowColor;

            public DisplayConfig(ConfigurationSection section) {
                if (section != null) {
                    ConfigurationSection offset = section.getConfigurationSection("offset");
                    if (offset != null) {
                        this.offsetX = (float) offset.getDouble("x", 0.0);
                        this.offsetY = (float) offset.getDouble("y", 0.0);
                        this.offsetZ = (float) offset.getDouble("z", 1.0);
                    } else {
                        this.offsetX = 0.0f;
                        this.offsetY = 0.0f;
                        this.offsetZ = 1.0f;
                    }

                    ConfigurationSection scale = section.getConfigurationSection("scale");
                    if (scale != null) {
                        this.scaleX = (float) scale.getDouble("x", 3.0);
                        this.scaleY = (float) scale.getDouble("y", 2.5);
                        this.scaleZ = (float) scale.getDouble("z", 2.0);
                    } else {
                        this.scaleX = 3.0f;
                        this.scaleY = 2.5f;
                        this.scaleZ = 2.0f;
                    }

                    this.rotationY = (float) section.getDouble("rotation_y", 1.5708);
                    this.rotationZ = (float) section.getDouble("rotation_z", 1.5708);
                    this.rotationX = (float) section.getDouble("rotation_x", 1.5708);
                    this.glowColor = section.getString("glow_color", "#000000");
                    this.attackGlowColor = section.getString("attack_glow_color", "#FF0000");
                } else {
                    this.offsetX = 0.0f;
                    this.offsetY = 0.0f;
                    this.offsetZ = 1.0f;
                    this.scaleX = 3.0f;
                    this.scaleY = 2.5f;
                    this.scaleZ = 2.0f;
                    this.rotationY = 1.5708f;
                    this.rotationZ = 1.5708f;
                    this.rotationX = 1.5708f;
                    this.glowColor = "#000000";
                    this.attackGlowColor = "#FF0000";
                }
            }
        }

        /**
         * Motion configuration for interpolation and smoothness of weapon movement.
         * Controls how many interpolation steps are used when teleporting or animating displays.
         */
        @Getter
        public static class MotionConfig {
            private final int displaySmoothSteps;

            public MotionConfig(ConfigurationSection section) {
                this.displaySmoothSteps = (section != null)
                        ? section.getInt("display_smooth_steps", 2)
                        : 2;
            }
        }

        /**
         * Self-applied effects configuration.
         * Controls potion effects applied to the executor during specific phases.
         */
        @Getter
        public static class EffectsConfig {
            private final int windupSlownessAmplifier;
            private final int attackSlownessAmplifier;

            public EffectsConfig(ConfigurationSection section) {
                if (section != null) {
                    this.windupSlownessAmplifier = section.getInt("windup_slowness_amplifier", 4);
                    this.attackSlownessAmplifier = section.getInt("attack_slowness_amplifier", 7);
                } else {
                    this.windupSlownessAmplifier = 4;
                    this.attackSlownessAmplifier = 7;
                }
            }
        }
    }
}
