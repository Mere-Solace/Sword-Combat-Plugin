package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.configuration.ConfigurationSection;

import btm.sword.action.throwing.types.ThrownItem;

/**
 * Timing configuration for cooldowns, durations, and update intervals.
 * <p>
 * Controls tick-based timing for thrown items, entity updates, combat cleanup,
 * and combo windows. All values in <b>ticks</b> (20 ticks = 1 second).
 * </p>
 *
 * <h2>Common Timing Patterns</h2>
 * <ul>
 *   <li><b>Grace Periods</b> - Short windows for forgiving input timing</li>
 *   <li><b>Disposal Timeouts</b> - Entity cleanup after inactivity</li>
 *   <li><b>Update Intervals</b> - Frequency of background tasks</li>
 * </ul>
 *
 * @see ThrownItem Thrown item lifecycle
 */
public final class TimingConfig {

    private TimingConfig() {}

    // Thrown items configuration
    public static int THROWN_ITEMS_CATCH_GRACE_PERIOD = 3;
    static { register(
        "timing.thrown_items_catch_grace_period",
        THROWN_ITEMS_CATCH_GRACE_PERIOD, Integer.class,
        v -> THROWN_ITEMS_CATCH_GRACE_PERIOD = v,
        ConfigurationSection::getInt
    ); }

    public static int THROWN_ITEMS_DISPOSAL_TIMEOUT = 30000; // 30 seconds
    static { register(
        "timing.thrown_items_disposal_timeout",
        THROWN_ITEMS_DISPOSAL_TIMEOUT, Integer.class,
        v -> THROWN_ITEMS_DISPOSAL_TIMEOUT = v,
        ConfigurationSection::getInt
    ); }

    public static int THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL = 500;
    static { register(
        "timing.thrown_items_disposal_check_interval",
        THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL, Integer.class,
        v -> THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL = v,
        ConfigurationSection::getInt
    ); }

    public static int THROWN_ITEMS_PIN_DELAY = 2;
    static { register(
        "timing.thrown_items_pin_delay",
        THROWN_ITEMS_PIN_DELAY, Integer.class,
        v -> THROWN_ITEMS_PIN_DELAY = v,
        ConfigurationSection::getInt
    ); }

    public static int THROWN_ITEMS_THROW_COMPLETION_DELAY = 6;
    static { register(
        "timing.thrown_items_throw_completion_delay",
        THROWN_ITEMS_THROW_COMPLETION_DELAY, Integer.class,
        v -> THROWN_ITEMS_THROW_COMPLETION_DELAY = v,
        ConfigurationSection::getInt
    ); }

    // Intervals configuration
    public static int INTERVALS_ENTITY_TICK = 1;
    static { register(
        "timing.intervals_entity_tick",
        INTERVALS_ENTITY_TICK, Integer.class,
        v -> INTERVALS_ENTITY_TICK = v,
        ConfigurationSection::getInt
    ); }

    public static int INTERVALS_STATUS_DISPLAY_UPDATE = 5;
    static { register(
        "timing.intervals_status_display_update",
        INTERVALS_STATUS_DISPLAY_UPDATE, Integer.class,
        v -> INTERVALS_STATUS_DISPLAY_UPDATE = v,
        ConfigurationSection::getInt
    ); }

    public static int INTERVALS_COMBAT_CLEANUP = 20;
    static { register(
        "timing.intervals_combat_cleanup",
        INTERVALS_COMBAT_CLEANUP, Integer.class,
        v -> INTERVALS_COMBAT_CLEANUP = v,
        ConfigurationSection::getInt
    ); }

    // Attacks configuration
    public static int ATTACKS_COMBO_WINDOW_BASE = 3;
    static { register(
        "timing.attacks_combo_window_base",
        ATTACKS_COMBO_WINDOW_BASE, Integer.class,
        v -> ATTACKS_COMBO_WINDOW_BASE = v,
        ConfigurationSection::getInt
    ); }

    /** Delay in milliseconds between right-click inputs. */
    public static int RIGHT_INTERACT_DELAY = 1;
    static { register(
        "timing.right_interact_delay",
        RIGHT_INTERACT_DELAY, Integer.class,
        v -> RIGHT_INTERACT_DELAY = v,
        ConfigurationSection::getInt
    ); }
}
