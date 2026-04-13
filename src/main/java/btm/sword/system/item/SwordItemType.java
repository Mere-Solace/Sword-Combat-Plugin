package btm.sword.system.item;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Enumerates all custom item types recognised by the Sword plugin, keyed by a persistent data string. */
public enum SwordItemType {
    GENERIC("generic"),

    UMBRAL_LINK("umbral_link"),
    UMBRAL_BLADE("umbral_blade"),

    SHORT_SWORD("short_sword"),
    BROAD_SWORD("broad_sword"),
    ZWEIHANDER("zweihander"),

    PISTOL("pistol"),

    ACTIVE_1("active_1"),
    ACTIVE_2("active_2");

    private final String string;


    private static final HashMap<String, SwordItemType> MAP_TO_ENUM = new HashMap<>();

    static {
        for (SwordItemType type : values()) {
            MAP_TO_ENUM.put(type.toString().toLowerCase(), type);
        }
    }

    SwordItemType(String string) {
        this.string = string;
    }

    /** Returns the lowercase string key used to identify this type in persistent data. */
    public String string() {
        return string;
    }

    /** Resolves the item type from the item's persistent data, defaulting to {@link #GENERIC} if not found. */
    public static SwordItemType fromString(ItemStack item) {
        return MAP_TO_ENUM.getOrDefault(KeyRegistry.getKeyField(item, KeyRegistry.ITEM_TYPE_KEY, PersistentDataType.STRING), GENERIC);
    }
}
