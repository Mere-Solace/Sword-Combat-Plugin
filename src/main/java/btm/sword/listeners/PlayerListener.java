package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

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
		Sword.getInstance().getLogger().info("Death event runs");
		new BukkitRunnable() {
			@Override
			public void run() {
				SwordEntityArbiter.register(event.getPlayer());
				event.getPlayer().sendMessage("You've been reassigned to your SwordPlayer object!");
				event.getPlayer().sendMessage("You: " + SwordEntityArbiter.get(event.getPlayer().getUniqueId()));
			}
		}.runTaskLater(Sword.getInstance(), 20);
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Sword.getInstance().getLogger().info("Respawn event runs");
		SwordEntityArbiter.register(event.getPlayer());
		event.getPlayer().sendMessage("You've been reassigned to your SwordPlayer object!");
		event.getPlayer().sendMessage("You: " + SwordEntityArbiter.get(event.getPlayer().getUniqueId()));
	}
}
