package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.player.PlayerManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
		
		Sword.getInstance().getLogger().info(player.toString());
		Sword.getInstance().getLogger().info(PlayerManager.getPlayerData(player.getUniqueId()).toString());
		
		Location l = player.getEyeLocation();
		ItemStack item  = player.getInventory().getItemInMainHand();
		
		Material itemType = item.getType();
		switch(itemType) {
//			case IRON_SHOVEL, DIAMOND_HOE -> attackManager.start();
//			case NETHERITE_SWORD -> Sword.getAttackManager().start(PlayerManager.getPlayerData(player.getUniqueId()), AttackSequencePrefab.ARC_ARC_LINE);
			case DIAMOND_AXE -> {}
			case WOODEN_SWORD ->
					player.getWorld().spawnParticle(
					Particle.CLOUD,
					l.clone().add(l.getDirection().multiply(4)),
					3, l.getDirection().getX(), l.getDirection().getY(), l.getDirection().getZ(), 1);
			case NETHERITE_PICKAXE ->
					player.getWorld().spawnParticle(
					Particle.END_ROD,
					l.clone().add(l.getDirection().multiply(4)),
					0, l.getDirection().getX(), l.getDirection().getY(), l.getDirection().getZ(), 2);
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
