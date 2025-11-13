package btm.sword.system.item;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.profile.PlayerProfile;

import btm.sword.Sword;
import net.kyori.adventure.text.Component;

/**
 * Builder utility for creating and customizing {@link ItemStack} instances in a fluent style.
 * <p>
 * Example usage:
 * <pre>
 * ItemStack custom = new ItemStackBuilder(Material.DIAMOND_SWORD)
 *     .name(Component.text("Epic Blade"))
 *     .lore(List.of(Component.text("Forged in the fires of the Jokers")))
 *     .unbreakable(true)
 *     .durability(10)
 *     .tag("customKey","customValue")
 *     .baseDamage(12.5)
 *     .customModelData(123)  // use new API
 *     .hideAll()
 *     .build();
 * </pre>
 * </p>
 *
 * @see ItemStack
 * @see ItemMeta
 * @see SkullMeta
 */
public class ItemStackBuilder {
    private final ItemStack item;
    private ItemMeta meta;
    private final Plugin plugin;

    /**
     * Creates an ItemStackBuilder for the given material.
     * If the item’s meta is null, it falls back to a default shield meta.
     *
     * @param material the material to build the item from
     */
    public ItemStackBuilder(Material material) {
        ItemMeta preMeta;
        this.item = new ItemStack(material);
        preMeta = item.getItemMeta();
        if (preMeta == null)
            preMeta = new ItemStack(Material.SHIELD).getItemMeta();
        this.meta = preMeta;
        this.plugin = Sword.getInstance();
    }

    /**
     * Sets the display name of the item.
     *
     * @param component the component representing the name to set
     * @return this builder, for chaining
     */
    public ItemStackBuilder name(Component component) {
        meta.itemName(component);
        return this;
    }

    /**
     * Sets the lore of the item.
     *
     * @param lore a list of Component instances representing the lore lines
     * @return this builder, for chaining
     */
    public ItemStackBuilder lore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Marks the item as unbreakable (or not).
     *
     * @param unbreakable true to make the item unbreakable; false otherwise
     * @return this builder, for chaining
     */
    public ItemStackBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Sets the damage value (durability) of the item if it is damageable.
     *
     * @param damage the damage to set (the higher the number, the more used the item appears)
     * @return this builder, for chaining
     */
    public ItemStackBuilder durability(int damage) {
        if (meta instanceof Damageable tool)
            tool.setDamage(damage);
        return this;
    }

    /**
     * Adds a persistent data tag (string) to the item.
     *
     * @param key the new key string (namespaced under your plugin) to store
     * @param value the string value to store
     * @return this builder, for chaining
     */
    public ItemStackBuilder tag(String key, String value) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, key),
                PersistentDataType.STRING,
                value
        );
        return this;
    }

    /**
     * Adds a persistent data tag (string) to the item.
     *
     * @param key the key (namespaced under your plugin) to store
     * @param value the string value to store
     * @return this builder, for chaining
     */
    public ItemStackBuilder tag(NamespacedKey key, String value) {
        meta.getPersistentDataContainer().set(
                key,
                PersistentDataType.STRING,
                value
        );
        return this;
    }

    /**
     * Adds a “weapon” tag (string) to the item in the persistent data container.
     * <p>
     * This is functionally identical to {@link #tag(String, String)} but indicates intent.
     * </p>
     *
     * @param key   the key (namespaced under your plugin) to store
     * @param value the string value to store
     * @return this builder, for chaining
     */
    public ItemStackBuilder weaponTag(String key, String value) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, key),
                PersistentDataType.STRING,
                value
        );
        return this;
    }

    /**
     * Sets the base damage value (double) for this item via persistent data.
     *
     * @param value the base damage value to store
     * @return this builder, for chaining
     */
    public ItemStackBuilder baseDamage(double value) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "damage"),
                PersistentDataType.DOUBLE,
                value
        );
        return this;
    }

    /**
     * Sets the custom model data using the newer API.
     * <p>
     * The legacy {@link ItemMeta#setCustomModelData(Integer)} is deprecated. According to the newer API
     * documentation, you should use {@link ItemMeta#setCustomModelDataComponent(CustomModelDataComponent)} instead.
     * </p>
     *
     * @param identifier the custom model data identifier (or other custom model data structure)
     * @return this builder, for chaining
     */
    @SuppressWarnings("all")
    public ItemStackBuilder customModelData(int identifier) {
        // Using new, experimental API methods:
        CustomModelDataComponent cmc = meta.getCustomModelDataComponent();
        // Clear or initialize as needed; in the simplest case we just set a single float value list:
        cmc.setFloats(List.of((float) identifier));
        meta.setCustomModelDataComponent(cmc);
        return this;
    }

    /**
     * Hides all item flags (makes the item display cleaner by hiding enchants, attributes, etc.).
     *
     * @return this builder, for chaining
     */
    public ItemStackBuilder hideAll() {
        meta.addItemFlags(
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_STORED_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Applies a player-head specific profile to this item if it is a skull/head.
     * <p>
     * Example usage:
     * <pre>
     * ItemStack head = new ItemStackBuilder(Material.PLAYER_HEAD)
     *      .playerHead(profile)
     *      .build();
     * </pre>
     * </p>
     *
     * @param profile the {@link PlayerProfile} to set on the skull meta
     * @return this builder, for chaining
     * @throws IllegalStateException if the underlying meta is not a {@link SkullMeta}
     */
    public ItemStackBuilder playerHead(PlayerProfile profile) {
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new IllegalStateException("ItemMeta for material is not SkullMeta");
        }
        skullMeta.setPlayerProfile(profile);

        this.item.setItemMeta(skullMeta);

        ItemMeta newMeta = item.getItemMeta();
        if (newMeta == null) {
            throw new IllegalStateException("Could not retrieve SkullMeta after setting player profile");
        }

        this.meta = newMeta;
        return this;
    }

    /**
     * Builds and returns the configured {@link ItemStack}.
     *
     * @return the built ItemStack with all applied meta
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
