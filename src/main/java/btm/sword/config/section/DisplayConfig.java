package btm.sword.config.section;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;

/**
 * Type-safe accessor for display and visual effect configuration values.
 */
@Getter
public class DisplayConfig {
    private final int defaultTeleportDuration;
    private final ItemDisplayFollowConfig itemDisplayFollow;
    private final ParticlesConfig particles;
    private final BezierConfig bezier;

    public DisplayConfig(FileConfiguration config) {
        ConfigurationSection display = config.getConfigurationSection("display");
        if (display != null) {
            this.defaultTeleportDuration = display.getInt("default_teleport_duration", 2);
            this.itemDisplayFollow = new ItemDisplayFollowConfig(display.getConfigurationSection("item_display_follow"));
            this.particles = new ParticlesConfig(display.getConfigurationSection("particles"));
            this.bezier = new BezierConfig(display.getConfigurationSection("bezier"));
        } else {
            this.defaultTeleportDuration = 2;
            this.itemDisplayFollow = new ItemDisplayFollowConfig(null);
            this.particles = new ParticlesConfig(null);
            this.bezier = new BezierConfig(null);
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
        private final TrailConfig trail;
        private final GroundedMarkerConfig groundedMarker;

        public ParticlesConfig(ConfigurationSection section) {
            if (section != null) {
                this.bleed = new BleedConfig(section.getConfigurationSection("bleed"));
                this.trail = new TrailConfig(section.getConfigurationSection("trail"));
                this.groundedMarker = new GroundedMarkerConfig(section.getConfigurationSection("grounded_marker"));
            } else {
                this.bleed = new BleedConfig(null);
                this.trail = new TrailConfig(null);
                this.groundedMarker = new GroundedMarkerConfig(null);
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

    @Getter
    public static class TrailConfig {
        private final int displayInterval;
        private final int blockTrailInterval;

        public TrailConfig(ConfigurationSection section) {
            if (section != null) {
                this.displayInterval = section.getInt("display_interval", 1);
                this.blockTrailInterval = section.getInt("block_trail_interval", 3);
            } else {
                this.displayInterval = 1;
                this.blockTrailInterval = 3;
            }
        }
    }

    @Getter
    public static class GroundedMarkerConfig {
        private final int updateInterval;
        private final double offsetStep;

        public GroundedMarkerConfig(ConfigurationSection section) {
            if (section != null) {
                this.updateInterval = section.getInt("update_interval", 5);
                this.offsetStep = section.getDouble("offset_step", 0.1);
            } else {
                this.updateInterval = 5;
                this.offsetStep = 0.1;
            }
        }
    }

    @Getter
    public static class BezierConfig {
        private final int numSteps;
        private final ParticleThresholdsConfig particleThresholds;

        public BezierConfig(ConfigurationSection section) {
            if (section != null) {
                this.numSteps = section.getInt("num_steps", 50);
                this.particleThresholds = new ParticleThresholdsConfig(section.getConfigurationSection("particle_thresholds"));
            } else {
                this.numSteps = 50;
                this.particleThresholds = new ParticleThresholdsConfig(null);
            }
        }
    }

    @Getter
    public static class ParticleThresholdsConfig {
        private final double layer1;
        private final double layer2;
        private final double layer3;
        private final double layer4;
        private final double layer5;

        public ParticleThresholdsConfig(ConfigurationSection section) {
            if (section != null) {
                this.layer1 = section.getDouble("layer_1", 0.1);
                this.layer2 = section.getDouble("layer_2", 0.3);
                this.layer3 = section.getDouble("layer_3", 0.5);
                this.layer4 = section.getDouble("layer_4", 0.625);
                this.layer5 = section.getDouble("layer_5", 0.75);
            } else {
                this.layer1 = 0.1;
                this.layer2 = 0.3;
                this.layer3 = 0.5;
                this.layer4 = 0.625;
                this.layer5 = 0.75;
            }
        }
    }
}
