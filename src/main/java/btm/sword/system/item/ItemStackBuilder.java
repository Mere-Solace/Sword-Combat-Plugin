package btm.sword.system.item;

import btm.sword.Sword;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ItemStackBuilder {
	private final ItemStack item;
	private final ItemMeta meta;
	private final Plugin plugin;
	
    public ItemStackBuilder(Material material) {
		this.item = new ItemStack(material);
		this.meta = item.getItemMeta();
		this.plugin = Sword.getInstance();
    }
	
	public ItemStackBuilder name(Component component) {
		meta.itemName(component);
		return this;
	}
	
	public ItemStackBuilder lore(List<Component> lore) {
		meta.lore(lore);
		return this;
	}
	
	public ItemStackBuilder unbreakable(boolean unbreakable) {
		meta.setUnbreakable(unbreakable);
		return this;
	}
	
	public ItemStackBuilder durability(int damage) {
		if (meta instanceof Damageable tool)
			tool.setDamage(damage);
		return this;
	}
	
	public ItemStackBuilder tag(String key, String value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, key),
				PersistentDataType.STRING,
				value
		);
		return this;
	}
	
	public ItemStackBuilder weaponTag(String key, String value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, key),
				PersistentDataType.STRING,
				value
		);
		return this;
	}
	
	public ItemStackBuilder baseDamage(double value) {
		meta.getPersistentDataContainer().set(
				new NamespacedKey(plugin, "damage"),
				PersistentDataType.DOUBLE,
				value
		);
		return this;
	}
	
	public ItemStackBuilder skinData(int identifier) {
		meta.setCustomModelData(identifier);
		return this;
	}
	
	public ItemStackBuilder hideAll() {
		meta.addItemFlags(ItemFlag.values());
		return this;
	}
	
	public ItemStack build() {
		item.setItemMeta(meta);
		return item;
	}
}
