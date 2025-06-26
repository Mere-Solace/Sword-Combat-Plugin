package btm.sword.system.item.prefab;

import btm.sword.system.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Prefab {
	public static ItemStack sword;
	public static ItemStack gun;
	
	static {
		sword = new ItemBuilder(Material.NETHERITE_SWORD)
				.name("Muramasa", TextColor.color(254,56,0), TextDecoration.BOLD)
				.lore(List.of(
						Component.text().content("yes").color(TextColor.color(89,89,89)).build(),
						Component.newline(),
						Component.newline(),
						Component.text().content("assiduity").color(TextColor.color(160,160,160)).build()))
				.unbreakable(true)
				.durability(3)
				.tag("weapon", "long_sword")
				.baseDamage(35)
				.build();
		
		gun = new ItemBuilder(Material.IRON_SHOVEL)
				.name("Gunblade", TextColor.color(0,174,200), TextDecoration.BOLD)
				.lore(List.of(
						Component.text().content("yes").color(TextColor.color(89,89,89)).build(),
						Component.newline(),
						Component.newline(),
						Component.text().content("assiduity").color(TextColor.color(160,160,160)).build()))
				.unbreakable(true)
				.durability(3)
				.tag("weapon", "gun")
				.baseDamage(35)
				.hideAll()
				.build();
	}
}
