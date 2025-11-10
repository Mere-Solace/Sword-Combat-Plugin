package btm.sword.system.action;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.entity.types.Combatant;

public abstract class SwordAction {
    protected static final BukkitScheduler s = Bukkit.getScheduler();
    protected static final Plugin plugin = Sword.getInstance();

    // cast allows each sword action method to cast itself, setting the current ability (cast) task
    // of the executor, thus not allowing the executor to cast other abilities during this time.
    //
    // After the cast duration, the ability task of the executor is set to null, and then only the runnable
    // itself may cancel its operations internally.
    //
    // abilities may still be canceled internally before the cast runnable is up, though.
    protected static void cast(Combatant executor, int castDuration, Runnable action) {
        BukkitTask castTask = s.runTask(plugin, action);

        if (castDuration <= 0) return;

        executor.setCastTask(castTask);
        SwordScheduler.runLater(new BukkitRunnable() {
            @Override
            public void run() {
                if (executor.getAbilityCastTask() != null) {
                    executor.setCastTask(null);
                }
            }
        }, castDuration, TimeUnit.MILLISECONDS);
    }
}
