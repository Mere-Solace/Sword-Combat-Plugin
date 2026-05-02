package btm.sword.action.throwing;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/** Contract for world items that can be targeted and interacted with via the dash system. */
public interface InteractiveItem {
    /** Removes this item from the world and performs any required cleanup. */
    void dispose();
    /** Returns the {@link ItemDisplay} entity representing this item in the world. */
    ItemDisplay getDisplay();
    /** Returns the {@link ItemStack} represented by this interactive item. */
    ItemStack getItemStack();
}
