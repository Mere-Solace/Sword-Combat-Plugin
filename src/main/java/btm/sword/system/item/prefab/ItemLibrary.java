package btm.sword.system.item.prefab;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.SwordItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ItemLibrary {
    public static ItemStack sword = new ItemStackBuilder(Material.STONE_SWORD)
        .hideAll()
        .name(Component.text("Falchion", TextColor.color(254,56,0), TextDecoration.BOLD))
        .lore(List.of(
            Component.text().content("Veracity").color(TextColor.color(160,160,160)).build(),
            Component.text().content("&").color(TextColor.color(89,89,89)).build(),
            Component.text().content("Assiduity").color(TextColor.color(160,160,160)).build()))
        .durability(3)
        .tagSwordItem(SwordItemType.BROAD_SWORD)
        .tagAttackStyle(WeaponAttackStyle.SLASH)
        .toughnessDamageAdder(5)
        .build();

    public static ItemStack gun = new ItemStackBuilder(Material.IRON_SHOVEL)
        .hideAll()
        .name(Component.text("Intrusion Ender", TextColor.color(0,174,200), TextDecoration.BOLD))
        .lore(List.of(
            Component.text().content("Veracity").color(TextColor.color(160,160,160)).build(),
            Component.text().content("&").color(TextColor.color(89,89,89)).build(),
            Component.text().content("Assiduity").color(TextColor.color(160,160,160)).build()))
        .durability(3)
        .tagSwordItem(SwordItemType.PISTOL)
        .toughnessDamageAdder(7)
        .build();
}
