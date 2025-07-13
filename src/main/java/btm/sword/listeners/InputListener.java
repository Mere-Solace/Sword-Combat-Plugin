package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;

import btm.sword.system.input.InputType;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class InputListener implements Listener {
	
	@EventHandler
	public void onNormalAttack(PrePlayerAttackEntityEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		ItemStack item = swordPlayer.getItemStackInHand(true);
		
		if (swordPlayer.evaluateItemInput(item, InputType.LEFT)) {
			event.setCancelled(true);
			return;
		}
		
		swordPlayer.act(InputType.LEFT);
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		ItemStack item = swordPlayer.getItemStackInHand(true);
		
		Action action = event.getAction();
		
		if (swordPlayer.hasPerformedDropAction()) return;
		
		if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
			if (swordPlayer.evaluateItemInput(item, InputType.LEFT)) {
				event.setCancelled(true);
				return;
			}
			swordPlayer.act(InputType.LEFT);
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (swordPlayer.evaluateItemInput(item, InputType.RIGHT)) {
				event.setCancelled(true);
				return;
			}
			
			swordPlayer.act(InputType.RIGHT);
		}
	}
	
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		ItemStack item = swordPlayer.getItemStackInHand(true);
		
		swordPlayer.setPerformedDropAction(true);
		
		if (swordPlayer.evaluateItemInput(item, InputType.DROP)) {
			event.setCancelled(true);
		}
		else if (!swordPlayer.isDroppingInInv()) {
			swordPlayer.act(InputType.DROP);
			event.setCancelled(true);
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {
				swordPlayer.setPerformedDropAction(false);
			}
		}.runTaskLater(Sword.getInstance(), 1);
	}
	
	@EventHandler
	public void onSneakEvent(PlayerToggleSneakEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		
		if (event.isSneaking()) {
			swordPlayer.act(InputType.SHIFT);
		}
		else {
			swordPlayer.endSneaking();
		}
	}
	
	@EventHandler
	public void onSwapEvent(PlayerSwapHandItemsEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		ItemStack item = swordPlayer.getItemStackInHand(true);
		
		if (swordPlayer.evaluateItemInput(item, InputType.SWAP)) {
			event.setCancelled(true);
		}
		else if (!swordPlayer.isSwappingInInv()) {
			swordPlayer.act(InputType.SWAP);
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onChangeItemEvent(PlayerItemHeldEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
		
		if (swordPlayer.inputReliantOnItem()) {
			swordPlayer.resetTree();
		}
		
		if (swordPlayer.isAttemptingThrow()) {
			ThrowAction.throwCancel(swordPlayer);
		}
	}
}
