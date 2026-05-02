package btm.sword.item.core;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import btm.sword.action.skill.SkillId;

/**
 * Reads the ability identity tag from a physical ability world item.
 *
 * @see AbilityItemBuilder
 */
public final class AbilityItemReader {

    private AbilityItemReader() {}

    /**
     * Returns the {@link SkillId} stored in the item's {@link KeyRegistry#ABILITY_ID_KEY}
     * PDC tag, or {@code null} if the item is not an ability item or the tag is malformed.
     *
     * @param item the item to inspect (may be {@code null} or empty)
     * @return the ability's {@link SkillId}, or {@code null}
     */
    @Nullable
    public static SkillId readId(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String raw = KeyRegistry.getKeyField(item, KeyRegistry.ABILITY_ID_KEY, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return SkillId.parse(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the item has a valid {@link KeyRegistry#ABILITY_ID_KEY} tag.
     *
     * @param item the item to check
     * @return {@code true} if this is an ability world item
     */
    public static boolean isAbilityItem(ItemStack item) {
        return readId(item) != null;
    }
}
