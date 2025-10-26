package btm.sword;

import btm.sword.commands.CommandManager;
import btm.sword.listeners.EntityListener;
import btm.sword.listeners.InputListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.system.playerdata.PlayerDataManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@Setter
public final class Sword extends JavaPlugin {
	@Getter
    private static Sword instance;
	@Getter
    private static ScheduledExecutorService scheduler;

    @Override
	public void onEnable() {
		instance = this;
		scheduler = Executors.newSingleThreadScheduledExecutor();
		
		getServer().getPluginManager().registerEvents(new InputListener(), this);
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

    public static void print(String str) {
        instance.getLogger().info(str);
    }
}
