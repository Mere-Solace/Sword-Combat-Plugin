package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for debug and developer configuration values.
 */
@Getter
public class DebugConfig {
    private final boolean profileBezierGeneration;
    private final boolean profileHitboxDetection;
    private final boolean showHitboxParticles;
    private final boolean showBezierControlPoints;

    public DebugConfig(FileConfiguration config) {
        ConfigurationSection debug = config.getConfigurationSection("debug");
        if (debug != null) {
            this.profileBezierGeneration = debug.getBoolean("profile_bezier_generation", false);
            this.profileHitboxDetection = debug.getBoolean("profile_hitbox_detection", false);
            this.showHitboxParticles = debug.getBoolean("show_hitbox_particles", false);
            this.showBezierControlPoints = debug.getBoolean("show_bezier_control_points", false);
        } else {
            this.profileBezierGeneration = false;
            this.profileHitboxDetection = false;
            this.showHitboxParticles = false;
            this.showBezierControlPoints = false;
        }
    }
}
