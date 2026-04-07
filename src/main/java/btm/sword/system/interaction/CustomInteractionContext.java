package btm.sword.system.interaction;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Snapshot wrapper passed to custom inventory interactions.
 */
public record CustomInteractionContext(
    Player player,
    InventoryView view
) {

    public static final int SMITHING_TEMPLATE_SLOT = 0;
    public static final int SMITHING_BASE_SLOT = 1;
    public static final int SMITHING_ADDITION_SLOT = 2;
    public static final int SMITHING_RESULT_SLOT = 3;

    /** Returns the top (crafting) inventory from the player's current inventory view. */
    public Inventory topInventory() {
        return view.getTopInventory();
    }

    /** Returns the type of the top inventory. */
    public InventoryType inventoryType() {
        return topInventory().getType();
    }

    /** Returns the item in the given slot of the top inventory, or an empty stack if absent. */
    public ItemStack topItem(int slot) {
        ItemStack item = topInventory().getItem(slot);
        return item == null ? ItemStack.empty() : item;
    }

    /** Sets the item in the given slot of the top inventory. */
    public void setTopItem(int slot, ItemStack item) {
        topInventory().setItem(slot, item);
    }
}
