package btm.sword.system.inventory;

import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DevMenu {
	public static final Component TITLE = Component.text("~* | Development Menu | *~", TextColor.color(191, 83, 30));
	private static final int SIZE = 54;
	
	private static final ArrayList<ItemStack[]> pages = new ArrayList<>();
	
	private static final HashMap<ItemStack, TriConsumer<SwordPlayer, ClickType, InventoryAction>> buttonPairings = new HashMap<>();
	
	private static final HashMap<BlockDisplay, SwordPlayer> workingBlocks = new HashMap<>();
	private static final HashMap<ItemDisplay, SwordPlayer> workingItems = new HashMap<>();
	
	static {
		ItemStack[] page1 = new ItemStack[SIZE];
		page1[13] = createButton(Material.SHIELD, "Test Quaternion Behavior", "Display a standard shield item display", MenuAction::spawnShieldDisplay);
		page1[31] = createButton(Material.SPRUCE_LOG, "Test Quaternion Behavior", "Display a standard log block display", MenuAction::spawnBlockDisplay);
		pages.add(page1);
	}
	
	public static Inventory createMenuPage(Player player, int pageNumber) {
		Inventory menu = Bukkit.createInventory(player, SIZE, TITLE);
		
		menu.setContents(pages.get(Math.max(Math.min(pageNumber, pages.size()-1), 0)));
		
		return menu;
	}
	
	public static ItemStack createButton(Material material, String name, String lore, TriConsumer<SwordPlayer, ClickType, InventoryAction> action) {
		ItemStack item = new ItemStackBuilder(material)
				.name(Component.text(name, TextColor.color(191, 104, 7)))
				.lore(List.of(Component.text(lore, TextColor.color(143, 146, 168), TextDecoration.ITALIC)))
				.tag("type", "button")
				.build();
		
		buttonPairings.put(item, action);
		
		return item;
	}
	
	public static void handleInput(ItemStack item, SwordPlayer player, ClickType clickType, InventoryAction action) {
		buttonPairings.get(item).accept(player, clickType, action);
	}
	
	public static void put() {
	
	}
}
