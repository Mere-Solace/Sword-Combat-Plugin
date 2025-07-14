package btm.sword.system.inventory.selection;

import btm.sword.system.inventory.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChangePageButton extends MenuButton {
	private Menu nextMenu;
	private Menu previousMenu;
	private Menu targetMenu;
	
	public ChangePageButton(Material itemType, Component name, List<Component> lore, Menu targetMenu) {
		super(itemType, name, lore);
		this.nextMenu = targetMenu;
		this.previousMenu = targetMenu;
		this.targetMenu = targetMenu;
	}
	
	public ChangePageButton(Material itemType, Component name, List<Component> lore, Menu nextMenu, Menu previousMenu, Menu targetMenu) {
		super(itemType, name, lore);
		this.nextMenu = nextMenu;
		this.previousMenu = previousMenu;
		this.targetMenu = targetMenu;
	}
	
	public ChangePageButton(ItemStack itemStack, Menu targetMenu) {
		super(itemStack);
		this.nextMenu = targetMenu;
		this.previousMenu = targetMenu;
		this.targetMenu = targetMenu;
	}
	
	public ChangePageButton(ItemStack itemStack, Menu nextMenu, Menu previousMenu, Menu targetMenu) {
		super(itemStack);
		this.nextMenu = nextMenu;
		this.previousMenu = previousMenu;
		this.targetMenu = targetMenu;
	}
	
	@Override
	public boolean onClick(InventoryClickEvent e) {
		ClickType clickType = e.getClick();
		switch (clickType) {
			case RIGHT, SHIFT_RIGHT -> nextMenu.display((Player) e.getWhoClicked());
			case LEFT, SHIFT_LEFT -> previousMenu.display((Player) e.getWhoClicked());
			default -> targetMenu.display((Player) e.getWhoClicked());
		}
		return true;
	}
	
	@Override
	public @NotNull String getId() {
		return "change_page";
	}
	
	public Menu getPreviousMenu() {
		return previousMenu;
	}
	
	public void setPreviousMenu(Menu previousMenu) {
		this.previousMenu = previousMenu;
	}
	
	public Menu getTargetMenu() {
		return targetMenu;
	}
	
	public void setTargetMenu(Menu targetMenu) {
		this.targetMenu = targetMenu;
	}
	
	public Menu getNextMenu() {
		return nextMenu;
	}
	
	public void setNextMenu(Menu nextMenu) {
		this.nextMenu = nextMenu;
	}
}
