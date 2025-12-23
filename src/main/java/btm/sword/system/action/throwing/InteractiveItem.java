package btm.sword.system.action.throwing;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public interface InteractiveItem {
    void dispose();
    ItemDisplay getDisplay();
    ItemStack getItemStack();
}
