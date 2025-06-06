package btm.sword;

import btm.sword.combat.attack.AttackManager;
import btm.sword.commands.CommandManager;
import btm.sword.effect.EffectManager;
import btm.sword.listeners.ItemUseListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.player.PlayerManager;
import btm.sword.util.ParticleSpawner;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sword extends JavaPlugin {
	private static Sword instance;
	
	private static AttackManager attackManager;
	
	@Override
	public void onEnable() {
		instance = this;
		attackManager =  new AttackManager(new EffectManager(new ParticleSpawner(50)));
		
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
	
	public static AttackManager getAttackManager() {
		return attackManager;
	}
}
