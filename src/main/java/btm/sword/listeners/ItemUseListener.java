package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.combat.attack.AttackTriggerType;
import btm.sword.system.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;

import org.bukkit.Material;
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
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		Material itemType = player.getInventory().getItemInMainHand().getType();

		if (event.getAction().isLeftClick()) {
			swordPlayer.performAbility(itemType, AttackTriggerType.LEFT);
		}
		else if (event.getAction().isRightClick()) {
			swordPlayer.performAbility(itemType, AttackTriggerType.RIGHT);
		}
	}
	
	// Dash IN REAL LIFE :D
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		event.setCancelled(true);
		
		Player player = event.getPlayer();
		
		double m = player.isSneaking() ? -0.75 : 0.75;
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					player.setVelocity(player.getEyeLocation().getDirection().multiply(m).add(new Vector(0, .4, 0)));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
