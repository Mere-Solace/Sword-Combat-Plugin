package btm.sword.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Type-safe accessor for detection and collision configuration values.
 */
@Getter
public class DetectionConfig {
    private final GroundCheckConfig groundCheck;

    public DetectionConfig(FileConfiguration config) {
        ConfigurationSection detection = config.getConfigurationSection("detection");
        if (detection != null) {
            this.groundCheck = new GroundCheckConfig(detection.getConfigurationSection("ground_check"));
        } else {
            this.groundCheck = new GroundCheckConfig(null);
        }
    }

    @Getter
    public static class GroundCheckConfig {
        private final double maxDistance;

        public GroundCheckConfig(ConfigurationSection section) {
            if (section != null) {
                this.maxDistance = section.getDouble("max_distance", 0.3);
            } else {
                this.maxDistance = 0.3;
            }
        }
    }
}
