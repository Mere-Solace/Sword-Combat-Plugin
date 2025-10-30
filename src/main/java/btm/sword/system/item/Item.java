package btm.sword.system.item;

import btm.sword.Sword;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

@Getter
public class Item {
	private final ItemStack itemStack;
	private final UUID uuid;
	
	public Item(ItemStack itemStack) {
		this.itemStack = itemStack;
		this.uuid = UUID.randomUUID();
		itemStack.getItemMeta().getPersistentDataContainer()
				.set(new NamespacedKey(Sword.getInstance(), "uuid"),
					PersistentDataType.STRING, getUUIDString());
	}

	public UUID getUniqueId() {
		return uuid;
	}
	
	public String getUUIDString() {
		return uuid.toString();
	}
}
