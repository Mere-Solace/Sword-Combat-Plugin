package btm.sword.system.item.special;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.item.KeyRegistry;
import lombok.Getter;

/**
 * Base class for all system-managed inventory items in the Sword plugin.
 * <p>
 * A {@code SpecialItem} wraps a Bukkit {@link ItemStack} and tags it with
 * {@link KeyRegistry#SPECIAL_ITEM_KEY} so it can be identified at runtime from any
 * inventory event. Subclasses add progressively stronger restrictions.
 * </p>
 *
 * <p>Hierarchy:</p>
 * <pre>
 * SpecialItem
 *   └─ NonMovableItem   (cannot be moved, thrown, or used to cast abilities)
 *        └─ SlotAnchoredItem  (auto-restores to a designated inventory slot)
 * </pre>
 *
 * @see NonMovableItem
 * @see SlotAnchoredItem
 */
@Getter
public abstract class SpecialItem {
    /** The underlying Bukkit ItemStack managed by this instance. */
    protected final ItemStack itemStack;

    /**
     * Constructs a {@code SpecialItem} wrapping the given {@link ItemStack}.
     * Tags the stack with {@link KeyRegistry#SPECIAL_ITEM_KEY} so it can be
     * identified from inventory events.
     *
     * @param itemStack the ItemStack to wrap; must not be null or air
     */
    protected SpecialItem(ItemStack itemStack) {
        this.itemStack = itemStack;
        KeyRegistry.setKeyField(itemStack, KeyRegistry.SPECIAL_ITEM_KEY, PersistentDataType.BOOLEAN, true);
    }

    /**
     * Returns whether the given {@link ItemStack} is a system-managed {@code SpecialItem}.
     *
     * @param item the stack to test; may be null
     * @return {@code true} if the stack carries {@link KeyRegistry#SPECIAL_ITEM_KEY}
     */
    public static boolean isSpecialItem(ItemStack item) {
        return item != null && !item.isEmpty() && KeyRegistry.hasKey(item, KeyRegistry.SPECIAL_ITEM_KEY);
    }
}
