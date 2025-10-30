package btm.sword.system.item;

import btm.sword.Sword;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Wraps an {@link ItemStack} with a unique identifier for tracking.
 * <p>
 * Each Item instance is assigned a UUID which is stored in the ItemStack's
 * persistent data container. This allows items to be uniquely identified
 * and tracked across the plugin.
 * </p>
 *
 * @see ItemStack
 * @see ItemStackBuilder
 */
@Getter
public class Item {
    /** The wrapped ItemStack. */
    private final ItemStack itemStack;

    /** The unique identifier for this item. */
    private final UUID uuid;

    /**
     * Constructs a new Item wrapper for the specified ItemStack.
     * <p>
     * A new UUID is generated and stored in the ItemStack's persistent data container
     * under the key "uuid".
     * </p>
     *
     * @param itemStack the ItemStack to wrap
     */
    public Item(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.uuid = UUID.randomUUID();
        itemStack.getItemMeta().getPersistentDataContainer()
                .set(new NamespacedKey(Sword.getInstance(), "uuid"),
                    PersistentDataType.STRING, getUUIDString());
    }

    /**
     * Gets the unique identifier for this item.
     *
     * @return the UUID
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Gets the unique identifier as a string.
     *
     * @return the UUID as a string
     */
    public String getUUIDString() {
        return uuid.toString();
    }
}
