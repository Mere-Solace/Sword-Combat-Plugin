package btm.sword.system.action;


import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import btm.sword.Sword;

public abstract class SwordAction {

    protected SwordAction() {}
    protected static final BukkitScheduler S = Bukkit.getScheduler();
    protected static final Plugin PLUGIN = Sword.getInstance();

    // cast allows each sword action method to cast itself, setting the current ability (cast) task
    // of the executor, thus not allowing the executor to cast other abilities during this time.
    //
    // After the cast duration, the ability task of the executor is set to null, and then only the runnable
    // itself may cancel its operations internally.
    //
    // abilities may still be canceled internally before the cast runnable is up, though.

    // Casting logic has been moved to {@link ActionCaster} to centralize cast scheduling and
    // make casting independent from the abstract action class. Callers should use
    // `ActionCaster.cast(executor, durationMillis, runnable)` instead.
}
