package btm.sword.system.inventory;

import btm.sword.system.inventory.selection.MenuButton;
import btm.sword.system.item.KeyCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Objects;

public class Menu {
	private final MenuLayout layout;
	private final Inventory inventory;
	
	public Menu(Component title, int size) {
		this.layout = new MenuLayout(size);
		this.inventory = Bukkit.createInventory(null, size, title);
	}
	
	public Menu(Component title, int size, int leftPadding, int rightPadding, int topPadding, int botPadding) {
		this.layout = new DynamicMenuLayout(size, leftPadding, rightPadding, topPadding, botPadding);
		this.inventory = Bukkit.createInventory(null, size, title);
	}
	
	public void display(Player player) {
		if (layout.isInvalidated())
			inventory.setContents(layout.render());
		player.openInventory(inventory);
	}
	
	public boolean handleClick(InventoryClickEvent e) {
		if (layout instanceof DynamicMenuLayout dml && !dml.isEmpty()) {
			ItemStack cursor = e.getCursor();
			ItemMeta cursorMeta = cursor.getItemMeta();
			String cursorId = cursorMeta == null ? null : cursorMeta.getPersistentDataContainer().get(KeyCache.buttonTagKey, PersistentDataType.STRING);
			
			ItemStack inSlot = e.getCurrentItem();
			ItemMeta inSlotMeta = inSlot == null ? null : inSlot.getItemMeta();
			String inSlotId = inSlotMeta == null ? null : inSlotMeta.getPersistentDataContainer().get(KeyCache.buttonTagKey, PersistentDataType.STRING);
			
			if (cursorId != null && !cursorId.isEmpty()) {
				MenuButton clicked = dml.getContent(cursorId);
				if (clicked != null) {
					return clicked.onClick(e);
				}
			}
			else if(inSlotId != null && !inSlotId.isEmpty()) {
				MenuButton clicked = dml.getContent(inSlotId);
				if (clicked != null) {
					return clicked.onClick(e);
				}
			}
		}
		int slot = e.getSlot();
		if (slot < 0)
			return true;
		MenuButton button = layout.getButton(slot);
		if (button == null)
			return true;
		
		return button.onClick(e);
	}
	
	public boolean equateInv(Inventory inventory) {
		return Objects.equals(this.inventory, inventory);
	}
	
	public void addButton(MenuButton button) {
		if (layout instanceof DynamicMenuLayout dml) {
			dml.addContent(button);
		}
		else {
			int empty = inventory.firstEmpty();
			if (empty == -1) return;
			setButton(empty, button);
		}
	}
	
	public boolean isFull() {
		return (layout instanceof DynamicMenuLayout db && db.isFull()) ||
				Arrays.stream(inventory.getContents()).noneMatch(itemStack -> itemStack == null || itemStack.getType().isAir() || itemStack.isEmpty());
	}
	
	public boolean isEmpty() {
		return (layout instanceof DynamicMenuLayout db && db.isEmpty()) ||
				Arrays.stream(inventory.getContents()).anyMatch(itemStack -> itemStack != null && !itemStack.getType().isAir());
	}
	
	public void removeButton(String uuidString) {
		if (layout instanceof DynamicMenuLayout dml) {
			dml.removeContent(uuidString);
		}
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
