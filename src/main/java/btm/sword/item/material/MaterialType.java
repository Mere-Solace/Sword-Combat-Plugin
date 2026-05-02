package btm.sword.item.material;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.config.Config;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.KeyRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Enum defining all material/crafting-ingredient types in the Sword item economy.
 * <p>
 * Each constant represents a discrete, stackable resource that players collect into their
 * material pouch. Materials are identified on {@link ItemStack}s via the
 * {@link KeyRegistry#MATERIAL_TYPE_KEY} PDC tag so they survive inventory manipulation.
 * </p>
 *
 * <p>To add a new material type, add a new constant here — no other registry changes needed.</p>
 */
public enum MaterialType {

    /** Ancient alloy fragments; a foundational crafting ingredient. */
    METAL_SCRAP(
        "metal_scrap",
        "Metal Scrap",
        Material.NETHERITE_SCRAP,
        List.of(
            Component.text("A fragment of ancient alloy.", Config.SwordColor.TEXT_ITEM_BASE)
                .decoration(TextDecoration.ITALIC, false),
            Component.text("Used in advanced forging recipes.", Config.SwordColor.TEXT_ITEM_BASE)
                .decoration(TextDecoration.ITALIC, false)
        )
    );

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<Component> lore;

    MaterialType(String id, String displayName, Material icon, List<Component> lore) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
    }

    /** @return the string id used as a PDC tag value and map key. */
    public String id() {
        return id;
    }

    /** @return the Bukkit {@link Material} used as the icon for this type in menus. */
    public Material icon() {
        return icon;
    }

    /** @return the display name {@link Component} for this material. */
    public Component displayName() {
        return Component.text(displayName, Config.SwordColor.TEXT_COOL)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false);
    }

    /** @return the lore lines for this material. */
    public List<Component> lore() {
        return lore;
    }

    /**
     * Builds a tagged {@link ItemStack} representing this material with the given stack size.
     * The item is tagged with {@link KeyRegistry#MATERIAL_TYPE_KEY} so it can be identified
     * when dropped in the world or placed in a player's inventory.
     *
     * @param amount the stack size; will be clamped to {@code [1, 64]}
     * @return a tagged ItemStack ready for use
     */
    public ItemStack buildItemStack(int amount) {
        ItemStack stack = new ItemStackBuilder(icon)
            .hideAll()
            .name(displayName())
            .lore(lore)
            .tag(KeyRegistry.MATERIAL_TYPE_KEY, PersistentDataType.STRING, id)
            .build();
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }

    /**
     * Reads the {@link KeyRegistry#MATERIAL_TYPE_KEY} PDC tag from an {@link ItemStack}
     * and returns the matching {@link MaterialType}, or {@code null} if not a material item.
     *
     * @param item the item to inspect
     * @return the corresponding {@link MaterialType}, or {@code null}
     */
    public static MaterialType fromItem(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String id = KeyRegistry.getKeyField(item, KeyRegistry.MATERIAL_TYPE_KEY, PersistentDataType.STRING);
        return id == null ? null : fromId(id);
    }

    /**
     * Returns the {@link MaterialType} matching the given string id, or {@code null}.
     *
     * @param id the material type id string
     * @return the matching constant, or {@code null}
     */
    public static MaterialType fromId(String id) {
        for (MaterialType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }
}
