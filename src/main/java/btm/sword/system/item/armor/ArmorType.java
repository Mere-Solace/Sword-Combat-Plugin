package btm.sword.system.item.armor;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Enumeration of all armor types available in the Sword plugin.
 *
 * <p>No constants are defined yet — this is a skeleton placeholder for future armor content.
 * Each constant will override {@link #buildItemStack()} when added.</p>
 */
public enum ArmorType {
    ;

    private final String id;

    ArmorType(String id) {
        this.id = id;
    }

    /**
     * Returns the unique string identifier for this armor type.
     *
     * @return the armor type id string
     */
    public String id() {
        return id;
    }

    /**
     * Builds and returns an {@link ItemStack} representing this armor type.
     *
     * @return the configured armor ItemStack, or {@link Material#AIR} if not yet implemented
     */
    public ItemStack buildItemStack() {
        return new ItemStack(Material.AIR);
    }

    /**
     * Resolves the {@link ArmorType} from an {@link ItemStack}.
     *
     * @param item the item to inspect
     * @return the matching ArmorType, or {@code null} if no match is found
     */
    public static ArmorType fromItem(ItemStack item) {
        return null;
    }
}
