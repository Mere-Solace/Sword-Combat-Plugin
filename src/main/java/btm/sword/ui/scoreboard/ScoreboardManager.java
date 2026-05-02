package btm.sword.ui.scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

/**
 * Global manager for per-player {@link SwordScoreboard} instances.
 *
 * <p>Maintains a registry of active scoreboards keyed by player UUID, handling creation,
 * retrieval, and cleanup. Player disconnections are handled automatically via
 * {@link #onPlayerQuit(Player)}, which is called by the join arbiter.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create and show a scoreboard
 * SwordScoreboard board = ScoreboardManager.create(player, Component.text("HUD"));
 * board.setLine(1, Component.text("HP: 100"));
 * board.show();
 *
 * // Update a line later
 * ScoreboardManager.get(player).ifPresent(b -> b.setLine(1, Component.text("HP: 75")));
 *
 * // Remove on player quit
 * ScoreboardManager.onPlayerQuit(player);
 * }</pre>
 */
public final class ScoreboardManager {

    private static final Map<UUID, SwordScoreboard> BOARDS = new HashMap<>();

    private ScoreboardManager() {}

    /**
     * Creates a new {@link SwordScoreboard} for the player and registers it.
     * If a scoreboard already exists for this player it is replaced and destroyed.
     *
     * @param player the player to create a scoreboard for
     * @param title  the sidebar title
     * @return the newly created scoreboard
     */
    public static SwordScoreboard create(Player player, Component title) {
        remove(player);
        SwordScoreboard board = new SwordScoreboard(player, title);
        BOARDS.put(player.getUniqueId(), board);
        return board;
    }

    /**
     * Returns the active {@link SwordScoreboard} for the given player, if any.
     *
     * @param player the player to look up
     * @return an {@link Optional} containing the scoreboard, or empty if none exists
     */
    public static Optional<SwordScoreboard> get(Player player) {
        return Optional.ofNullable(BOARDS.get(player.getUniqueId()));
    }

    /**
     * Hides and destroys the scoreboard for the given player, removing it from the registry.
     * Safe to call even if no scoreboard exists for this player.
     *
     * @param player the player whose scoreboard to remove
     */
    public static void remove(Player player) {
        SwordScoreboard existing = BOARDS.remove(player.getUniqueId());
        if (existing != null) {
            existing.remove();
        }
    }

    /**
     * Cleans up the scoreboard for a player who has disconnected.
     * Called automatically by {@code ServerJoinArbiter.onPlayerQuit}.
     *
     * @param player the player who quit
     */
    public static void onPlayerQuit(Player player) {
        remove(player);
    }

    /**
     * Destroys all registered scoreboards. Called on plugin disable.
     */
    public static void removeAll() {
        BOARDS.values().forEach(SwordScoreboard::remove);
        BOARDS.clear();
    }
}
