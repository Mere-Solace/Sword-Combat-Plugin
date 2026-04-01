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

    public Inventory topInventory() {
        return view.getTopInventory();
    }

    public InventoryType inventoryType() {
        return topInventory().getType();
    }

    public ItemStack topItem(int slot) {
        ItemStack item = topInventory().getItem(slot);
        return item == null ? ItemStack.empty() : item;
    }

    public void setTopItem(int slot, ItemStack item) {
        topInventory().setItem(slot, item);
    }
}
