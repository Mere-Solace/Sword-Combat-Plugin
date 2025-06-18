package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public abstract class SwordAction {
	protected static final BukkitScheduler s = Bukkit.getScheduler();
	protected static final Plugin plugin = Sword.getInstance();
	
	protected static void disassociateTask(Combatant executor) {
		executor.entity().sendMessage("Setting ability task to null");
		executor.setAbilityTask(null, "none");
	}
}
