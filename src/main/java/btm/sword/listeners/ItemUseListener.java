package btm.sword.listeners;

import btm.sword.combat.CombatManager;
import btm.sword.combat.attack.AttackType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemUseListener implements Listener {
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.getAction().isRightClick()) return;
		
		Player player = event.getPlayer();
		ItemStack item  = player.getInventory().getItemInMainHand();
		
		Material itemType = item.getType();
		switch(itemType) {
			case IRON_SHOVEL, DIAMOND_HOE -> CombatManager.executeAttack(player, AttackType.GUNSHOT);
			case NETHERITE_SWORD -> CombatManager.executeAttack(player, AttackType.SWORD_SLASH);
			default -> { }
		}
	}
}
