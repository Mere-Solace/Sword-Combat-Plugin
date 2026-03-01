package btm.sword.system.item;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

    public String string() {
        return string;
    }

    public static SwordItemType fromString(ItemStack item) {
        return MAP_TO_ENUM.getOrDefault(KeyRegistry.getKeyField(item, KeyRegistry.ITEM_TYPE_KEY, PersistentDataType.STRING), GENERIC);
    }
}
