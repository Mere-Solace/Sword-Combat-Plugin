package btm.sword.system.item;

import btm.sword.Sword;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Builder class for creating customized {@link ItemStack} instances.
 * <p>
 * Provides a fluent API for setting item properties such as name, lore, durability,
 * custom tags, and more. This follows the Builder design pattern to make item
 * creation more readable and maintainable.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * ItemStack sword = new ItemStackBuilder(Material.NETHERITE_SWORD)
 *     .name(Component.text("Legendary Blade"))
 *     .baseDamage(10.0)
 *     .unbreakable(true)
 *     .hideAll()
 *     .build();
 * }</pre>
 *
 * @see ItemStack
 * @see Item
 */
public class ItemStackBuilder {
    /** The ItemStack being built. */
    private final ItemStack item;

    /** The ItemMeta being modified. */
    private final ItemMeta meta;

    /** Plugin instance for creating NamespacedKeys. */
    private final Plugin plugin;

    /**
     * Constructs a new ItemStackBuilder for the specified material.
     * <p>
     * If the material has no default ItemMeta, a shield's meta is used as a fallback.
     * </p>
     *
     * @param material the material for the ItemStack
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
     * Sets the item's display name.
     *
     * @param component the display name component
     * @return this builder for chaining
     */
    public ItemStackBuilder name(Component component) {
        meta.itemName(component);
        return this;
    }

    /**
     * Sets the item's lore (description text).
     *
     * @param lore the list of lore components
     * @return this builder for chaining
     */
    public ItemStackBuilder lore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Sets whether the item is unbreakable.
     *
     * @param unbreakable true if the item should be unbreakable
     * @return this builder for chaining
     */
    public ItemStackBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Sets the item's durability damage.
     *
     * @param damage the damage value (higher = more damaged)
     * @return this builder for chaining
     */
    public ItemStackBuilder durability(int damage) {
        if (meta instanceof Damageable tool)
            tool.setDamage(damage);
        return this;
    }

    /**
     * Adds a custom string tag to the item's persistent data container.
     *
     * @param key the tag key
     * @param value the tag value
     * @return this builder for chaining
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
     * Adds a weapon-specific tag to the item's persistent data container.
     * <p>
     * This is functionally identical to {@link #tag(String, String)} but semantically
     * indicates the tag is weapon-related.
     * </p>
     *
     * @param key the tag key
     * @param value the tag value
     * @return this builder for chaining
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
     * Sets the base damage value for the weapon.
     * <p>
     * Stores the damage value in the persistent data container under the key "damage".
     * </p>
     *
     * @param value the base damage value
     * @return this builder for chaining
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
     * Sets the custom model data for item textures/models.
     *
     * @param identifier the custom model data identifier
     * @return this builder for chaining
     */
    public ItemStackBuilder skinData(int identifier) {
        meta.setCustomModelData(identifier);
        return this;
    }

    /**
     * Hides all item flags (attributes, enchantments, etc.).
     *
     * @return this builder for chaining
     */
    public ItemStackBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    /**
     * Builds and returns the configured ItemStack.
     * <p>
     * This applies all configured metadata to the ItemStack and returns it.
     * </p>
     *
     * @return the built ItemStack
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
