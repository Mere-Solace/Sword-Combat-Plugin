package btm.sword.system.inventory.selection;

import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChangePageButton implements SelectionItem {
	private Material itemType;
	private final ItemStack item;
	
	public ChangePageButton(Material itemType, Component name, List<Component> lore) {
		this.itemType = itemType;
		
		item = new ItemStackBuilder(itemType)
				.name(name)
				.lore(lore)
				.build();
	}
	
	@Override
	public @NotNull ItemStack getItem() {
		return item;
	}
	
	@Override
	public void onClick(InventoryClickEvent e) {
	
	}
	
	@Override
	public @NotNull String getId() {
		return "change_page_button";
	}
}
