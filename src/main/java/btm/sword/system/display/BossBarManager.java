package btm.sword.system.display;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Global manager for {@link SwordBossBar} instances.
 *
 * <p>Tracks all active boss bars and handles lifecycle events — most importantly,
 * removing players from all bars when they disconnect. Boss bars are registered
 * automatically on creation and deregistered when {@link SwordBossBar#remove()} is called.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Persistent bar shown to multiple players
 * SwordBossBar bar = BossBarManager.create(
 *     Component.text("Dungeon Boss").color(NamedTextColor.RED),
 *     1.0f,
 *     BossBar.Color.RED,
 *     BossBar.Overlay.PROGRESS
 * );
 * bar.addViewer(player1);
 * bar.addViewer(player2);
 * bar.setProgress(0.3f); // update during encounter
 * bar.remove();          // clean up when done
 *
 * // Timed countdown bar
 * SwordBossBar countdown = BossBarManager.createTimed(
 *     Component.text("Round starts in 10s"),
 *     1.0f, BossBar.Color.YELLOW, 10, TimeUnit.SECONDS
 * );
 * countdown.addViewer(player);
 * }</pre>
 *
 * <h2>Limits and notes</h2>
 * <ul>
 *   <li>Minecraft clients display up to ~20 boss bars simultaneously, though 1–3 is typical.</li>
 *   <li>Progress values are clamped to {@code [0.0, 1.0]} by {@link SwordBossBar#setProgress}.</li>
 *   <li>Available colors: {@code PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE}.</li>
 *   <li>Available overlays: {@code PROGRESS} (solid), {@code NOTCHED_6}, {@code NOTCHED_10},
 *       {@code NOTCHED_12}, {@code NOTCHED_20}.</li>
 * </ul>
 */
public final class BossBarManager {

    private static final List<SwordBossBar> activeBars = new ArrayList<>();

    private BossBarManager() {}

    /**
     * Creates and registers a new boss bar with a solid progress overlay.
     *
     * @param title    the display title
     * @param progress the initial fill progress (0.0–1.0)
     * @param color    the bar color
     * @return the newly created boss bar
     */
    public static SwordBossBar create(Component title, float progress, BossBar.Color color) {
        return create(title, progress, color, BossBar.Overlay.PROGRESS);
    }

    /**
     * Creates and registers a new boss bar.
     *
     * @param title    the display title
     * @param progress the initial fill progress (0.0–1.0)
     * @param color    the bar color
     * @param overlay  the segment overlay style
     * @return the newly created boss bar
     */
    public static SwordBossBar create(Component title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        SwordBossBar bar = new SwordBossBar(title, progress, color, overlay);
        activeBars.add(bar);
        return bar;
    }

    /**
     * Creates and registers a boss bar that automatically removes itself after the given duration.
     *
     * @param title    the display title
     * @param progress the initial fill progress (0.0–1.0)
     * @param color    the bar color
     * @param duration the delay before auto-removal
     * @param unit     the time unit of the delay
     * @return the newly created timed boss bar
     */
    public static SwordBossBar createTimed(
            Component title, float progress, BossBar.Color color, int duration, TimeUnit unit) {
        SwordBossBar bar = create(title, progress, color);
        bar.removeAfter(duration, unit);
        return bar;
    }

    /**
     * Removes the given player from all active boss bars they are viewing.
     * Called automatically by {@code ServerJoinArbiter.onPlayerQuit}.
     *
     * @param player the player who disconnected
     */
    public static void onPlayerQuit(Player player) {
        for (SwordBossBar bar : activeBars) {
            if (bar.hasViewer(player)) {
                bar.removeViewer(player);
            }
        }
    }

    /**
     * Destroys all active boss bars. Called on plugin disable.
     */
    public static void removeAll() {
        List<SwordBossBar> snapshot = new ArrayList<>(activeBars);
        snapshot.forEach(SwordBossBar::remove);
        activeBars.clear();
    }

    /**
     * Internal: called by {@link SwordBossBar#remove()} to deregister the bar.
     * Not intended for external use.
     *
     * @param bar the bar to deregister
     */
    static void unregister(SwordBossBar bar) {
        activeBars.remove(bar);
    }
}
