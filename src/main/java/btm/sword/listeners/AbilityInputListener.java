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
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityInputListener implements Listener {
	
	@EventHandler
	public void onNormalAttack(PrePlayerAttackEntityEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.entity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		swordPlayer.act(InputType.LEFT, itemType);
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.entity();
		Material mainItemType = player.getInventory().getItemInMainHand().getType();
		Material offItemType = player.getInventory().getItemInOffHand().getType();
		Action action = event.getAction();
		
		if (swordPlayer.hasPerformedDropAction()) return;
		
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
			swordPlayer.act(InputType.LEFT, mainItemType);
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			swordPlayer.act(InputType.RIGHT, mainItemType);
		}
		
		Bukkit.getCurrentTick();
	}
	
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Material itemType = event.getItemDrop().getItemStack().getType();
		swordPlayer.setPerformedDropAction(true);
		
		if (swordPlayer.isGrabbing()) swordPlayer.setGrabbing(false);
		
		else swordPlayer.act(InputType.DROP, itemType);
		
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
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.entity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		if (event.isSneaking()) {
			swordPlayer.act(InputType.SHIFT, itemType);
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onSwapEvent(PlayerSwapHandItemsEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.entity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		swordPlayer.act(InputType.SWAP, itemType);
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onChangeItemEvent(PlayerItemHeldEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		
		swordPlayer.resetTree();
	}
}
