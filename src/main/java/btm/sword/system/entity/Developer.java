package btm.sword.system.entity;

import btm.sword.system.inventory.Menu;
import btm.sword.system.inventory.MenuActions;
import btm.sword.system.inventory.PaginatedMenu;
import btm.sword.system.inventory.selection.ChangePageButton;
import btm.sword.system.inventory.selection.CustomActionButton;
import btm.sword.system.inventory.selection.DisplayReferenceButton;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.playerdata.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class Developer extends SwordPlayer {
	private final Menu devMenu;
	private final PaginatedMenu displays = new PaginatedMenu();
	
	private static final int DEV_MENU_SIZE = 27;
	private static final int DISPLAY_MENU_SIZE = 54;
	private static final int DISPLAY_MENU_L_PAD = 2;
	
	public Developer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data);
		
		devMenu = new Menu(Component.text("Development Tools"), DEV_MENU_SIZE);
		predefinedMenus.add(devMenu);
		displays.addMenuPage(new Menu(Component.text("Current Displays"), DISPLAY_MENU_SIZE, DISPLAY_MENU_L_PAD, 0, 0, 0));
		predefinedMenus.add(displays.getFirst());
		
		mainMenu.setButton(0,
				new ChangePageButton(Material.ECHO_SHARD, Component.text("Development Tools"), null, devMenu));
		
		devMenu.setButton(0,
				new ChangePageButton(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Component.text("Go Back"), null, mainMenu));
		devMenu.setButton(8,
				new ChangePageButton(Material.PAPER, Component.text("View Displays"), null, displays.getFirst()));
		devMenu.setButton(13,
				new CustomActionButton(Material.DISC_FRAGMENT_5, Component.text("Spawn Display"), null, MenuActions::spawnDisplay));
		
		displays.getFirst().setButton(0,
				new ChangePageButton(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Component.text("Go Back"), null, devMenu));
		displays.getFirst().setDecoration(9,
				new ItemStackBuilder(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE)
						.name(Component.text("- Guide -"))
						.lore(List.of(
								Component.text("Right Click -> Select the display"),
								Component.text("Left Click -> De-select the display"),
								Component.text("Double Click -> Delete display")
						))
						.hideAll()
						.build());
	}
	
	public void addDisplay(Display display) {
		if (displays.getLast().isFull()) {
			displays.getLast().setButton(8,
					new ChangePageButton(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Component.text("Next Page"), null, devMenu));
			int prev = displays.getPages().size() - 1;
			displays.addMenuPage(new Menu(Component.text("Current Displays"), DISPLAY_MENU_SIZE, DISPLAY_MENU_L_PAD, 0, 0, 0));
			displays.getLast().setButton(0,
					new ChangePageButton(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Component.text("Last Page"), null, null, displays.getPage(prev), devMenu));
		}
		displays.getLast().addButton(
				new DisplayReferenceButton(display,
						Component.text(
								String.valueOf(
										display instanceof BlockDisplay bd ?
												bd.getBlock().getMaterial() :
												((ItemDisplay) display).getItemStack().getType())),
						null));
	}
	
	public void removeDisplay(Display display) {
		for (Menu menu : displays.getPages()) {
			menu.removeButton(display.getUniqueId().toString());
		}
		display.remove();
		
		if (displays.getLast().isEmpty()) {
			displays.removeLast();
		}
	}
}
