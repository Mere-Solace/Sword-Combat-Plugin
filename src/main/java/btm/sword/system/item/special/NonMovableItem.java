package btm.sword.system.item.special;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;

/**
 * A {@link SpecialItem} that cannot be moved, thrown, or used to cast abilities.
 * <p>
 * All inventory click, drag, and drop events for a {@code NonMovableItem} must be
 * cancelled by the relevant listeners. The item is tagged at construction time with
 * {@link KeyRegistry#NON_MOVABLE_KEY} so event handlers can identify it from the
 * raw {@link ItemStack} alone.
 * </p>
 *
 * <p>Use {@link #isNonMovable(ItemStack)} inside event handlers to decide whether
 * an action should be cancelled.</p>
 *
 * @see SlotAnchoredItem
 */
public abstract class NonMovableItem extends SpecialItem {

    /**
     * Constructs a {@code NonMovableItem} wrapping the given {@link ItemStack}.
     * Tags the stack with both {@link KeyRegistry#SPECIAL_ITEM_KEY} and
     * {@link KeyRegistry#NON_MOVABLE_KEY}.
     *
     * @param itemStack the ItemStack to wrap; must not be null or air
     */
    protected NonMovableItem(ItemStack itemStack) {
        super(itemStack);
        KeyRegistry.setKeyField(itemStack, KeyRegistry.NON_MOVABLE_KEY, PersistentDataType.BOOLEAN, true);
    }

    /**
     * Returns whether the given {@link ItemStack} is a {@code NonMovableItem}.
     * <p>
     * Use this inside inventory click, drag, and drop event handlers to decide
     * whether the action should be cancelled.
     * </p>
     *
     * @param item the stack to test; may be null
     * @return {@code true} if the stack carries {@link KeyRegistry#NON_MOVABLE_KEY}
     */
    public static boolean isNonMovable(ItemStack item) {
        if (!Debug.SPECIAL_ITEM_CHECKS_ENABLED) return false;
        return item != null && !item.isEmpty() && KeyRegistry.hasKey(item, KeyRegistry.NON_MOVABLE_KEY);
    }
}
