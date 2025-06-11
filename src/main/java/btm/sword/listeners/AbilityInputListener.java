package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;

import btm.sword.system.input.InputType;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
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

public class AbilityInputListener implements Listener {
	
	@EventHandler
	public void onNormalAttackEvent(PrePlayerAttackEntityEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		swordPlayer.takeInput(InputType.LEFT, itemType);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		Action action = event.getAction();
		
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) && !swordPlayer.hasPerformedDropAction()) {
			swordPlayer.takeInput(InputType.LEFT, itemType);
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			swordPlayer.takeInput(InputType.RIGHT, itemType);
		}
		
		Bukkit.getCurrentTick();
	}
	
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = event.getItemDrop().getItemStack().getType();
		
		swordPlayer.takeInput(InputType.DROP, itemType);
		
		swordPlayer.setPerformedDropAction(true);
		
		new BukkitRunnable() {
			@Override
			public void run() {
				swordPlayer.setPerformedDropAction(false);
			}
		}.runTaskLater(Sword.getInstance(), 1);
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onSneakEvent(PlayerToggleSneakEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		
		if (event.isSneaking()) {
			swordPlayer.takeInput(InputType.SHIFT, itemType);
		}
	}
}
