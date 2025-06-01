package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.combat.CombatManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ItemUseListener implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.getAction().isRightClick()) return;
		
		Player player = event.getPlayer();
		ItemStack item  = player.getInventory().getItemInMainHand();
		
		Material itemType = item.getType();
		switch(itemType) {
			case IRON_SHOVEL, DIAMOND_HOE -> CombatManager.executeAttack(player);
			case NETHERITE_SWORD -> CombatManager.test(player);
			case DIAMOND_AXE -> CombatManager.arcTest(player);
			default -> { }
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
