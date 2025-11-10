package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for timing and cooldown configuration values.
 * <p>
 * Uses hybrid pattern: Single value flattened to direct field, complex groups kept nested.
 * </p>
 */
@Getter
public class TimingConfig {
    private final ThrownItemsConfig thrownItems;
    private final IntervalsConfig intervals;

    // Flattened attacks config (1 simple value - no wrapper class needed)
    private final int attacksComboWindowBase;

    public TimingConfig(FileConfiguration config) {
        ConfigurationSection timing = config.getConfigurationSection("timing");
        if (timing != null) {
            this.thrownItems = new ThrownItemsConfig(timing.getConfigurationSection("thrown_items"));
            this.intervals = new IntervalsConfig(timing.getConfigurationSection("intervals"));

            // Load attacks value directly
            ConfigurationSection attacks = timing.getConfigurationSection("attacks");
            if (attacks != null) {
                this.attacksComboWindowBase = attacks.getInt("combo_window_base", 3);
            } else {
                this.attacksComboWindowBase = 3;
            }
        } else {
            this.thrownItems = new ThrownItemsConfig(null);
            this.intervals = new IntervalsConfig(null);
            this.attacksComboWindowBase = 3;
        }
    }

    @Getter
    public static class ThrownItemsConfig {
        private final int catchGracePeriod;
        private final int disposalTimeout;
        private final int disposalCheckInterval;
        private final int pinDelay;
        private final int throwCompletionDelay;

        public ThrownItemsConfig(ConfigurationSection section) {
            if (section != null) {
                this.catchGracePeriod = section.getInt("catch_grace_period", 20);
                this.disposalTimeout = section.getInt("disposal_timeout", 1000);
                this.disposalCheckInterval = section.getInt("disposal_check_interval", 5);
                this.pinDelay = section.getInt("pin_delay", 3);
                this.throwCompletionDelay = section.getInt("throw_completion_delay", 2);
            } else {
                this.catchGracePeriod = 20;
                this.disposalTimeout = 1000;
                this.disposalCheckInterval = 5;
                this.pinDelay = 3;
                this.throwCompletionDelay = 2;
            }
        }
    }

    @Getter
    public static class IntervalsConfig {
        private final int itemMotionUpdate;
        private final int displayFollowUpdate;
        private final int pinCheck;
        private final int markerParticle;
        private final int impalementCheck;

        public IntervalsConfig(ConfigurationSection section) {
            if (section != null) {
                this.itemMotionUpdate = section.getInt("item_motion_update", 1);
                this.displayFollowUpdate = section.getInt("display_follow_update", 2);
                this.pinCheck = section.getInt("pin_check", 2);
                this.markerParticle = section.getInt("marker_particle", 5);
                this.impalementCheck = section.getInt("impalement_check", 1);
            } else {
                this.itemMotionUpdate = 1;
                this.displayFollowUpdate = 2;
                this.pinCheck = 2;
                this.markerParticle = 5;
                this.impalementCheck = 1;
            }
        }
    }
}
