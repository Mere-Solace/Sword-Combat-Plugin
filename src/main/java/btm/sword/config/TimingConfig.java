package btm.sword.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Type-safe accessor for timing and cooldown configuration values.
 */
@Getter
public class TimingConfig {
    private final ThrownItemsConfig thrownItems;
    private final AttacksConfig attacks;
    private final IntervalsConfig intervals;

    public TimingConfig(FileConfiguration config) {
        ConfigurationSection timing = config.getConfigurationSection("timing");
        if (timing != null) {
            this.thrownItems = new ThrownItemsConfig(timing.getConfigurationSection("thrown_items"));
            this.attacks = new AttacksConfig(timing.getConfigurationSection("attacks"));
            this.intervals = new IntervalsConfig(timing.getConfigurationSection("intervals"));
        } else {
            this.thrownItems = new ThrownItemsConfig(null);
            this.attacks = new AttacksConfig(null);
            this.intervals = new IntervalsConfig(null);
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
    public static class AttacksConfig {
        private final int comboWindowBase;

        public AttacksConfig(ConfigurationSection section) {
            if (section != null) {
                this.comboWindowBase = section.getInt("combo_window_base", 3);
            } else {
                this.comboWindowBase = 3;
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
