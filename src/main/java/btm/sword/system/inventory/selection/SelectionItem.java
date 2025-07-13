package btm.sword.system.inventory.selection;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SelectionItem {
	@NotNull ItemStack getItem();
	
	void onClick(InventoryClickEvent e);
	
	@NotNull String getId();
}
