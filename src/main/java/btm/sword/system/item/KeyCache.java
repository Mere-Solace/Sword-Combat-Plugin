package btm.sword.system.item;

import btm.sword.Sword;
import org.bukkit.NamespacedKey;

import java.util.UUID;

public class KeyCache {
	public static final String menuStr = "menu_item_tag";
	public static final NamespacedKey menu = new NamespacedKey(Sword.getInstance(), menuStr);
	public static final String menuUUID = UUID.randomUUID().toString();
	
	public static final String displayIdStr = "display_id";
	public static final NamespacedKey displayIdKey = new NamespacedKey(Sword.getInstance(), displayIdStr);
	
	public static final String buttonIdStr = "dev_button_id";
	public static final NamespacedKey buttonIdKey = new NamespacedKey(Sword.getInstance(), buttonIdStr);
}
