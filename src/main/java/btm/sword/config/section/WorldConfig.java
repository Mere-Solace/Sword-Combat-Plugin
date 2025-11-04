package btm.sword.config.section;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Type-safe accessor for world interaction configuration values.
 */
@Getter
public class WorldConfig {
    private final MarkerPlacementConfig markerPlacement;
    private final ExplosionsConfig explosions;

    public WorldConfig(FileConfiguration config) {
        ConfigurationSection world = config.getConfigurationSection("world");
        if (world != null) {
            this.markerPlacement = new MarkerPlacementConfig(world.getConfigurationSection("marker_placement"));
            this.explosions = new ExplosionsConfig(world.getConfigurationSection("explosions"));
        } else {
            this.markerPlacement = new MarkerPlacementConfig(null);
            this.explosions = new ExplosionsConfig(null);
        }
    }

    @Getter
    public static class MarkerPlacementConfig {
        private final double pullbackStep;
        private final int maxPullbackIterations;

        public MarkerPlacementConfig(ConfigurationSection section) {
            if (section != null) {
                this.pullbackStep = section.getDouble("pullback_step", 0.1);
                this.maxPullbackIterations = section.getInt("max_pullback_iterations", 30);
            } else {
                this.pullbackStep = 0.1;
                this.maxPullbackIterations = 30;
            }
        }
    }

    @Getter
    public static class ExplosionsConfig {
        private final float power;
        private final boolean setFire;
        private final boolean breakBlocks;

        public ExplosionsConfig(ConfigurationSection section) {
            if (section != null) {
                this.power = (float) section.getDouble("power", 1.0);
                this.setFire = section.getBoolean("set_fire", false);
                this.breakBlocks = section.getBoolean("break_blocks", false);
            } else {
                this.power = 1.0f;
                this.setFire = false;
                this.breakBlocks = false;
            }
        }
    }
}
