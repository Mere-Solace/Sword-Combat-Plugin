package btm.sword.system.inventory;

import btm.sword.system.entity.SwordPlayer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;

public class MenuAction {
	public static void spawnShieldDisplay(SwordPlayer swordPlayer, ClickType clickType, InventoryAction action) {
		Player player = swordPlayer.player();
		ItemDisplay shield = (ItemDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)), EntityType.ITEM_DISPLAY);
		shield.setItemStack(new ItemStack(Material.SHIELD));
	}
	
	public static void spawnBlockDisplay(SwordPlayer swordPlayer, ClickType clickType, InventoryAction action) {
		Player player = swordPlayer.player();
		BlockDisplay block = (BlockDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)), EntityType.BLOCK_DISPLAY);
		block.setGlowing(true);
		block.setGlowColorOverride(Color.fromRGB(255, 131, 37));
	}
}
