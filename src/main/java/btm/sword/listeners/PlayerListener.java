package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.event.EventTasks;
import btm.sword.system.event.PlayerGroundedUpdateEvent;
import btm.sword.util.EntityUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
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
		Player p = event.getPlayer();
		SwordEntityArbiter.register(p);
		p.sendMessage("Hello!");
		EventTasks.playerGroundedMap.put(p.getUniqueId(), EntityUtil.isOnGround(p));
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		SwordEntityArbiter.remove(event.getPlayer().getUniqueId());
		Sword.getInstance().getLogger().info(event.getPlayer().getName() + " has left the server ;(");
		EventTasks.playerGroundedMap.remove(event.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {

	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		SwordEntityArbiter.register(event.getPlayer());
		event.getPlayer().sendMessage("You've been reassigned to your SwordPlayer object!");
		event.getPlayer().sendMessage("You: " + SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId()));
	}
	
	@EventHandler
	public void onGroundedUpdate(PlayerGroundedUpdateEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());

		if (event.isGrounded()) {
			swordPlayer.resetAirDashesPerformed();
			swordPlayer.message("Resetting your # air dashes");
		}
	}
}
