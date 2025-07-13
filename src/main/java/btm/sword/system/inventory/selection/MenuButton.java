package btm.sword.system.inventory.selection;

import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class MenuButton implements SelectionItem {
	protected ItemStack itemStack;
	
	public MenuButton(Material itemType, Component name, List<Component> lore) {
		itemStack = new ItemStackBuilder(itemType)
				.name(name)
				.lore(lore)
				.build();
	}
	
	@Override
	public @NotNull ItemStack getItem() {
		return itemStack;
	}
}
