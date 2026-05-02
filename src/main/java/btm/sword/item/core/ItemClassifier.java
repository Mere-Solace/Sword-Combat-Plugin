package btm.sword.item.core;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Classifies {@link ItemStack}s into {@link ItemClass} values to determine
 * how the Sword input system should respond when a player holds or uses an item.
 * <p>
 * Classification priority (highest to lowest):
 * <ol>
 *   <li>Explicit PDC override via {@link KeyRegistry#ITEM_CLASS_KEY}</li>
 *   <li>Material-based detection for block materials and PDC throw-style items
 *       ({@link ItemClass#THROWABLE})</li>
 *   <li>Material-based detection for {@link ItemClass#USABLE} items
 *       (edible items, shield, bow, crossbow, fishing rod, firework rocket, potions)</li>
 *   <li>Material tag detection for {@link ItemClass#THROWABLE} items
 *       (swords, axes, pickaxes, shovels, hoes)</li>
 *   <li>{@link ItemClass#NEUTRAL} fallback</li>
 * </ol>
 * </p>
 *
 * <p>Custom items can be explicitly classified by storing the {@link ItemClass} name
 * as a string under {@link KeyRegistry#ITEM_CLASS_KEY} in the item's persistent data
 * container, overriding all material-based detection.</p>
 *
 * @see ItemClass
 * @see KeyRegistry#ITEM_CLASS_KEY
 */
public final class ItemClassifier {

    /**
     * Non-edible materials that preserve vanilla right-click behavior.
     */
    private static final Set<Material> USABLE_MATERIALS = Set.of(
        Material.SHIELD,
        Material.BOW,
        Material.CROSSBOW,
        Material.FISHING_ROD,
        Material.FIREWORK_ROCKET,
        Material.POTION,
        Material.SPLASH_POTION,
        Material.LINGERING_POTION
    );

    private ItemClassifier() {}

    /**
     * Classifies an item stack into an {@link ItemClass}.
     *
     * @param item the item to classify; may be null or empty
     * @return the item's classification; never null
     */
    public static ItemClass classify(ItemStack item) {
        if (item == null || item.isEmpty()) return ItemClass.NEUTRAL;

        // Explicit PDC override takes highest priority
        String explicit = KeyRegistry.getKeyField(item, KeyRegistry.ITEM_CLASS_KEY, PersistentDataType.STRING);
        if (explicit != null) {
            try {
                return ItemClass.valueOf(explicit);
            } catch (IllegalArgumentException ignored) {}
        }

        Material mat = item.getType();

        // Blocks are throwable regardless of other properties (e.g. cake is edible but throwable)
        if (mat.isBlock() || KeyRegistry.hasKey(item, KeyRegistry.THROW_STYLE_KEY)) {
            return ItemClass.THROWABLE;
        }

        if (mat.isEdible() || USABLE_MATERIALS.contains(mat)) {
            return ItemClass.USABLE;
        }

        if (isThrowableByMaterial(mat)) {
            return ItemClass.THROWABLE;
        }

        return ItemClass.NEUTRAL;
    }

    /**
     * Returns {@code true} if the item is {@link ItemClass#USABLE} — i.e., it has vanilla
     * right-click behavior and should not route right-click or drop through the Sword input tree.
     *
     * @param item the item to check; may be null or empty
     * @return true if the item is USABLE
     */
    public static boolean isUsable(ItemStack item) {
        return classify(item) == ItemClass.USABLE;
    }

    /**
     * Returns {@code true} if the item is {@link ItemClass#BLOCKED} — i.e., it is fully
     * managed by the Sword system and suppresses all inputs, routing them through
     * {@link btm.sword.entity.player.SwordPlayer#handleItemInteraction} instead.
     *
     * @param item the item to check; may be null or empty
     * @return true if the item is BLOCKED
     */
    public static boolean isBlocked(ItemStack item) {
        return classify(item) == ItemClass.BLOCKED;
    }

    /**
     * Returns {@code true} if the item's material falls under a vanilla melee weapon or
     * tool tag, or is a block material — making it eligible for the Sword throw system.
     * Uses Bukkit's {@link Tag} API for accurate, future-proof material detection.
     */
    private static boolean isThrowableByMaterial(Material mat) {
        return Tag.ITEMS_SWORDS.isTagged(mat)
            || Tag.ITEMS_AXES.isTagged(mat)
            || Tag.ITEMS_PICKAXES.isTagged(mat)
            || Tag.ITEMS_SHOVELS.isTagged(mat)
            || Tag.ITEMS_HOES.isTagged(mat)
            || mat.isBlock();
    }
}
