package btm.sword.system.item;


import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

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

    /** Persistent data key marking an item as system-managed ({@code SpecialItem}). */
    public static final String SPECIAL_ITEM = "special_item";

    /** Cached {@link NamespacedKey} for {@link #SPECIAL_ITEM}. */
    public static final NamespacedKey SPECIAL_ITEM_KEY = key(SPECIAL_ITEM);


    /** Persistent data key marking an item as immovable by the player ({@code NonMovableItem}). */
    public static final String NON_MOVABLE = "non_movable";

    /** Cached {@link NamespacedKey} for {@link #NON_MOVABLE}. */
    public static final NamespacedKey NON_MOVABLE_KEY = key(NON_MOVABLE);



    public static final String SOUL_LINK = "soul_link";

    public static final NamespacedKey SOUL_LINK_KEY = key(SOUL_LINK);

    public static final String UMBRAL_BLADE = "umbral_blade";

    public static final NamespacedKey UMBRAL_BLADE_KEY = key(UMBRAL_BLADE);

    /** Persistent data key for GUI buttons or menu items. */
    public static final String MAIN_MENU_BUTTON = "main_menu_button";

    /** Cached {@link NamespacedKey} for {@link #MAIN_MENU_BUTTON}. */
    public static final NamespacedKey MAIN_MENU_BUTTON_KEY = key(MAIN_MENU_BUTTON);


    public static final String ITEM_TYPE = "item_type";

    public static final NamespacedKey ITEM_TYPE_KEY = key(ITEM_TYPE);


    public static final String ATTACK_STYLE = "attack_style";

    public static final NamespacedKey ATTACK_STYLE_KEY = key(ATTACK_STYLE);


    public static final String TOUGHNESS_DAMAGE_ADDER = "toughness_damage_adder";

    public static final NamespacedKey TOUGHNESS_DAMAGE_ADDER_KEY = key(TOUGHNESS_DAMAGE_ADDER);


    public static final String SHARD_DAMAGE_ADDER = "shard_damage_adder";

    public static final NamespacedKey SHARD_DAMAGE_ADDER_KEY = key(SHARD_DAMAGE_ADDER);


    public static final String BREAKABLE_WEAPON = "breakable_weapon";

    public static final NamespacedKey BREAKABLE_WEAPON_KEY = key(BREAKABLE_WEAPON);


    public static final String MAX_USES = "max_uses";

    public static final NamespacedKey MAX_USES_KEY = key(MAX_USES);


    /** Persistent data key for the throw style of a thrown item. */
    public static final String THROW_STYLE = "throw_style";

    /** Cached {@link NamespacedKey} for {@link #THROW_STYLE}. */
    public static final NamespacedKey THROW_STYLE_KEY = key(THROW_STYLE);


    /**
     * Persistent data key for an explicit {@link ItemClass} override.
     * Stores the {@link ItemClass} name as a string. When present, this value
     * takes priority over all material-based classification in {@link ItemClassifier}.
     */
    public static final String ITEM_CLASS = "item_class";

    /** Cached {@link NamespacedKey} for {@link #ITEM_CLASS}. */
    public static final NamespacedKey ITEM_CLASS_KEY = key(ITEM_CLASS);


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
    public static boolean hasKey(ItemStack itemStack, @NotNull NamespacedKey key) {
        return itemStack.getPersistentDataContainer().has(key);
    }

    /**
     * Stores a string value into an {@link ItemStack}'s persistent data container.
     * Automatically saves the updated {@link ItemMeta}.
     *
     * @param itemStack the item to modify
     * @param key the key to use
     * @param value the string value to store
     */
    public static <T, Z> void setKeyField(ItemStack itemStack, @NotNull NamespacedKey key, PersistentDataType<T, Z> dataType, Z value) {
        itemStack.editPersistentDataContainer(pdc -> pdc.set(key, dataType, value));
    }

    public static <T, Z> Z getKeyField(ItemStack itemStack, @NotNull NamespacedKey key, PersistentDataType<T, Z> dataType) {
        return itemStack.getPersistentDataContainer().get(key, dataType);
    }

    /**
     * Removes a persistent data key from the given {@link ItemStack}, if present.
     *
     * @param itemStack the item to modify
     * @param key the key to remove
     */
    public static void removeKey(ItemStack itemStack, @NotNull NamespacedKey key) {
        itemStack.editPersistentDataContainer(pdc -> pdc.remove(key));
    }
}
