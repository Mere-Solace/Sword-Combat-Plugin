package btm.sword.system.item;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.item.armor.ArmorType;
import btm.sword.system.item.material.MaterialType;
import btm.sword.system.item.quest.QuestItemType;
import btm.sword.system.item.weapon.WeaponType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Enumeration of item library categories shown in the {@link btm.sword.system.inventory.menu.ItemLibraryMenu}.
 *
 * <p>Each constant groups a family of game items under a display name and icon, and provides
 * a supplier that builds the current list of {@link ItemStack}s for that category.</p>
 */
public enum LibraryCategory {

    /** All registered armor types. */
    ARMOR("Armor", Material.IRON_CHESTPLATE,
        () -> Arrays.stream(ArmorType.values()).map(ArmorType::buildItemStack).collect(Collectors.toList())),

    /** All registered weapon types. */
    WEAPONS("Weapons", Material.STONE_SWORD,
        () -> Arrays.stream(WeaponType.values()).map(WeaponType::buildItemStack).collect(Collectors.toList())),

    /** All registered crafting materials. */
    MATERIALS("Materials", MaterialType.METAL_SCRAP.icon(),
        () -> Arrays.stream(MaterialType.values()).map(t -> t.buildItemStack(1)).collect(Collectors.toList())),

    /** All registered quest items. */
    QUEST("Quest Items", Material.WRITABLE_BOOK,
        () -> Arrays.stream(QuestItemType.values()).map(QuestItemType::buildItemStack).collect(Collectors.toList()));

    private final String displayName;
    private final Material icon;
    private final Supplier<List<ItemStack>> itemsSupplier;

    LibraryCategory(String displayName, Material icon, Supplier<List<ItemStack>> itemsSupplier) {
        this.displayName = displayName;
        this.icon = icon;
        this.itemsSupplier = itemsSupplier;
    }

    /** Returns the human-readable display name for this category. */
    public String displayName() {
        return displayName;
    }

    /** Returns the icon {@link Material} used to represent this category in menus. */
    public Material icon() {
        return icon;
    }

    /**
     * Returns all items belonging to this category by invoking the items supplier.
     *
     * @return a list of built {@link ItemStack}s for this category
     */
    public List<ItemStack> getItems() {
        return itemsSupplier.get();
    }

    /**
     * Returns a styled {@link Component} display name for use in menu headers and buttons.
     *
     * @return the display name component
     */
    public Component displayNameComponent() {
        return Component.text(displayName, Config.SwordColor.TEXT_COOL, TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);
    }
}
