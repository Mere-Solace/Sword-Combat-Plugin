package btm.sword.listeners;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;

import btm.sword.Sword;
import btm.sword.system.control.SwordScheduler;
import btm.sword.utility.SwordTimeUnit;

/**
 * Central error-handling listener for the Sword plugin.
 * <p>
 * Captures {@link ServerExceptionEvent} and logs it with full context so server errors
 * are never silently swallowed.  Also exposes {@link #safeRemove(Entity)}, a drop-in
 * replacement for {@link Entity#remove()} that catches Paper's chunk-system guard
 * ({@code "prevented from being removed … processing section status updates"}) and
 * automatically defers the removal to the following tick.
 * </p>
 */
public class ErrorListener implements Listener {

    /**
     * Logs every unhandled server exception with its source, message, and stack trace.
     *
     * @param event the server exception event fired by Paper
     */
    @EventHandler
    public void onServerException(ServerExceptionEvent event) {
        Throwable ex = event.getException();
        Sword.getInstance().getLogger().severe(
            "[ErrorListener] Unhandled server exception (" + ex.getClass().getSimpleName() + "): " + ex.getMessage()
        );
        ex.printStackTrace();
    }

    // -----------------------------------------------------------------------
    // Static utilities

    /**
     * Attempts to remove an entity immediately.  If Paper's chunk-system guard prevents
     * the removal (entity is mid-section-status-update), the call is deferred one tick
     * and a warning is logged.
     *
     * <p>Safe to call with {@code null} or already-dead entities — both are no-ops.</p>
     *
     * @param entity the entity to remove, or {@code null}
     */
    public static void safeRemove(Entity entity) {
        if (entity == null || !entity.isValid()) return;
        try {
            entity.remove();
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[ErrorListener] Could not remove " + entity.getType()
                    + " (uuid=" + entity.getUniqueId() + ") immediately — deferring 1 tick. Reason: " + e.getMessage()
            );
            SwordScheduler.runBukkitTaskLater(entity::remove, SwordTimeUnit.MILLISECONDS_PER_TICK, TimeUnit.MILLISECONDS);
        }
    }
}
