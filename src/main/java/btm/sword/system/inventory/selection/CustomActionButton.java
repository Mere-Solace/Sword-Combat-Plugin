package btm.sword.system.inventory.selection;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class CustomActionButton extends MenuButton {
	private final Consumer<InventoryClickEvent> onClick;
	
	public CustomActionButton(Material itemType, Component name, List<Component> lore, Consumer<InventoryClickEvent> onClick) {
		super(itemType, name, lore);
		this.onClick = onClick;
	}
	
	public CustomActionButton(ItemStack itemStack, Consumer<InventoryClickEvent> onClick) {
		super(itemStack);
		this.onClick = onClick;
	}
	
	@Override
	public boolean onClick(InventoryClickEvent e) {
		if (onClick == null) return true;
		onClick.accept(e);
		return true;
	}
}
