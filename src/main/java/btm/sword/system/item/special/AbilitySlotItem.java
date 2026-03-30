package btm.sword.system.item.special;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A {@link SlotAnchoredItem} managing one ability hotbar slot (slot 1 or 2).
 *
 * <p>The slot has four visual states:
 * <ul>
 *   <li>{@link SlotState#LOCKED} - gray glass, slot has not been unlocked.</li>
 *   <li>{@link SlotState#EMPTY} - gray glass, unlocked but no ability equipped.</li>
 *   <li>{@link SlotState#EQUIPPED} - the ability's world item, tagged and non-movable.</li>
 *   <li>{@link SlotState#DEPLETED} - black glass, ability uses exhausted.</li>
 * </ul>
 *
 * <p>{@link #isSatisfied(Player)} returns {@code true} when any of these four states
 * occupies the target slot. If none match, {@link #restore(Player)} replaces the slot
 * with the current state's item.</p>
 *
 * <p>These items follow the same protection pattern as the umbral blade and soul link -
 * they are only protected/restored when {@link btm.sword.utility.Debug#SPECIAL_ITEM_CHECKS_ENABLED}
 * is {@code true}. The {@code inventoryUpkeep()} loop already checks this for managed items.</p>
 */
public class AbilitySlotItem extends SlotAnchoredItem {

    /** Visual states for an ability hotbar slot. */
    public enum SlotState {
        /** Slot has not been unlocked yet. */
        LOCKED,
        /** Unlocked but no ability equipped. */
        EMPTY,
        /** An ability is equipped and has remaining uses. */
        EQUIPPED,
        /** Ability uses exhausted. */
        DEPLETED
    }

    /**
     * -- GETTER --
     *  Returns the current visual state of this slot.
     */
    @Getter
    private SlotState state;
    private final SwordItemType swordItemType;

    /**
     * The mutable item that represents the current visual state. Overrides the
     * immutable {@code itemStack} from {@link SpecialItem} for restore operations.
     */
    private ItemStack currentItem;

    /**
     * Constructs an {@code AbilitySlotItem} for the given hotbar slot.
     *
     * @param targetSlot    the inventory slot index (1 or 2)
     * @param swordItemType {@link SwordItemType#ACTIVE_1} or {@link SwordItemType#ACTIVE_2}
     * @param initialState  the initial visual state
     */
    public AbilitySlotItem(int targetSlot, SwordItemType swordItemType, SlotState initialState) {
        super(buildStateItem(initialState, swordItemType), targetSlot, KeyRegistry.ABILITY_SLOT_KEY);
        this.swordItemType = swordItemType;
        this.state = initialState;
        this.currentItem = buildStateItem(initialState, swordItemType);
    }

    /**
     * Updates the slot's visual state and internal item. Does not place the item
     * into the player's inventory - call {@link #restore(Player)} afterward.
     *
     * @param newState the new state
     */
    public void setState(SlotState newState) {
        this.state = newState;
        this.currentItem = buildStateItem(newState, swordItemType);
    }

    /**
     * Updates the slot to {@link SlotState#EQUIPPED} with a specific ability item.
     * The item is tagged with the ability slot PDC key and the appropriate {@link SwordItemType}.
     *
     * @param abilityItem the ability's world item (already tagged with {@code ability_id})
     */
    public void setEquipped(ItemStack abilityItem) {
        this.state = SlotState.EQUIPPED;
        ItemStack tagged = abilityItem.clone();
        KeyRegistry.setKeyField(tagged, KeyRegistry.ABILITY_SLOT_KEY, PersistentDataType.STRING,
            SlotState.EQUIPPED.name());
        KeyRegistry.setKeyField(tagged, KeyRegistry.ITEM_TYPE_KEY, PersistentDataType.STRING,
            swordItemType.string());
        KeyRegistry.setKeyField(tagged, KeyRegistry.NON_MOVABLE_KEY, PersistentDataType.BOOLEAN, true);
        this.currentItem = tagged;
    }

    /**
     * Returns {@code true} if the slot is satisfied.
     *
     * <p>For {@link SlotState#LOCKED} and {@link SlotState#EMPTY}, the slot is satisfied as long
     * as it does not contain a stale ability item (tagged with {@link KeyRegistry#ABILITY_SLOT_KEY}).
     * If one is found, the slot is unsatisfied so {@link #restore(Player)} can clear it.
     * For {@link SlotState#EQUIPPED} and {@link SlotState#DEPLETED}, the slot must contain an
     * item tagged with {@link KeyRegistry#ABILITY_SLOT_KEY}.</p>
     *
     * @param player the player whose inventory to check
     * @return {@code true} if the slot is occupied correctly or does not need enforcement
     */
    @Override
    public boolean isSatisfied(Player player) {
        ItemStack slotItem = player.getInventory().getItem(getTargetSlot());
        if (state == SlotState.LOCKED || state == SlotState.EMPTY) {
            // Satisfied unless a stale ability item is still sitting here
            return slotItem == null || !KeyRegistry.hasKey(slotItem, KeyRegistry.ABILITY_SLOT_KEY);
        }
        if (slotItem == null || slotItem.isEmpty()) return false;
        return KeyRegistry.hasKey(slotItem, KeyRegistry.ABILITY_SLOT_KEY);
    }

    /**
     * Places the current state's item into the target slot.
     *
     * <p>When a real ability item is displaced, it is first offered back to the player's
     * inventory and only falls back to a dropped-item world spawn if inventory is full.
     * Placeholder panes are never preserved.</p>
     *
     * @param player the player whose inventory to restore the item into
     */
    @Override
    public void restore(Player player) {
        ItemStack existing = player.getInventory().getItem(getTargetSlot());
        if (state == SlotState.LOCKED || state == SlotState.EMPTY) {
            if (existing != null && KeyRegistry.hasKey(existing, KeyRegistry.ABILITY_SLOT_KEY)) {
                player.getInventory().setItem(getTargetSlot(), null);
            }
            return;
        }

        ItemStack displaced = shouldPreserveOnReplace(existing) ? toInventoryItem(existing) : null;
        player.getInventory().setItem(getTargetSlot(), currentItem);
        if (displaced != null) {
            moveToInventoryOrDrop(player, displaced);
        }
    }

    private boolean shouldPreserveOnReplace(ItemStack existing) {
        if (existing == null || existing.isEmpty() || isCurrentSlotItem(existing)) {
            return false;
        }

        if (!KeyRegistry.hasKey(existing, KeyRegistry.ABILITY_SLOT_KEY)) {
            return toInventoryItem(existing) != null;
        }

        String existingAbilityId = KeyRegistry.getKeyField(existing, KeyRegistry.ABILITY_ID_KEY, PersistentDataType.STRING);
        String currentAbilityId = currentItem == null
            ? null
            : KeyRegistry.getKeyField(currentItem, KeyRegistry.ABILITY_ID_KEY, PersistentDataType.STRING);

        return existingAbilityId != null
            && currentAbilityId != null
            && !existingAbilityId.equals(currentAbilityId)
            && toInventoryItem(existing) != null;
    }

    private boolean isCurrentSlotItem(ItemStack existing) {
        return currentItem != null
            && existing.isSimilar(currentItem)
            && existing.getAmount() == currentItem.getAmount();
    }

    private ItemStack toInventoryItem(ItemStack existing) {
        if (existing == null || existing.isEmpty()) return null;

        if (KeyRegistry.hasKey(existing, KeyRegistry.ABILITY_SLOT_KEY)
            && !KeyRegistry.hasKey(existing, KeyRegistry.ABILITY_ID_KEY)) {
            return null;
        }

        ItemStack copy = existing.clone();
        KeyRegistry.removeKey(copy, KeyRegistry.ABILITY_SLOT_KEY);
        KeyRegistry.removeKey(copy, KeyRegistry.NON_MOVABLE_KEY);
        KeyRegistry.removeKey(copy, KeyRegistry.ITEM_TYPE_KEY);
        return copy;
    }

    private void moveToInventoryOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(leftover -> InteractiveItemArbiter.dropNaturally(player.getLocation(), leftover));
    }

    private static ItemStack buildStateItem(SlotState state, SwordItemType swordItemType) {
        return switch (state) {
            case LOCKED -> buildPlaceholder(Material.GRAY_STAINED_GLASS_PANE,
                Component.text("Locked Ability Slot", NamedTextColor.DARK_GRAY, TextDecoration.BOLD),
                List.of(Component.text("Unlock this slot from the Character Menu.", NamedTextColor.GRAY)),
                swordItemType);
            case EMPTY -> buildPlaceholder(Material.GRAY_STAINED_GLASS_PANE,
                Component.text("Empty Ability Slot", NamedTextColor.GRAY),
                List.of(Component.text("Equip an ability from the Character Menu.", NamedTextColor.GRAY)),
                swordItemType);
            case DEPLETED -> buildPlaceholder(Material.BLACK_STAINED_GLASS_PANE,
                Component.text("Depleted", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
                List.of(Component.text("This ability has been used up.", NamedTextColor.DARK_GRAY)),
                swordItemType);
            case EQUIPPED ->
                buildPlaceholder(Material.GRAY_STAINED_GLASS_PANE,
                    Component.text("Empty Ability Slot", NamedTextColor.GRAY),
                    List.of(Component.text("Equip an ability from the Character Menu.", NamedTextColor.GRAY)),
                    swordItemType);
        };
    }

    private static ItemStack buildPlaceholder(Material material, Component name,
                                              List<Component> lore, SwordItemType swordItemType) {
        ItemStack item = ItemStackBuilder.of(material)
            .name(name)
            .lore(lore)
            .tagSwordItem(swordItemType)
            .build();
        KeyRegistry.setKeyField(item, KeyRegistry.ABILITY_SLOT_KEY, PersistentDataType.STRING, "PLACEHOLDER");
        KeyRegistry.setKeyField(item, KeyRegistry.NON_MOVABLE_KEY, PersistentDataType.BOOLEAN, true);
        return item;
    }
}
