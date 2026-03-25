package btm.sword.system.item.special;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A {@link SlotAnchoredItem} managing one ability hotbar slot (slot 1 or 2).
 *
 * <p>The slot has four visual states:
 * <ul>
 *   <li>{@link SlotState#LOCKED} — gray glass, slot has not been unlocked.</li>
 *   <li>{@link SlotState#EMPTY} — gray glass, unlocked but no ability equipped.</li>
 *   <li>{@link SlotState#EQUIPPED} — the ability's world item, tagged and non-movable.</li>
 *   <li>{@link SlotState#DEPLETED} — black glass, ability uses exhausted.</li>
 * </ul>
 *
 * <p>{@link #isSatisfied(Player)} returns {@code true} when any of these four states
 * occupies the target slot. If none match, {@link #restore(Player)} replaces the slot
 * with the current state's item.</p>
 *
 * <p>These items follow the same protection pattern as the umbral blade and soul link —
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
     * Returns the current visual state of this slot.
     *
     * @return the current {@link SlotState}
     */
    public SlotState getState() {
        return state;
    }

    /**
     * Updates the slot's visual state and internal item. Does not place the item
     * into the player's inventory — call {@link #restore(Player)} afterward.
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
     * Returns {@code true} if the target slot holds any valid ability-slot item
     * (locked, empty, equipped, or depleted placeholder).
     *
     * @param player the player whose inventory to check
     * @return {@code true} if the slot is occupied by an ability-slot item
     */
    @Override
    public boolean isSatisfied(Player player) {
        ItemStack slotItem = player.getInventory().getItem(getTargetSlot());
        if (slotItem == null || slotItem.isEmpty()) return false;
        return KeyRegistry.hasKey(slotItem, KeyRegistry.ABILITY_SLOT_KEY);
    }

    /**
     * Places the current state's item into the target slot.
     *
     * @param player the player whose inventory to restore the item into
     */
    @Override
    public void restore(Player player) {
        player.getInventory().setItem(getTargetSlot(), currentItem);
    }

    // ── Static item builders ─────────────────────────────────────────────────

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
            case EQUIPPED -> // Equipped items are set via setEquipped(); this fallback should not normally be reached.
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
