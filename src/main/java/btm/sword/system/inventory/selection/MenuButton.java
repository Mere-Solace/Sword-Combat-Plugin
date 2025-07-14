package btm.sword.system.inventory.selection;

import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public abstract class MenuButton implements SelectionItem {
	protected ItemStack itemStack;
	private final String uuidString;
	
	public MenuButton(Material itemType, Component name, List<Component> lore) {
		uuidString = UUID.randomUUID().toString();
		itemStack = new ItemStackBuilder(itemType)
				.name(name)
				.lore(lore)
				.tag(KeyCache.buttonTag, uuidString)
				.hideAll()
				.build();
	}
	
	public MenuButton(ItemStack itemStack) {
		this(itemStack.getType(),
				itemStack.getItemMeta() == null ? null : itemStack.getItemMeta().itemName(),
				itemStack.getItemMeta() == null ? null : itemStack.getItemMeta().lore());
	}
	
	@Override
	public @NotNull ItemStack getItem() {
		return itemStack;
	}
	
	@Override
	public @NotNull String getId() {
		return uuidString;
	}
}
