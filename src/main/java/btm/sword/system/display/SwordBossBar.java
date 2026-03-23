package btm.sword.system.display;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import btm.sword.system.control.SwordScheduler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * A managed wrapper around an Adventure {@link BossBar} with viewer tracking and lifecycle control.
 *
 * <p>Supports progress updates, color and overlay changes, multi-viewer management, and
 * time-based auto-removal. When a player disconnects, the {@link BossBarManager} automatically
 * removes them from all active bars via {@link BossBarManager#onPlayerQuit(Player)}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Persistent bar
 * SwordBossBar bar = BossBarManager.create(Component.text("Boss HP"), 1.0f, BossBar.Color.RED);
 * bar.addViewer(player);
 * bar.setProgress(0.5f);
 *
 * // Timed bar (auto-removes after 5 seconds)
 * SwordBossBar timer = BossBarManager.createTimed(
 *     Component.text("Round starts in 5s"), 1.0f, BossBar.Color.YELLOW, 5, TimeUnit.SECONDS);
 * timer.addViewer(player);
 * }</pre>
 *
 * <p>Obtain instances via {@link BossBarManager} — do not construct directly.</p>
 */
public final class SwordBossBar {

    private final BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();
    private boolean removed;

    /**
     * Constructs a new boss bar. Use {@link BossBarManager#create} instead of calling directly.
     *
     * @param title   the initial title
     * @param progress the initial progress (0.0–1.0)
     * @param color   the bar color
     * @param overlay the segment overlay style
     */
    SwordBossBar(Component title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        this.bossBar = BossBar.bossBar(title, progress, color, overlay);
        this.removed = false;
    }

    /**
     * Sets the displayed title text.
     *
     * @param title the new title component
     */
    public void setTitle(Component title) {
        bossBar.name(title);
    }

    /**
     * Sets the fill progress of the bar. Values are clamped to {@code [0.0, 1.0]}.
     *
     * @param progress the new progress value
     */
    public void setProgress(float progress) {
        bossBar.progress(Math.max(0f, Math.min(1f, progress)));
    }

    /**
     * Sets the bar color.
     *
     * @param color the new {@link BossBar.Color}
     */
    public void setColor(BossBar.Color color) {
        bossBar.color(color);
    }

    /**
     * Sets the segment overlay style (e.g. solid, 6 notches, 10 notches, 12 notches, 20 notches).
     *
     * @param overlay the new {@link BossBar.Overlay}
     */
    public void setOverlay(BossBar.Overlay overlay) {
        bossBar.overlay(overlay);
    }

    /**
     * Shows this boss bar to the given player. Has no effect if the bar has already been removed.
     *
     * @param player the player to add as a viewer
     */
    public void addViewer(Player player) {
        if (removed) return;
        viewers.add(player.getUniqueId());
        player.showBossBar(bossBar);
    }

    /**
     * Hides this boss bar from the given player and removes them from the viewer set.
     *
     * @param player the player to remove
     */
    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.hideBossBar(bossBar);
        }
    }

    /**
     * Hides this boss bar from all current viewers.
     */
    public void clearViewers() {
        Set<UUID> snapshot = new HashSet<>(viewers);
        for (UUID uuid : snapshot) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                removeViewer(p);
            } else {
                viewers.remove(uuid);
            }
        }
    }

    /**
     * Returns whether the given player is currently a viewer of this bar.
     *
     * @param player the player to check
     * @return {@code true} if the player is a viewer
     */
    public boolean hasViewer(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    /**
     * Destroys this boss bar, clearing all viewers and unregistering it from the
     * {@link BossBarManager}. After calling this, the instance should be discarded.
     * Calling this more than once is a no-op.
     */
    public void remove() {
        if (removed) return;
        removed = true;
        clearViewers();
        BossBarManager.unregister(this);
    }

    /**
     * Schedules this boss bar to automatically remove itself after the given duration.
     *
     * @param duration the delay before removal
     * @param unit     the time unit of the delay
     */
    public void removeAfter(int duration, TimeUnit unit) {
        SwordScheduler.runBukkitTaskLater(this::remove, duration, unit);
    }

    /**
     * Schedules this boss bar to automatically remove itself after the given duration,
     * invoking {@code onComplete} just before removal.
     *
     * @param duration   the delay before removal
     * @param unit       the time unit of the delay
     * @param onComplete callback invoked with this bar immediately before it is removed
     */
    public void removeAfter(int duration, TimeUnit unit, Consumer<SwordBossBar> onComplete) {
        SwordScheduler.runBukkitTaskLater(() -> {
            onComplete.accept(this);
            remove();
        }, duration, unit);
    }

    /**
     * Returns the underlying Adventure {@link BossBar} for direct manipulation.
     *
     * @return the wrapped boss bar
     */
    public BossBar getBossBar() {
        return bossBar;
    }

    /**
     * Returns whether this bar has been removed.
     *
     * @return {@code true} if removed
     */
    public boolean isRemoved() {
        return removed;
    }

    /**
     * Returns an unmodifiable view of the UUIDs currently registered as viewers.
     *
     * @return unmodifiable set of viewer UUIDs
     */
    public Set<UUID> getViewerIds() {
        return Collections.unmodifiableSet(viewers);
    }
}
