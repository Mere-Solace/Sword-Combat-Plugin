package btm.sword.system.item.special;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.item.KeyRegistry;
import lombok.Getter;

/**
 * A {@link NonMovableItem} that is bound to a specific inventory slot.
 * <p>
 * If the item is absent from its {@link #targetSlot}, {@link #restore(Player)} places
 * it back. Call {@link #isSatisfied(Player)} first to avoid an unnecessary overwrite.
 * </p>
 *
 * <p>The default satisfaction check verifies that {@link #targetSlot} contains a stack
 * tagged with the {@code identityKey} supplied at construction. Subclasses may override
 * {@link #isSatisfied(Player)} to accept alternative items in the slot (e.g., the Soul
 * Link / Umbral Blade shared anchor).</p>
 *
 * @see SoulLinkItem
 */
@Getter
public class SlotAnchoredItem extends NonMovableItem {
    /** Inventory slot index this item must always occupy. */
    private final int targetSlot;

    /**
     * The {@link NamespacedKey} used to verify whether the correct item occupies
     * {@link #targetSlot}. Compared against the slot's persistent data container.
     */
    private final NamespacedKey identityKey;

    /**
     * Constructs a {@code SlotAnchoredItem}.
     *
     * @param itemStack   the ItemStack to anchor; must not be null or air
     * @param targetSlot  the inventory slot index this item must always occupy
     * @param identityKey the {@link NamespacedKey} used to identify a satisfying item
     *                    in the target slot
     */
    public SlotAnchoredItem(ItemStack itemStack, int targetSlot, NamespacedKey identityKey) {
        super(itemStack);
        this.targetSlot = targetSlot;
        this.identityKey = identityKey;
    }

    /**
     * Returns {@code true} if {@link #targetSlot} currently holds an item tagged with
     * {@link #identityKey}. Subclasses may override this to accept alternative items.
     *
     * @param player the player whose inventory to check
     * @return {@code true} if the slot is occupied by an acceptable item
     */
    public boolean isSatisfied(Player player) {
        ItemStack slotItem = player.getInventory().getItem(targetSlot);
        return slotItem != null && !slotItem.isEmpty() && KeyRegistry.hasKey(slotItem, identityKey);
    }

    /**
     * Places {@link #itemStack} into {@link #targetSlot}, restoring the anchored item.
     * <p>
     * Call {@link #isSatisfied(Player)} before invoking this to avoid an unnecessary
     * overwrite when the correct item is already present.
     * </p>
     *
     * @param player the player whose inventory to restore the item into
     */
    public void restore(Player player) {
        player.getInventory().setItem(targetSlot, itemStack);
    }
}
