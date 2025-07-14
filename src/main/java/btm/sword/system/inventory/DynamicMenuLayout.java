package btm.sword.system.inventory;

import btm.sword.system.inventory.selection.MenuButton;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;

public class DynamicMenuLayout extends MenuLayout {
	private final ArrayList<MenuButton> contents = new ArrayList<>();
	private final int leftPadding;
	private final int rightPadding;
	private final int topPadding;
	private final int botPadding;
	private boolean isFull;
	private final int maxSize;
	private boolean invalidPadding;
	
	public DynamicMenuLayout(int size, int leftPadding, int rightPadding, int topPadding, int botPadding) {
		super(size);
		this.leftPadding = leftPadding;
		this.rightPadding = rightPadding;
		this.topPadding = topPadding;
		this.botPadding = botPadding;
		maxSize = (9-leftPadding-rightPadding)*((size/9)-topPadding-botPadding);
		if (maxSize <= 0) invalidPadding = true;
	}
	
	public MenuButton getContent(String uuid) {
		for (MenuButton button : contents) {
			if (button.getId().equals(uuid)) return button;
		}
		return null;
	}
	
	public void addContent(MenuButton button) {
		if (isFull) return;
		
		contents.add(button);
		invalidated = true;
		
		if (contents.size() >= maxSize) {
			isFull = true;
		}
	}
	
	public void removeContent(String uuidString) {
		for (int i = 0; i < contents.size(); i++) {
			if (contents.get(i).getId().equals(uuidString)) {
				contents.remove(i);
				return;
			}
		}
	}
	
	public void removeContent(MenuButton button) {
		contents.remove(button);
		invalidated = true;
	}
	
	@Override
	public ItemStack[] render() {
		ItemStack[] curContents = super.render();
		
		int start = leftPadding + topPadding*9;
		if (start > size()) return curContents;
		
		int end = (9-rightPadding) + (size() - botPadding*9);
		Iterator<MenuButton> iterator = contents.iterator();
		for (int i = start; i < end; i++) {
			if (i > size()-1 || !iterator.hasNext()) return curContents;
			curContents[i] = iterator.next().getItem();
		}
		invalidated = false;
		return curContents;
	}
	
	public boolean isFull() {
		return isFull;
	}
	
	public boolean isEmpty() {
		return contents.isEmpty();
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public boolean isInvalidPadding() {
		return invalidPadding;
	}
}
