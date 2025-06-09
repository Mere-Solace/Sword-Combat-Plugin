package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.combat.attack.AttackTriggerType;
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

public class ItemUseListener implements Listener {
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		Action action = event.getAction();
		
		Material itemType = player.getInventory().getItemInMainHand().getType();
		
		Bukkit.getCurrentTick();
		
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			swordPlayer.performAbility(itemType, AttackTriggerType.LEFT);
			
			
			swordPlayer.setCancelRightClick(true);
			// do execution logic though
			if (swordPlayer.getTicksRightClick() > 1.5*20)
				player.sendMessage("Attempted to throw weapon!");
		}
		else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			swordPlayer.performAbility(itemType, AttackTriggerType.RIGHT);
			
			
			if (!swordPlayer.isHoldingRightClick()) {
				swordPlayer.setIsHoldingRight(true);
				swordPlayer.startRightClickTimer();
			}
		}
	}
	
	// Dash IN REAL LIFE :D
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		
		event.setCancelled(true);
		
		Player player = event.getPlayer();
		player.sendMessage("Dashing!");
//		if (player.getOpenInventory().getType() != InventoryType.CRAFTING) return;
		
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
	}
	
	@EventHandler
	public void onSneakEvent(PlayerToggleSneakEvent event) {
		SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.get(event.getPlayer().getUniqueId());
		Player player = (Player) swordPlayer.getAssociatedEntity();
		
		if (event.isSneaking()) {
			swordPlayer.setCancelRightClick(true);
		}
	}
}
