package btm.sword.system.inventory.selection;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SelectionItem {
	@NotNull ItemStack getItem();
	
	// return true only if the original click event should be cancelled
	boolean onClick(InventoryClickEvent e);
	
	@NotNull String getId();
}
