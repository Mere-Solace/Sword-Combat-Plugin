package btm.sword.system.entity;

import btm.sword.system.inventory.DevMenu;
import btm.sword.system.playerdata.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class Developer extends SwordPlayer {
	private final DevMenu devMenu;
	
	public Developer(LivingEntity associatedEntity, PlayerData data) {
		super(associatedEntity, data);
		devMenu = new DevMenu(this);
		
	}
	
	public void handleMenuInput(ItemStack item, ClickType clickType) {
		devMenu.handleInput(item, clickType);
	}
	
	public DevMenu getDevMenu() {
		return devMenu;
	}
}
