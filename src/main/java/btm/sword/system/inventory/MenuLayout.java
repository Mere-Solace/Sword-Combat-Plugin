package btm.sword.system.inventory;

import btm.sword.system.inventory.selection.MenuButton;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MenuLayout {
	private final Map<Integer, MenuButton> buttons;
	private final Map<Integer, ItemStack> decorations;
	private final int size;
	
	protected boolean invalidated;
	
	public MenuLayout(int size) {
		this.size = size;
		buttons = new HashMap<>(size);
		decorations = new HashMap<>(size);
		
		invalidated = true;
	}
	
	public void handleInput(int slot, InventoryClickEvent e) {
		buttons.get(slot).onClick(e);
	}
	
	public ItemStack[] render() {
		ItemStack[] contents = new ItemStack[size];
		
		for (Map.Entry<Integer, MenuButton> pair : buttons.entrySet()) {
			contents[pair.getKey()] = pair.getValue().getItem();
		}
		
		for (Map.Entry<Integer, ItemStack> pair : decorations.entrySet()) {
			int slot = pair.getKey();
			if (contents[slot] == null || contents[slot].getType().isAir())
				contents[slot] = pair.getValue();
		}
		
		invalidated = false;
		return contents;
	}
	
	public void setButton(int slot, MenuButton button) {
		invalidated = true;
		buttons.put(slot, button);
	}
	
	public MenuButton getButton(int slot) {
		return buttons.get(slot);
	}
	
	public void removeButton(int slot) {
		invalidated = true;
		buttons.remove(slot);
	}
	
	public void setDecoration(int slot, ItemStack decoration) {
		invalidated = true;
		decorations.put(slot, decoration);
	}
	
	public void removeDecoration(int slot) {
		invalidated = true;
		decorations.remove(slot);
	}
	
	public boolean isInvalidated() {
		return invalidated;
	}
	
	public int size() {
		return size;
	}
}
