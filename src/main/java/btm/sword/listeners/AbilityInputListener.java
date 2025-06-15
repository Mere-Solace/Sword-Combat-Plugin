package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.action.MovementAction;
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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityInputListener implements Listener {
	
	@EventHandler
	public void onNormalAttack(PrePlayerAttackEntityEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();

		swordPlayer.takeInput(InputType.LEFT, itemType);
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material mainItemType = player.getInventory().getItemInMainHand().getType();
		Material offItemType = player.getInventory().getItemInOffHand().getType();
		Action action = event.getAction();
		
		if (swordPlayer.hasPerformedDropAction()) return;
		
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
			swordPlayer.takeInput(InputType.LEFT, mainItemType);
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			swordPlayer.takeInput(InputType.RIGHT, mainItemType);
		}
		else if (!offItemType.isAir() && !mainItemType.isAir()) {
			player.sendMessage("Doing some other interact action rn while holding: " + offItemType + " in off hand, and " + mainItemType + " in main.");
			swordPlayer.takeInput(InputType.RIGHT, mainItemType);
		}
		
		Bukkit.getCurrentTick();
	}
	
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Material itemType = event.getItemDrop().getItemStack().getType();
		swordPlayer.setPerformedDropAction(true);
		
		if (swordPlayer.isGrabbing()) {
			swordPlayer.getGrabTask().cancel();
			swordPlayer.setGrabbing(false);
			
			Bukkit.getScheduler().runTaskLater(Sword.getInstance(), MovementAction.toss(swordPlayer, swordPlayer.getGrabbedEntity()), 2);
		}
		else
			swordPlayer.takeInput(InputType.DROP, itemType);
		
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
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		
		if (event.isSneaking()) {
			swordPlayer.takeInput(InputType.SHIFT, itemType);
		}
	}
	
	@EventHandler
	public void onSwapEvent(PlayerSwapHandItemsEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		swordPlayer.takeInput(InputType.SWAP, itemType);
	}
}
