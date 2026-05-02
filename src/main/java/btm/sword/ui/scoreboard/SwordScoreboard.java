package btm.sword.ui.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;

/**
 * A per-player sidebar scoreboard wrapper with easy line management.
 *
 * <p>Manages a dedicated {@link Scoreboard} and SIDEBAR {@link Objective} for a single player.
 * Lines are numbered top-down (line 1 = topmost, line {@value #MAX_LINES} = bottommost) and
 * support full Adventure {@link Component} formatting. Showing and hiding is handled by
 * swapping the player's active scoreboard.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * SwordScoreboard board = ScoreboardManager.create(player, Component.text("Combat HUD"));
 * board.setLine(1, Component.text("Health: 100").color(NamedTextColor.RED));
 * board.setLine(2, Component.empty());
 * board.setLine(3, Component.text("Soulfire: 50").color(NamedTextColor.BLUE));
 * board.show();
 * }</pre>
 *
 * <p>Obtain instances via {@link ScoreboardManager#create} — do not construct directly.</p>
 */
public final class SwordScoreboard {

    /** Maximum number of sidebar lines supported by Minecraft's scoreboard. */
    public static final int MAX_LINES = 15;

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private boolean visible;

    /**
     * Constructs a new scoreboard for the given player.
     * Use {@link ScoreboardManager#create} instead of calling this directly.
     *
     * @param player the player who owns this scoreboard
     * @param title  the initial sidebar title
     */
    SwordScoreboard(Player player, Component title) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("sword_hud", Criteria.DUMMY, title, RenderType.INTEGER);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.numberFormat(NumberFormat.blank());
        this.visible = false;
    }

    /**
     * Updates the sidebar title.
     *
     * @param title the new title component
     */
    public void setTitle(Component title) {
        objective.displayName(title);
    }

    /**
     * Sets a line of text on the scoreboard.
     *
     * <p>Line 1 is the topmost row; line {@value #MAX_LINES} is the bottommost. Passing
     * {@link Component#empty()} creates a blank spacer line. Has no effect if {@code line}
     * is outside {@code [1, MAX_LINES]}.</p>
     *
     * @param line the line number (1 = top, {@value #MAX_LINES} = bottom)
     * @param text the component to display
     */
    public void setLine(int line, Component text) {
        if (line < 1 || line > MAX_LINES) return;
        Score score = objective.getScore(entryName(line));
        score.setScore(MAX_LINES - line);
        score.customName(text);
    }

    /**
     * Removes a single line from the scoreboard, leaving that row empty.
     *
     * @param line the line number to remove (1–{@value #MAX_LINES})
     */
    public void clearLine(int line) {
        if (line < 1 || line > MAX_LINES) return;
        scoreboard.resetScores(entryName(line));
    }

    /**
     * Removes all lines from the scoreboard, leaving it empty.
     */
    public void clear() {
        for (int i = 1; i <= MAX_LINES; i++) {
            clearLine(i);
        }
    }

    /**
     * Shows this scoreboard to the player by setting it as their active scoreboard.
     */
    public void show() {
        player.setScoreboard(scoreboard);
        visible = true;
    }

    /**
     * Hides this scoreboard by restoring the player to the main server scoreboard.
     * Has no effect if the player is offline.
     */
    public void hide() {
        if (player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        visible = false;
    }

    /**
     * Hides and permanently destroys this scoreboard, unregistering its objective.
     * After calling this, the instance should be discarded.
     */
    public void remove() {
        hide();
        objective.unregister();
    }

    /**
     * Returns whether this scoreboard is currently shown to the player.
     *
     * @return {@code true} if visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns the player this scoreboard belongs to.
     *
     * @return the owning player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Produces a stable, unique internal entry name for the given line slot.
     * These names are never shown to players directly when {@code customName} is set.
     */
    private static String entryName(int line) {
        return "sword_line_" + line;
    }
}
