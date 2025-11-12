package btm.sword.system.item;

import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import btm.sword.Sword;

/**
 * Centralized registry and utility class for {@link NamespacedKey} management
 * throughout the Sword plugin.
 * <p>
 * This class acts as a global cache and utility hub for common plugin keys
 * used in {@link org.bukkit.persistence.PersistentDataContainer}s. All plugin
 * systems should prefer using these shared keys rather than creating new
 * {@code NamespacedKey} instances on the fly to ensure data consistency.
 * </p>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // Setting a tag
 * ItemStack stack = ...;
 * KeyRegistry.setString(stack, KeyRegistry.BUTTON_TAG, "menu_next");
 *
 * // Checking a tag
 * if (KeyRegistry.hasKey(stack, KeyRegistry.BUTTON_TAG)) {
 *     Bukkit.getLogger().info("Button tag found!");
 * }
 *
 * // Getting a tag
 * String tagValue = KeyRegistry.getString(stack, KeyRegistry.BUTTON_TAG);
 * </pre>
 *
 * @author Sword
 * @see NamespacedKey
 * @see org.bukkit.persistence.PersistentDataContainer
 */
public final class KeyRegistry {
    private static final Plugin PLUGIN = Sword.getInstance();

    // ------------------------------
    //  Commonly Used Keys
    // ------------------------------

    public static final String SOUL_LINK = "soul_link";

    public static final NamespacedKey SOUL_LINK_KEY = key(SOUL_LINK);

    /** Persistent data key for GUI buttons or menu items. */
    public static final String MAIN_MENU_BUTTON = "main_menu_button";

    /** Cached {@link NamespacedKey} for {@link #MAIN_MENU_BUTTON}. */
    public static final NamespacedKey MAIN_MENU_BUTTON_KEY = key(MAIN_MENU_BUTTON);

    /** Persistent data key for unique item identifiers (UUIDs). */
    public static final String ITEM_UUID = "uuid";

    /** Cached {@link NamespacedKey} for {@link #ITEM_UUID}. */
    public static final NamespacedKey ITEM_UUID_KEY = key(ITEM_UUID);

    /** Persistent data key for base weapon damage values. */
    public static final String BASE_DAMAGE = "damage";

    /** Cached {@link NamespacedKey} for {@link #BASE_DAMAGE}. */
    public static final NamespacedKey BASE_DAMAGE_KEY = key(BASE_DAMAGE);

    /** Persistent data key for skin/model data references. */
    public static final String MODEL_ID = "model_id";

    /** Cached {@link NamespacedKey} for {@link #MODEL_ID}. */
    public static final NamespacedKey MODEL_ID_KEY = key(MODEL_ID);

    // ------------------------------
    //  Utility Methods
    // ------------------------------

    private KeyRegistry() {
        // Utility class — prevent instantiation
    }

    /**
     * Creates a new {@link NamespacedKey} under the plugin's namespace.
     *
     * @param key the key name (e.g., "weapon_damage")
     * @return a new NamespacedKey under this plugin’s namespace
     */
    public static NamespacedKey key(String key) {
        return new NamespacedKey(PLUGIN, key);
    }

    /**
     * Checks whether the given {@link ItemStack} contains a persistent tag
     * associated with the specified {@link NamespacedKey}.
     *
     * @param itemStack the item to check
     * @param key the key to search for
     * @return {@code true} if the key exists, otherwise {@code false}
     */
    public static boolean hasKey(ItemStack itemStack, NamespacedKey key) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key);
    }

    /**
     * Retrieves a string value from an {@link ItemStack}'s persistent data container.
     *
     * @param itemStack the item to read from
     * @param key the key to retrieve
     * @return the stored string, or {@code null} if not found
     */
    public static String getString(ItemStack itemStack, NamespacedKey key) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(key, PersistentDataType.STRING);
    }

    /**
     * Stores a string value into an {@link ItemStack}'s persistent data container.
     * Automatically saves the updated {@link ItemMeta}.
     *
     * @param itemStack the item to modify
     * @param key the key to use
     * @param value the string value to store
     */
    public static void setString(ItemStack itemStack, NamespacedKey key, String value) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        itemStack.setItemMeta(meta);
    }

    /**
     * Removes a persistent data key from the given {@link ItemStack}, if present.
     *
     * @param itemStack the item to modify
     * @param key the key to remove
     * @return {@code true} if a value was removed, otherwise {@code false}
     */
    public static boolean removeKey(ItemStack itemStack, NamespacedKey key) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(key)) return false;

        container.remove(key);
        itemStack.setItemMeta(meta);
        return true;
    }

    /**
     * Returns all persistent keys currently stored in an item's data container.
     *
     * @param itemStack the item to inspect
     * @return a {@link Set} of {@link NamespacedKey}s found on the item
     */
    public static Set<NamespacedKey> listKeys(ItemStack itemStack) {
        if (itemStack == null) return Set.of();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return Set.of();
        return meta.getPersistentDataContainer().getKeys();
    }

    /**
     * Clears all persistent data stored on the given {@link ItemStack}.
     *
     * @param itemStack the item to clear
     */
    public static void clear(ItemStack itemStack) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (NamespacedKey key : container.getKeys()) {
            container.remove(key);
        }
        itemStack.setItemMeta(meta);
    }
}
