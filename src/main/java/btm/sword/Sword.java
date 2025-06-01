package btm.sword;

import btm.sword.commands.CommandManager;
import btm.sword.listeners.ItemUseListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.player.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getServer().getPluginManager().registerEvents(new ItemUseListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		
		CommandManager.register();
		
		PlayerManager.initialize();
		
		getLogger().info("~ Sword Plugin has been enabled ~");
	}
	
	@Override
	public void onDisable() {
		PlayerManager.shutdown();
		
		getLogger().info("~ Sword Plugin has been disabled ~");
	}
	
	public static Sword getInstance() {
		return instance;
	}
}
