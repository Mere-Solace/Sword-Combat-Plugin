package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;

import lombok.Getter;

/**
 * Type-safe accessor for display and visual effect configuration values.
 * <p>
 * Uses hybrid pattern: Simple 2-3 value groups flattened to direct fields,
 * complex groups like BleedConfig (4 fields) kept nested.
 * </p>
 */
@Getter
public class DisplayConfig {
    private final int defaultTeleportDuration;
    private final ItemDisplayFollowConfig itemDisplayFollow;
    private final ParticlesConfig particles;

    // Flattened status display brightness (2 simple values)
    private final int statusDisplayBlockBrightness;
    private final int statusDisplaySkyBrightness;

    // Flattened bezier config (merged with particle thresholds - 6 related values)
    private final int bezierNumSteps;
    private final double bezierParticleThresholdLayer1;
    private final double bezierParticleThresholdLayer2;
    private final double bezierParticleThresholdLayer3;
    private final double bezierParticleThresholdLayer4;
    private final double bezierParticleThresholdLayer5;

    public DisplayConfig(FileConfiguration config) {
        ConfigurationSection display = config.getConfigurationSection("display");
        if (display != null) {
            this.defaultTeleportDuration = display.getInt("default_teleport_duration", 2);
            this.statusDisplayBlockBrightness = display.getInt("status_display_block_brightness", 15);
            this.statusDisplaySkyBrightness = display.getInt("status_display_sky_brightness", 15);
            this.itemDisplayFollow = new ItemDisplayFollowConfig(display.getConfigurationSection("item_display_follow"));
            this.particles = new ParticlesConfig(display.getConfigurationSection("particles"));

            // Load bezier values directly (merged BezierConfig and ParticleThresholdsConfig)
            ConfigurationSection bezier = display.getConfigurationSection("bezier");
            if (bezier != null) {
                this.bezierNumSteps = bezier.getInt("num_steps", 50);
                ConfigurationSection particleThresholds = bezier.getConfigurationSection("particle_thresholds");
                if (particleThresholds != null) {
                    this.bezierParticleThresholdLayer1 = particleThresholds.getDouble("layer_1", 0.1);
                    this.bezierParticleThresholdLayer2 = particleThresholds.getDouble("layer_2", 0.3);
                    this.bezierParticleThresholdLayer3 = particleThresholds.getDouble("layer_3", 0.5);
                    this.bezierParticleThresholdLayer4 = particleThresholds.getDouble("layer_4", 0.625);
                    this.bezierParticleThresholdLayer5 = particleThresholds.getDouble("layer_5", 0.75);
                } else {
                    this.bezierParticleThresholdLayer1 = 0.1;
                    this.bezierParticleThresholdLayer2 = 0.3;
                    this.bezierParticleThresholdLayer3 = 0.5;
                    this.bezierParticleThresholdLayer4 = 0.625;
                    this.bezierParticleThresholdLayer5 = 0.75;
                }
            } else {
                this.bezierNumSteps = 50;
                this.bezierParticleThresholdLayer1 = 0.1;
                this.bezierParticleThresholdLayer2 = 0.3;
                this.bezierParticleThresholdLayer3 = 0.5;
                this.bezierParticleThresholdLayer4 = 0.625;
                this.bezierParticleThresholdLayer5 = 0.75;
            }
        } else {
            this.defaultTeleportDuration = 2;
            this.statusDisplayBlockBrightness = 15;
            this.statusDisplaySkyBrightness = 15;
            this.itemDisplayFollow = new ItemDisplayFollowConfig(null);
            this.particles = new ParticlesConfig(null);
            this.bezierNumSteps = 50;
            this.bezierParticleThresholdLayer1 = 0.1;
            this.bezierParticleThresholdLayer2 = 0.3;
            this.bezierParticleThresholdLayer3 = 0.5;
            this.bezierParticleThresholdLayer4 = 0.625;
            this.bezierParticleThresholdLayer5 = 0.75;
        }
    }

    @Getter
    public static class ItemDisplayFollowConfig {
        private final int updateInterval;
        private final int particleInterval;
        private final Display.Billboard billboardMode;

        public ItemDisplayFollowConfig(ConfigurationSection section) {
            if (section != null) {
                this.updateInterval = section.getInt("update_interval", 2);
                this.particleInterval = section.getInt("particle_interval", 4);
                String billboardStr = section.getString("billboard_mode", "FIXED");
                this.billboardMode = Display.Billboard.valueOf(billboardStr);
            } else {
                this.updateInterval = 2;
                this.particleInterval = 4;
                this.billboardMode = Display.Billboard.FIXED;
            }
        }
    }

    @Getter
    public static class ParticlesConfig {
        private final BleedConfig bleed;

        // Flattened trail config (2 simple values - no wrapper class needed)
        private final int trailDisplayInterval;
        private final int trailBlockTrailInterval;

        // Flattened grounded marker config (2 simple values - no wrapper class needed)
        private final int groundedMarkerUpdateInterval;
        private final double groundedMarkerOffsetStep;

        public ParticlesConfig(ConfigurationSection section) {
            if (section != null) {
                this.bleed = new BleedConfig(section.getConfigurationSection("bleed"));

                // Load trail values directly
                ConfigurationSection trail = section.getConfigurationSection("trail");
                if (trail != null) {
                    this.trailDisplayInterval = trail.getInt("display_interval", 1);
                    this.trailBlockTrailInterval = trail.getInt("block_trail_interval", 3);
                } else {
                    this.trailDisplayInterval = 1;
                    this.trailBlockTrailInterval = 3;
                }

                // Load grounded marker values directly
                ConfigurationSection groundedMarker = section.getConfigurationSection("grounded_marker");
                if (groundedMarker != null) {
                    this.groundedMarkerUpdateInterval = groundedMarker.getInt("update_interval", 5);
                    this.groundedMarkerOffsetStep = groundedMarker.getDouble("offset_step", 0.1);
                } else {
                    this.groundedMarkerUpdateInterval = 5;
                    this.groundedMarkerOffsetStep = 0.1;
                }
            } else {
                this.bleed = new BleedConfig(null);
                this.trailDisplayInterval = 1;
                this.trailBlockTrailInterval = 3;
                this.groundedMarkerUpdateInterval = 5;
                this.groundedMarkerOffsetStep = 0.1;
            }
        }
    }

    @Getter
    public static class BleedConfig {
        private final double lineLength;
        private final double lineWidth;
        private final double stickLength;
        private final double stickWidth;

        public BleedConfig(ConfigurationSection section) {
            if (section != null) {
                this.lineLength = section.getDouble("line_length", 0.75);
                this.lineWidth = section.getDouble("line_width", 0.25);
                this.stickLength = section.getDouble("stick_length", 0.3);
                this.stickWidth = section.getDouble("stick_width", 0.25);
            } else {
                this.lineLength = 0.75;
                this.lineWidth = 0.25;
                this.stickLength = 0.3;
                this.stickWidth = 0.25;
            }
        }
    }
}
