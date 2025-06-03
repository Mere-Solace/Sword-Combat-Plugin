package btm.sword.listeners;

import btm.sword.player.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		PlayerManager.registerPlayer(event.getPlayer());
	}
}
