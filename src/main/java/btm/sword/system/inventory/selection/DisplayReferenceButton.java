package btm.sword.system.inventory.selection;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class DisplayReferenceButton extends MenuButton {
	private final Display display;
	
	public DisplayReferenceButton(Display display, Component name, List<Component> lore) {
		super(display instanceof BlockDisplay bd ? bd.getBlock().getMaterial() : ((ItemDisplay) display).getItemStack().getType(), name, lore);
		this.display = display;
	}
	
	@Override
	public boolean onClick(InventoryClickEvent e) {
		ClickType clickType = e.getClick();
		switch (clickType) {
			case RIGHT -> itemStack.addUnsafeEnchantment(Enchantment.SHARPNESS, 255);
			case LEFT -> itemStack.removeEnchantments();
			case DOUBLE_CLICK -> display.remove();
		}
		return false;
	}
}
