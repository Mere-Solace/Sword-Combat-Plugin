package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for world interaction configuration values.
 * <p>
 * Uses hybrid pattern: Simple 2-3 value groups flattened to direct fields.
 * </p>
 */
@Getter
public class WorldConfig {
    // Flattened marker placement config (2 simple values - no wrapper class needed)
    private final double markerPlacementPullbackStep;
    private final int markerPlacementMaxPullbackIterations;

    // Flattened explosions config (3 simple values - no wrapper class needed)
    private final float explosionsPower;
    private final boolean explosionsSetFire;
    private final boolean explosionsBreakBlocks;

    public WorldConfig(FileConfiguration config) {
        ConfigurationSection world = config.getConfigurationSection("world");
        if (world != null) {
            // Load marker placement values directly
            ConfigurationSection markerPlacement = world.getConfigurationSection("marker_placement");
            if (markerPlacement != null) {
                this.markerPlacementPullbackStep = markerPlacement.getDouble("pullback_step", 0.1);
                this.markerPlacementMaxPullbackIterations = markerPlacement.getInt("max_pullback_iterations", 30);
            } else {
                this.markerPlacementPullbackStep = 0.1;
                this.markerPlacementMaxPullbackIterations = 30;
            }

            // Load explosions values directly
            ConfigurationSection explosions = world.getConfigurationSection("explosions");
            if (explosions != null) {
                this.explosionsPower = (float) explosions.getDouble("power", 1.0);
                this.explosionsSetFire = explosions.getBoolean("set_fire", false);
                this.explosionsBreakBlocks = explosions.getBoolean("break_blocks", false);
            } else {
                this.explosionsPower = 1.0f;
                this.explosionsSetFire = false;
                this.explosionsBreakBlocks = false;
            }
        } else {
            this.markerPlacementPullbackStep = 0.1;
            this.markerPlacementMaxPullbackIterations = 30;
            this.explosionsPower = 1.0f;
            this.explosionsSetFire = false;
            this.explosionsBreakBlocks = false;
        }
    }
}
