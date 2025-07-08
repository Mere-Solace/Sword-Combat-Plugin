package btm.sword.system.item;

import btm.sword.Sword;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ItemBuilder {
	private final ItemStack item;
	private final ItemMeta meta;
	private final Plugin plugin;
	
    public ItemBuilder(Material material) {
		this.item = new ItemStack(material);
		this.meta = item.getItemMeta();
		this.plugin = Sword.getInstance();
    }
	
	public ItemBuilder name(String name, TextColor color, TextDecoration style) {
		meta.itemName(Component.text(name).color(color).decoration(style, true));
		return this;
	}
	
	public ItemBuilder lore(List<Component> lore) {
		meta.lore(lore);
		return this;
	}
	
	public ItemBuilder unbreakable(boolean unbreakable) {
		meta.setUnbreakable(unbreakable);
		return this;
	}
	
	public ItemBuilder durability(int damage) {
		if (meta instanceof Damageable tool)
			tool.setDamage(damage);
		return this;
	}
	
	public ItemBuilder tag(String key, String value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, key),
				PersistentDataType.STRING,
				value
		);
		return this;
	}
	
	public ItemBuilder weaponTag(String key, String value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, key),
				PersistentDataType.STRING,
				value
		);
		return this;
	}
	
	public ItemBuilder baseDamage(double value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, "damage"),
				PersistentDataType.DOUBLE,
				value
		);
		return this;
	}
	
	public ItemBuilder skinData(int identifier) {
		meta.setCustomModelData(identifier);
		return this;
	}
	
	public ItemBuilder hideAll() {
		for (ItemFlag flag : ItemFlag.values())
			meta.addItemFlags(flag);
		return this;
	}
	
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
}
