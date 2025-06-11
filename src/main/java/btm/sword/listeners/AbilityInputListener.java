package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AbilityInputListener implements Listener {
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		Action action = event.getAction();
		
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		player.sendMessage("performed drop (before)?: " + swordPlayer.hasPerformedDropAction());
		
		Bukkit.getCurrentTick();
		
		
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && !swordPlayer.hasPerformedDropAction()) {
			player.sendMessage("You Left clicked!");
		}
		
		player.sendMessage("performed drop (after)?: " + swordPlayer.hasPerformedDropAction());
		
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {

			
			
			swordPlayer.setCancelRightClick(true);
			// do execution logic though
			if (swordPlayer.getTicksRightClick() > 1.5*20)
				player.sendMessage("Attempted to throw weapon!");
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			
			
			if (!swordPlayer.isHoldingRightClick()) {
				swordPlayer.setIsHoldingRight(true);
				swordPlayer.startRightClickTimer();
			}
		}
	}
	
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		swordPlayer.setPerformedDropAction(true);
		new BukkitRunnable() {
			@Override
			public void run() {
				swordPlayer.setPerformedDropAction(false);
			}
		}.runTaskLater(Sword.getInstance(), 1);
		
		
		player.sendMessage("Type of Inventory: " + player.getOpenInventory().getType());
		player.sendMessage("TopInventory: " + player.getOpenInventory().getTopInventory());
		
		double dashPower = 1;
		double m = player.isSneaking() ? -dashPower : dashPower;
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					player.setVelocity(player.getEyeLocation().getDirection().multiply(m).add(new Vector(0, .4, 0)));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onSneakEvent(PlayerToggleSneakEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		if (event.isSneaking()) {
			swordPlayer.setCancelRightClick(true);
		}
	}
	
	private void rightClickHandler() {
	
	}
	
	private void leftClickHandler() {
	
	}
	
	private void dropHandler() {
	
	}
	
	private void sneakHandler() {
	
	}
}
