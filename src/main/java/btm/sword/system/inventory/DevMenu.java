package btm.sword.system.inventory;

import btm.sword.system.entity.Developer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.util.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import java.util.*;

public class DevMenu {
	public static final String BASE_TITLE_STRING = "| Development Menu |";
	public static final TextColor BASE_TITLE_COLOR = TextColor.color(191, 83, 30);
	public static final String PAGE_STRING = " Page ";
	public static final String buttonKey = "dev_button_id";
	private static final int SIZE = 45;
	
	private final List<Inventory> pages = new LinkedList<>();
	private int highestPageNum;
	private int curPageNum;
	
	private final HashMap<String, BiConsumer<Developer, ClickType>> buttonPairings = new HashMap<>();
	public final ItemStack nextPage;
	
	private final Developer dev;
	
	private final HashMap<String, Display> displayMap = new HashMap<>();
	private final HashMap<String, ItemStack> itemStackMap = new HashMap<>();
	
	private final HashSet<Display> currentlySelected = new HashSet<>();
	
	private float theta;
	private boolean shouldDisplayOriginParticles;
	
	public DevMenu(Developer developer) {
		this.dev = developer;
		
		dev.message("Display map identity hash code: " + System.identityHashCode(displayMap));
		
		theta = (float) Math.PI/180;
		shouldDisplayOriginParticles = true;
		
		highestPageNum = 0;
		Inventory mainMenu = Bukkit.createInventory(developer.player(), SIZE,
				Component.text(BASE_TITLE_STRING, BASE_TITLE_COLOR).append(Component.text(PAGE_STRING + 1)));
		
		this.nextPage = createButton(Material.TORCHFLOWER,
				"Next Page",
				"Shift-click to return to main menu",
				this::pageTurn);
		
		ItemStack[] mainPageContents = new ItemStack[SIZE];
		mainPageContents[22] = createButton(Material.SHIELD,
				"Spawn Test Display",
				"Right of Left click: spawn a display of the item in your main/off hand",
				MenuActions::spawnDisplay);
		for (int i = 36; i < SIZE-1; i++) {
			mainPageContents[i] = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
		}
		mainPageContents[44] = nextPage;
		
		mainMenu.setContents(mainPageContents);
		pages.add(mainMenu);
	}
	
	public void displayMenuPage(int pageNumber) {
		dev.player().openInventory(pages.get(Math.min(Math.max(pageNumber, 0), pages.size()-1)));
	}
	
	public ItemStack createButton(Material material, String name, String lore, BiConsumer<Developer, ClickType> action) {
		String uuidString = UUID.randomUUID().toString();
		ItemStack item = new ItemStackBuilder(material)
				.name(Component.text(name, TextColor.color(191, 104, 7)))
				.lore(List.of(Component.text(lore, TextColor.color(143, 146, 168), TextDecoration.ITALIC)))
				.tag(buttonKey, uuidString)
				.build();
		
		buttonPairings.put(uuidString, action);
		
		return item;
	}
	
	public void handleInput(ItemStack item, ClickType clickType) {
		if (item == null) return;
		
		if (item.getType().isAir()) {
			
			return;
		}
		
		ItemMeta meta = item.getItemMeta();
		
		if (meta == null) {
			dev.message("ItemMeta is null");
			return;
		}
		
		String uuid = meta.getPersistentDataContainer().get(KeyCache.buttonIdKey, PersistentDataType.STRING);
		
		if (uuid == null || uuid.isEmpty()) {
			dev.message("This is not a button, but may be interactable.");
			handleDisplayClick(meta, clickType);
		}
		else {
			BiConsumer<Developer, ClickType> action = buttonPairings.get(uuid);
			if (action != null) {
				dev.message("Action is not null! performing now");
				action.accept(dev, clickType);
			}
		}
	}
	
	public void handleDisplayClick(ItemMeta meta, ClickType clickType) {
		String uuidString = meta.getPersistentDataContainer().get(KeyCache.displayIdKey, PersistentDataType.STRING);
		
		if (uuidString == null || uuidString.isEmpty()) return;
		
		switch (clickType) {
			case LEFT -> select(uuidString);
			case RIGHT -> deselect(uuidString);
			case SWAP_OFFHAND -> removeDisplay(uuidString);
		}
	}
	
	public void select(String uuid) {
		ItemStack reference = itemStackMap.get(uuid);
		if (reference == null) return;
		
		reference.addUnsafeEnchantment(Enchantment.SHARPNESS, 255);
		currentlySelected.add(displayMap.get(uuid));
	}
	
	public void deselect(String uuid) {
		ItemStack reference = itemStackMap.get(uuid);
		if (reference == null) return;
		
		reference.removeEnchantments();
		currentlySelected.remove(displayMap.get(uuid));
	}
	
	public void pageTurn(Developer developer, ClickType clickType) {
		debugDump();
//
//		dev.message("|- Max page num: " + highestPageNum
//				+ "\n|- CurPageNum: " + curPageNum
//				+ "\n|- Keys in display map: " + displayMap.size()
//				+ "\n|- Keys of display map:\n\n" + displayMap.keySet() + "\n\n");
//		switch (clickType) {
//			case SHIFT_LEFT, SHIFT_RIGHT -> curPageNum = 0;
//			case RIGHT -> curPageNum = Math.min(curPageNum + 1, pages.size() - 1);
//			case LEFT -> curPageNum = Math.max(curPageNum - 1, 0);
//		}
//		dev.message("New Page num: " + curPageNum);
		dev.player().closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
//		displayMenuPage(curPageNum);
//
//		debugFullDump();
	}
	
	public void debugDump() {
		dev.message("------ FULL DEBUG DUMP ------");
		dev.message("Display map identity hash code: " + System.identityHashCode(displayMap));
		dev.message("displayMap size: " + displayMap.size());
		for (Map.Entry<String, Display> entry : displayMap.entrySet()) {
			dev.message("Key: " + entry.getKey() + " | Display: " + entry.getValue());
		}
		dev.message("ItemStackMap size: " + itemStackMap.size());
		for (Map.Entry<String, ItemStack> entry : itemStackMap.entrySet()) {
			dev.message("Key: " + entry.getKey() + " | Item: " + entry.getValue());
		}
		dev.message("Pages: " + pages.size());
		dev.message("------------------------------");
	}
	
	public void addPage() {
		debugDump();
		int num = highestPageNum + 1;
		Inventory newPage = Bukkit.createInventory(dev.player(), SIZE,
				Component.text(BASE_TITLE_STRING, BASE_TITLE_COLOR).append(Component.text(PAGE_STRING + num)));
		
		ItemStack[] taskBar = new ItemStack[SIZE];
		taskBar[44] = nextPage;
		for (int i = 36; i < SIZE - 1; i++) {
			taskBar[i] = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
		}
		newPage.setContents(taskBar);
		
		pages.add(newPage);
		dev.message("Added a new page to the list of pages");
		
		highestPageNum++;
		debugDump();
	}
	
	public void removePage() {
		pages.removeLast();
		highestPageNum--;
	}
	
	public float getTheta() {
		return theta;
	}
	
	public void setTheta(float theta) {
		this.theta = theta;
	}
	
	public void addNewDisplay(ItemStack itemStack, Display display) {
		debugDump();
		dev.message("Registering a new display correlated to item stack: " + itemStack);
		String uuidString = UUID.randomUUID().toString();
		Transformation tr = display.getTransformation();
		ItemStack reference = new ItemStackBuilder(itemStack.getType())
				.name(Component.text((display instanceof ItemDisplay ? "Item Display" : "Block Display") + displayMap.size() + 1,
						display instanceof ItemDisplay ? TextColor.color(143, 81, 169) : TextColor.color(64, 114, 167)))
				.lore(List.of(
						Component.text("Transformation:"),
						Component.text("~  Translation: " + tr.getTranslation()),
						Component.text("~  LeftRotation: " + tr.getLeftRotation()),
						Component.text("~  Scale: " + tr.getScale()),
						Component.text("~  RightRotation: " + tr.getRightRotation()),
						Component.text("Origin Location: " + display.getLocation())))
				.tag("display_id", uuidString)
				.build();
		
		displayMap.put(uuidString, display);
		dev.message("Hash code of display map: " + Objects.hashCode(displayMap));
		dev.message("Display map size: " + displayMap.size());
		
		itemStackMap.put(uuidString, reference);
		
		if (pages.size() == 1) addPage();
		
		Inventory lastPage = pages.getLast();
		boolean isFull = Arrays.stream(lastPage.getContents()).noneMatch(item -> item == null || item.getType().isAir());
		dev.message("Is the last page full?: " + isFull);
		if (isFull) {
			addPage();
			lastPage = pages.getLast();
			
		}
		
		lastPage.addItem(reference);
		debugDump();
	}
	
	public void reLoreDisplayReference(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType().isAir()) return;
		
		ItemMeta meta = itemStack.getItemMeta();
		
		if (meta == null) return;
		
		String uuid = meta.getPersistentDataContainer().get(KeyCache.displayIdKey, PersistentDataType.STRING);
		if (uuid == null || uuid.isEmpty()) return;
		
		Display display = displayMap.get(uuid);
		
		Transformation tr = display.getTransformation();
		itemStack.lore(List.of(
				Component.text("Transformation:"),
				Component.text("~  Translation: " + tr.getTranslation()),
				Component.text("~  LeftRotation: " + tr.getLeftRotation()),
				Component.text("~  Scale: " + tr.getScale()),
				Component.text("~  RightRotation: " + tr.getRightRotation()),
				Component.text("Origin Location: " + display.getLocation())));
	}
	
	public void removeDisplay(String uuid) {
		Display display = displayMap.get(uuid);
		if (display == null) return;
		
		displayMap.remove(uuid);
		display.remove();
		
		if (pages.size() == 1) return;
		
		Inventory lastPage = pages.getLast();
		
		ItemStack reference = itemStackMap.get(uuid);
		lastPage.remove(reference);
		
		boolean isEmpty = Arrays.stream(lastPage.getContents()).allMatch(item -> item == null || item.getType().isAir() || item.isEmpty());
		
		if (isEmpty && pages.size() > 2) {
			if (curPageNum == pages.size()-1) {
				curPageNum -= 1;
				displayMenuPage(curPageNum);
			}
			removePage();
		}
	}
	
	public boolean shouldDisplayOriginParticles() {
		return shouldDisplayOriginParticles;
	}
	
	public void setShouldDisplayOriginParticles(boolean shouldDisplayOriginParticles) {
		this.shouldDisplayOriginParticles = shouldDisplayOriginParticles;
	}
	
	public void toggleDisplayOriginParticles(Developer developer, ClickType clickType) {
		shouldDisplayOriginParticles = !shouldDisplayOriginParticles;
	}
}
