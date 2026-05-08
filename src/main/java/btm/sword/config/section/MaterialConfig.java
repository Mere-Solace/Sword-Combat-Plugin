package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.Material;

import btm.sword.config.Config;

/**
 * Material overrides for inventory UI buttons and storage shortcut items.
 */
public final class MaterialConfig {

    private MaterialConfig() {}

    /** Material used for the Currency Storage shortcut button in the player's inventory. */
    public static Material CURRENCY_STORAGE_ITEM = Material.NETHERITE_INGOT;
    static { register(
        "materials.currency_storage_item",
        CURRENCY_STORAGE_ITEM, Material.class,
        v -> CURRENCY_STORAGE_ITEM = v,
        Config::loadMaterial); }

    /** Material used for the Material Storage shortcut button in the player's inventory. */
    public static Material MATERIAL_STORAGE_ITEM = Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE;
    static { register(
        "materials.material_storage_item",
        MATERIAL_STORAGE_ITEM, Material.class,
        v -> MATERIAL_STORAGE_ITEM = v,
        Config::loadMaterial); }

    /** Material used for the Quest Storage shortcut button in the player's inventory. */
    public static Material QUEST_STORAGE_ITEM = Material.WRITABLE_BOOK;
    static { register(
        "materials.quest_storage_item",
        QUEST_STORAGE_ITEM, Material.class,
        v -> QUEST_STORAGE_ITEM = v,
        Config::loadMaterial); }
}
