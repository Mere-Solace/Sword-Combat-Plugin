package btm.sword;

import btm.sword.listeners.ItemUseListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getServer().getPluginManager().registerEvents(new ItemUseListener(), this);
		
		getLogger().info("~ Sword Plugin has been enabled ~");
	}
	
	@Override
	public void onDisable() {
		getLogger().info("~ Sword Plugin has been disabled ~");
	}
	
	public static Sword getInstance() {
		return instance;
	}
}
