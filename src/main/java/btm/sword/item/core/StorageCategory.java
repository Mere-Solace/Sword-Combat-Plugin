package btm.sword.item.core;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.config.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Identifies one of the three inventory storage-shortcut slots anchored in the
 * column directly above the Main Menu button (hotbar slot 8).
 *
 * <p>Slot layout (top → bottom, above slot 8):</p>
 * <pre>
 *   slot 17 — QUEST
 *   slot 26 — MATERIAL
 *   slot 35 — CURRENCY
 *   slot  8 — Main Menu (hotbar)
 * </pre>
 *
 * <p>Each category is tagged onto its {@link ItemStack} via
 * {@link KeyRegistry#STORAGE_BUTTON_KEY} and routed in
 * {@code SwordPlayer#handleItemInteraction} and
 * {@code SwordPlayer#handleInventoryInput}.</p>
 *
 * <p>The {@link #loreSupplier} stored on each constant is a static default
 * (returns an empty list). Per-player dynamic lore is supplied by
 * {@code SwordPlayer} and applied at item build and upkeep time.</p>
 */
public enum StorageCategory {

    /** Currency and tradeable resources (slot 35, just above hotbar). */
    CURRENCY(
        "currency",
        "Currency Storage",
        () -> Config.Materials.CURRENCY_STORAGE_ITEM,
        35,
        List::of
    ),

    /** Crafting materials and gathered resources (slot 26). */
    MATERIAL(
        "material",
        "Material Storage",
        () -> Config.Materials.MATERIAL_STORAGE_ITEM,
        26,
        List::of
    ),

    /** Quest items and unique/special rewards (slot 17). */
    QUEST(
        "quest",
        "Quest Storage",
        () -> Config.Materials.QUEST_STORAGE_ITEM,
        17,
        List::of
    );

    private final String string;
    private final String displayName;
    private final Supplier<Material> material;
    private final int slot;
    /** Default static lore supplier. Per-player lore is provided by {@code SwordPlayer}. */
    private final Supplier<List<Component>> loreSupplier;

    private static final HashMap<String, StorageCategory> MAP_TO_ENUM = new HashMap<>();

    static {
        for (StorageCategory c : values()) {
            MAP_TO_ENUM.put(c.string, c);
        }
    }

    StorageCategory(
        String string,
        String displayName,
        Supplier<Material> material,
        int slot,
        Supplier<List<Component>> loreSupplier
    ) {
        this.string = string;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.loreSupplier = loreSupplier;
    }

    /** @return the PDC string value for this category. */
    public String string() {
        return string;
    }

    /** @return the inventory slot this button occupies. */
    public int slot() {
        return slot;
    }

    /** @return the {@link Material} for this button, resolved from config at call time. */
    public Material material() {
        return material.get();
    }

    /** @return the default lore for this category (empty list for all built-in constants). */
    public List<Component> lore() {
        return loreSupplier.get();
    }

    /** @return the display name {@link Component} for the button item. */
    public Component displayName() {
        return Component.text("~ | " + displayName + " | ~", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Reads the {@link StorageCategory} tag from an {@link ItemStack}.
     * Returns {@code null} if the item has no storage button tag.
     *
     * @param item the item to inspect
     * @return the category, or {@code null} if not a storage button
     */
    public static StorageCategory fromItem(ItemStack item) {
        String val = KeyRegistry.getKeyField(item, KeyRegistry.STORAGE_BUTTON_KEY, PersistentDataType.STRING);
        return val == null ? null : MAP_TO_ENUM.get(val);
    }
}
