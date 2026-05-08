package btm.sword.join;

import org.bukkit.Location;

import btm.sword.config.section.JoinSequenceConfig;

/**
 * Closed set of destinations a player may select from the
 * {@link btm.sword.join.menu.JoinRouterMenu}.
 *
 * <h2>Layering</h2>
 * <p>The menu surfaces user intent as one of these enum values; it never deals in raw
 * world {@link Location}s. The join lifecycle code (the future {@code JoinSession})
 * resolves a {@code Destination} to a {@code Location} via {@link #spawn()} when it is
 * about to perform the routing teleport.</p>
 *
 * <h2>Adding a new destination</h2>
 * <ol>
 *   <li>Add a new enum value here that returns its own configured {@link Location}.</li>
 *   <li>Add a matching {@link Location} field and {@code register} call in
 *       {@link JoinSequenceConfig}.</li>
 *   <li>Add the YAML default to {@code config.yaml}.</li>
 *   <li>Wire a button in {@link btm.sword.join.menu.JoinRouterMenu} that raises the new
 *       value through its {@code onSelect} callback.</li>
 * </ol>
 */
public enum Destination {

    /** The central social area — non-combat hub. */
    HUB {
        @Override public Location spawn() { return JoinSequenceConfig.DESTINATION_HUB; }
    },

    /** The matchmaking waiting room — entry into queued PvP. */
    QUICK_JOIN {
        @Override public Location spawn() { return JoinSequenceConfig.DESTINATION_QUICK_JOIN; }
    },

    /** The adventure-mode world entry point. */
    ADVENTURE {
        @Override public Location spawn() { return JoinSequenceConfig.DESTINATION_ADVENTURE; }
    },

    /** The roguelike-mode world entry point. */
    ROGUELIKE {
        @Override public Location spawn() { return JoinSequenceConfig.DESTINATION_ROGUELIKE; }
    };

    /**
     * Resolves this destination to its currently-configured world {@link Location}.
     * Reads the {@link JoinSequenceConfig} static field for the underlying value, so
     * runtime config edits via the in-game config menu take effect on the next call.
     *
     * @return the spawn location for this destination; never null assuming
     *         {@link JoinSequenceConfig} loaded successfully
     */
    public abstract Location spawn();
}
