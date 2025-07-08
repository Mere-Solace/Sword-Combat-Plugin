package btm.sword.util.dev;

import btm.sword.system.entity.SwordPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DevMenu {
	public static final Component TITLE = Component.text("Development Menu", TextColor.color(119, 33, 180));
	
	public static Inventory create(SwordPlayer sp) {
		Inventory inv = Bukkit.createInventory(sp.player(), 72, TITLE);
		
		inv.setItem(3, new ItemStack(Material.SHIELD));
		
		return inv;
	}
}
