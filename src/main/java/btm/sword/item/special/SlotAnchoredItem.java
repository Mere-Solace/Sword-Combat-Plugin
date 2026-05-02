package btm.sword.item.special;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.item.core.KeyRegistry;
import lombok.Getter;
import lombok.Setter;

/**
 * A {@link NonMovableItem} that is bound to a specific inventory slot.
 * <p>
 * If the item is absent from its {@link #targetSlot}, {@link #restore(Player)} places
 * it back. Call {@link #isSatisfied(Player)} first to avoid an unnecessary overwrite.
 * </p>
 *
 * <p>Satisfaction is determined by a {@link Predicate} supplied at construction time.
 * Use the {@link NamespacedKey} constructor for custom Sword items identified by PDC tag,
 * or the {@link Material} constructor for standard vanilla items identified by material type.</p>
 *
 * @see SoulLinkItem
 */
public class SlotAnchoredItem extends NonMovableItem {
    /** Inventory slot index this item must always occupy. */
    @Getter
    private final int targetSlot;

    private final Predicate<ItemStack> satisfactionCheck;

    /**
     * Controls whether {@link btm.sword.entity.player.SwordPlayer}'s periodic upkeep should
     * attempt to restore this item. Set to {@code false} to suppress automatic replacement
     * (e.g., during cutscenes or creative dev mode) without removing the item from its slot.
     * -- SETTER --
     *  Sets whether periodic upkeep should restore this item.
     *  Does not affect explicit calls to
     * .
     * -- GETTER --
     *  Returns whether periodic upkeep should restore this item.
     */
    @Getter
    @Setter
    private boolean upkeepEnabled = true;

    /**
     * Constructs a {@code SlotAnchoredItem} that is satisfied when the target slot
     * contains an item tagged with the given {@link NamespacedKey}.
     * Use this for custom Sword items identified by a PDC key.
     *
     * @param itemStack   the ItemStack to anchor; must not be null or air
     * @param targetSlot  the inventory slot index this item must always occupy
     * @param identityKey the {@link NamespacedKey} used to identify a satisfying item
     */
    public SlotAnchoredItem(ItemStack itemStack, int targetSlot, NamespacedKey identityKey) {
        super(itemStack);
        this.targetSlot = targetSlot;
        this.satisfactionCheck = item -> KeyRegistry.hasKey(item, identityKey);
    }

    /**
     * Constructs a {@code SlotAnchoredItem} that is satisfied when the target slot
     * contains an item of the given {@link Material}.
     * Use this for vanilla items that need no custom identification.
     *
     * @param itemStack        the ItemStack to anchor; must not be null or air
     * @param targetSlot       the inventory slot index this item must always occupy
     * @param identityMaterial the material type used to identify a satisfying item
     */
    public SlotAnchoredItem(ItemStack itemStack, int targetSlot, Material identityMaterial) {
        super(itemStack);
        this.targetSlot = targetSlot;
        this.satisfactionCheck = item -> item.getType() == identityMaterial;
    }

    /**
     * Returns {@code true} if {@link #targetSlot} currently holds an item that passes
     * the satisfaction check supplied at construction. Subclasses may override this
     * to accept alternative items (e.g., the Soul Link / Umbral Blade shared anchor).
     *
     * @param player the player whose inventory to check
     * @return {@code true} if the slot is occupied by an acceptable item
     */
    public boolean isSatisfied(Player player) {
        ItemStack slotItem = player.getInventory().getItem(targetSlot);
        return slotItem != null && !slotItem.isEmpty() && satisfactionCheck.test(slotItem);
    }

    /**
     * Places {@link #itemStack} into {@link #targetSlot}, restoring the anchored item.
     * This is an unconditional write — it does not check {@link #upkeepEnabled}.
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
