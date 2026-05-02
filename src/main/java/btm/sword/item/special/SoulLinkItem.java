package btm.sword.item.special;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.item.core.KeyRegistry;

/**
 * A {@link SlotAnchoredItem} for the Soul Link, anchored to hotbar slot 0.
 * <p>
 * Soul Link and Umbral Blade are interchangeable in slot 0 — the slot is considered
 * satisfied when either item is present, so {@link #isSatisfied(Player)} accepts both
 * {@link KeyRegistry#SOUL_LINK_KEY} and {@link KeyRegistry#UMBRAL_BLADE_KEY}.
 * {@link #restore(Player)} always restores the Soul Link specifically (the blade manages
 * its own placement via the Umbral state machine).
 * </p>
 */
public class SoulLinkItem extends SlotAnchoredItem {

    /** Inventory slot shared by Soul Link and Umbral Blade. */
    public static final int SLOT = 0;

    /**
     * Constructs a {@code SoulLinkItem} anchored to {@link #SLOT}.
     *
     * @param itemStack the Soul Link ItemStack; must carry {@link KeyRegistry#SOUL_LINK_KEY}
     */
    public SoulLinkItem(ItemStack itemStack) {
        super(itemStack, SLOT, KeyRegistry.SOUL_LINK_KEY);
    }

    /**
     * Returns {@code true} if slot {@link #SLOT} holds either the Soul Link or the
     * Umbral Blade. Both are acceptable occupants of this shared anchor slot.
     *
     * @param player the player whose inventory to check
     * @return {@code true} if slot 0 contains a Soul Link or Umbral Blade
     */
    @Override
    public boolean isSatisfied(Player player) {
        ItemStack slotItem = player.getInventory().getItem(SLOT);
        if (slotItem == null || slotItem.isEmpty()) return false;
        return KeyRegistry.hasKey(slotItem, KeyRegistry.SOUL_LINK_KEY)
            || KeyRegistry.hasKey(slotItem, KeyRegistry.UMBRAL_BLADE_KEY);
    }
}
