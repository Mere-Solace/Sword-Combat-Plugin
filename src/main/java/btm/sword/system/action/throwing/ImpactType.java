package btm.sword.system.action.throwing;

import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.item.KeyRegistry;

/**
 * Describes how a thrown item behaves on entity impact.
 * <p>
 * Tagged onto an {@link ItemStack} via {@link KeyRegistry#IMPACT_TYPE_KEY} and read by
 * {@link btm.sword.system.action.throwing.types.ThrownItem#onHit()} to choose between
 * impalement and knockback-only impact handling.
 * </p>
 */
public enum ImpactType {

    /** The item embeds in the target and starts an impalement task. */
    IMPALE("impale"),

    /** The item applies knockback only — no embedding. */
    KNOCKBACK("knockback");

    private final String string;

    private static final HashMap<String, ImpactType> MAP_TO_ENUM = new HashMap<>();

    static {
        for (ImpactType type : values()) {
            MAP_TO_ENUM.put(type.string, type);
        }
    }

    ImpactType(String string) {
        this.string = string;
    }

    /** @return the PDC string value for this type. */
    public String string() {
        return string;
    }

    /**
     * Reads the {@link ImpactType} tag from an {@link ItemStack}.
     * Defaults to {@link #KNOCKBACK} if untagged.
     *
     * @param item the item to inspect
     * @return the impact type, never {@code null}
     */
    public static ImpactType fromItem(ItemStack item) {
        return MAP_TO_ENUM.getOrDefault(
            KeyRegistry.getKeyField(item, KeyRegistry.IMPACT_TYPE_KEY, PersistentDataType.STRING),
            KNOCKBACK
        );
    }
}
