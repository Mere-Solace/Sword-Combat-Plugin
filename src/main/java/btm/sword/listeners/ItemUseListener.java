package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.combat.AttackTriggerType;
import btm.sword.system.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ItemUseListener implements Listener {
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer player = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		
		if (event.getAction().isLeftClick()) {
			player.performAbility(player.getAssociatedEntity().getActiveItem().getType(), AttackTriggerType.LEFT);
		}
		else if (event.getAction().isRightClick()) {
			player.performAbility(player.getAssociatedEntity().getActiveItem().getType(), AttackTriggerType.RIGHT);
		}
	}
	
	// Dash IN REAL LIFE :D
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		event.setCancelled(true);
		
		Player player = event.getPlayer();
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					player.setVelocity(player.getEyeLocation().getDirection().multiply(0.1).add(new Vector(0, .25, 0)));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
