package btm.sword;

import btm.sword.commands.CommandManager;
import btm.sword.listeners.EntityListener;
import btm.sword.listeners.InputListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.system.event.EventTasks;
import btm.sword.system.playerdata.PlayerDataManager;

import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getServer().getPluginManager().registerEvents(new InputListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new EntityListener(), this);
		
		EventTasks.startPlayerGroundedTask();
		
		CommandManager.register();
		
		PlayerDataManager.initialize();
		
		getLogger().info("~ Sword Plugin has been enabled ~");
	}
	
	@Override
	public void onDisable() {
		PlayerDataManager.shutdown();
		
		EventTasks.playerGroundedUpdateEventTask.cancel();
		
		getLogger().info("~ Sword Plugin has been disabled ~");
	}
	
	public static Sword getInstance() {
		return instance;
	}
}
