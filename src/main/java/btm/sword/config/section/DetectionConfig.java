package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for detection and collision configuration values.
 * <p>
 * Uses hybrid pattern: Single value flattened to direct field (no wrapper class needed).
 * </p>
 */
@Getter
public class DetectionConfig {
    // Flattened ground check config (1 simple value - no wrapper class needed)
    private final double groundCheckMaxDistance;

    public DetectionConfig(FileConfiguration config) {
        ConfigurationSection detection = config.getConfigurationSection("detection");
        if (detection != null) {
            ConfigurationSection groundCheck = detection.getConfigurationSection("ground_check");
            if (groundCheck != null) {
                this.groundCheckMaxDistance = groundCheck.getDouble("max_distance", 0.3);
            } else {
                this.groundCheckMaxDistance = 0.3;
            }
        } else {
            this.groundCheckMaxDistance = 0.3;
        }
    }
}
