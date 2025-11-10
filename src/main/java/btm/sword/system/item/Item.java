package btm.sword.system.item;

import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.Sword;
import lombok.Getter;

/**
 * Represents a wrapped item within the Sword system.
 * <p>
 * Each {@code Item} instance contains a reference to an underlying {@link ItemStack}
 * and a generated {@link UUID} that is also stored inside the item’s persistent data container
 * for later identification or retrieval.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
 * Item custom = new Item(stack);
 *
 * if (custom.hasTag("uuid")) {
 *     String id = custom.getTag("uuid");
 *     Bukkit.getLogger().info("This item's UUID is " + id);
 * }
 * </pre>
 * </p>
 *
 * @see org.bukkit.inventory.ItemStack
 * @see org.bukkit.persistence.PersistentDataContainer
 */
@Getter
public class Item {
    /** The underlying Bukkit ItemStack object. */
    private final ItemStack itemStack;
    /** A unique identifier assigned to this item instance. */
    private final UUID uuid;

    /**
     * Constructs a new Item wrapper around the given {@link ItemStack}.
     * <p>
     * A random {@link UUID} is generated and stored both in this object
     * and in the item’s persistent data container using the key {@code "uuid"}.
     * </p>
     *
     * @param itemStack the Bukkit ItemStack to associate with this Item
     */
    public Item(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.uuid = UUID.randomUUID();
        itemStack.getItemMeta().getPersistentDataContainer()
                .set(new NamespacedKey(Sword.getInstance(), "uuid"),
                    PersistentDataType.STRING, getUUIDString());
    }

    /**
     * Returns the unique UUID assigned to this item.
     *
     * @return the item’s UUID
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Returns the UUID as a string representation.
     *
     * @return this item’s UUID in string form
     */
    public String getUUIDString() {
        return uuid.toString();
    }

    /**
     * Retrieves a string tag value from this item’s persistent data container.
     *
     * @param key the key of the tag to retrieve
     * @return the stored string value, or {@code null} if not found
     */
    public String getTag(String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(new NamespacedKey(Sword.getInstance(), key), PersistentDataType.STRING);
    }

    /**
     * Checks if the item has a persistent tag with the given key.
     *
     * @param key the key to check for
     * @return {@code true} if the tag exists, otherwise {@code false}
     */
    public boolean hasTag(String key) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(new NamespacedKey(Sword.getInstance(), key), PersistentDataType.STRING);
    }

    /**
     * Checks whether this item has a UUID stored in its persistent data container.
     *
     * @return {@code true} if the item has a stored UUID tag
     */
    public boolean hasUUIDTag() {
        return hasTag("uuid");
    }

    /**
     * Retrieves the UUID stored in this item’s persistent data container, if available.
     *
     * @return the UUID found in persistent data, or {@code null} if not present or invalid
     */
    public UUID getStoredUUID() {
        String id = getTag("uuid");
        try {
            return id != null ? UUID.fromString(id) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Adds or updates a string tag in the item’s persistent data container.
     *
     * @param key   the key to store under your plugin namespace
     * @param value the string value to store
     */
    public void setTag(String key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(Sword.getInstance(), key), PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
    }
}
