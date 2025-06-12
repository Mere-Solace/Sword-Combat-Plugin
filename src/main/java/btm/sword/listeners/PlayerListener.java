package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		SwordEntityArbiter.register(event.getPlayer());
		event.getPlayer().sendMessage("Hello!");
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		SwordEntityArbiter.remove(event.getPlayer().getUniqueId());
		Sword.getInstance().getLogger().info(event.getPlayer().getName() + " has left the server ;(");
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		// TODO
	}
}
