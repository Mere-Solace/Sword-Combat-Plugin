package btm.sword.system.inventory;

import btm.sword.system.inventory.selection.MenuButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class Menu {
	private final MenuLayout layout;
	private final Inventory inventory;
	
	public Menu(Component title, int size) {
		this.layout = new MenuLayout(size);
		this.inventory = Bukkit.createInventory(null, size, title);
	}
	
	public void display(Player player) {
		if (layout.isInvalidated())
			inventory.setContents(layout.render());
		
		player.openInventory(inventory);
	}
	
	public void handleClick(InventoryClickEvent e) {
		int slot = e.getSlot();
		if (slot < 0)
			return;
		MenuButton button = layout.getButton(slot);
		if (button == null)
			return;
		button.onClick(e);
	}
	
	public boolean equateInv(Inventory inventory) {
		return Objects.equals(this.inventory, inventory);
	}
	
	public void setButton(int slot, MenuButton button) {
		layout.setButton(slot, button);
	}
	
	public void removeButton(int slot) {
		layout.removeButton(slot);
	}
	
	public void setDecoration(int slot, ItemStack decoration) {
		layout.setDecoration(slot, decoration);
	}
	
	public void removeDecoration(int slot) {
		layout.removeDecoration(slot);
	}
}
