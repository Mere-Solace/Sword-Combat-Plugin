package btm.sword.system.action;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.Combatant;

/**
 * Centralized location for "casting" logic (scheduling an ability to block other actions
 * for a given duration). This replaces the previous responsibility in {@code SwordAction}.
 */
public final class ActionCaster {
    private static final Plugin plugin = Sword.getInstance();

    private ActionCaster() {}

    /**
     * Executes {@code action} on the next server tick and, if {@code castDurationMillis > 0},
     * stores the resulting {@link BukkitTask} on {@code executor} to block further actions
     * for the specified duration (scaled by the global time arbiter).
     *
     * @param executor          the combatant performing the cast
     * @param castDurationMillis how long (in ms) to lock the executor after casting; {@code <= 0}
     *                          means no lock is applied
     * @param action            the action payload to run on the next tick
     */
    public static void cast(Combatant executor, int castDurationMillis, Runnable action) {
        BukkitTask castTask = Bukkit.getScheduler().runTask(plugin, action);

        if (castDurationMillis <= 0) return;

        executor.setCastTask(castTask);
        SwordScheduler.runBukkitTaskLater(() -> {
            if (executor.getAbilityCastTask() != null) {
                executor.setCastTask(null);
            }
            // Use Global Time Scale here to effect cast times.
        }, (int) (castDurationMillis / TimeArbiter.getGLOBAL_TIME_SCALE()), TimeUnit.MILLISECONDS);
    }
}
