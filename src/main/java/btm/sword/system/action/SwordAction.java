package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

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
	protected static void cast(Combatant executor, long castDuration, Runnable action) {
		executor.message("Casting dis ting");
		executor.setCastTask(s.runTask(plugin, action));
		new BukkitRunnable() {
			@Override
			public void run() {
				if (executor.getAbilityCastTask() != null) {
					executor.message("Stopped Casting dis ting, ");
					executor.setCastTask(null);
				}
			}
		}.runTaskLater(plugin, castDuration);
	}
}
