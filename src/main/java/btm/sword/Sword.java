package btm.sword;

import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	@Override
	public void onEnable() {
		instance = this;
		
		getLogger().info("~ Sword Plugin starting up ~");
	}
	
	@Override
	public void onDisable() {
		getLogger().info("~ Sword Plugin has been disabled ~");
	}
}
