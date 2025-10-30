package btm.sword.system.item.prefab;

import btm.sword.system.item.ItemStackBuilder;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Prefab {
    public static ItemStack sword = new ItemStackBuilder(Material.DIAMOND_SWORD)
            .name(Component.text("Masamune", TextColor.color(254,56,0), TextDecoration.BOLD))
            .lore(List.of(
                    Component.text().content("Veracity").color(TextColor.color(160,160,160)).build(),
                    Component.text().content("&").color(TextColor.color(89,89,89)).build(),
                    Component.text().content("Assiduity").color(TextColor.color(160,160,160)).build()))
            .unbreakable(true)
            .durability(3)
            .tag("weapon", "long_sword")
            .baseDamage(35)
            .build();

    public static ItemStack gun = new ItemStackBuilder(Material.IRON_SHOVEL)
            .name(Component.text("Gunblade", TextColor.color(0,174,200), TextDecoration.BOLD))
            .lore(List.of(
                    Component.text().content("Veracity").color(TextColor.color(160,160,160)).build(),
                    Component.text().content("&").color(TextColor.color(89,89,89)).build(),
                    Component.text().content("Assiduity").color(TextColor.color(160,160,160)).build()))
            .unbreakable(true)
            .durability(3)
            .tag("weapon", "gun")
            .baseDamage(35)
            .hideAll()
            .build();
}
