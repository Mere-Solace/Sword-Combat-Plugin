package btm.sword;

import btm.sword.commands.CommandManager;
import btm.sword.listeners.EntityListener;
import btm.sword.listeners.AbilityInputListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.system.playerdata.PlayerDataManager;

import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getServer().getPluginManager().registerEvents(new AbilityInputListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new EntityListener(), this);

		CommandManager.register();
		
		PlayerDataManager.initialize();
		
		getLogger().info("~ Sword Plugin has been enabled ~");
	}
	
	@Override
	public void onDisable() {
		PlayerDataManager.shutdown();
		
		getLogger().info("~ Sword Plugin has been disabled ~");
	}
	
	public static Sword getInstance() {
		return instance;
	}
}
