package btm.sword.system.item.quest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Enum defining all quest/artifact item types in the Sword item economy.
 * <p>
 * Quest items are uniquely-identified, lore-rich items that sit outside the normal
 * stackable material model. They may represent plot items, usable consumables,
 * unique weapons, or ability-granting artifacts depending on the type.
 * </p>
 *
 * <p>This enum is intentionally empty — quest item types will be added here
 * as content is developed. Each type should define a display name, icon material,
 * descriptive lore, and whether it is usable (e.g., triggers a skill or ability).</p>
 */
public enum QuestItemType {
    // Quest and artifact item types to be defined as content is developed.
    ;

    private final String id;

    QuestItemType(String id) {
        this.id = id;
    }

    /** @return the string id used to identify this quest item type. */
    public String id() {
        return id;
    }

    /**
     * Builds an {@link ItemStack} representing this quest item type.
     * Placeholder implementation — will be overridden per constant as content is added.
     *
     * @return an ItemStack for this quest item type
     */
    public ItemStack buildItemStack() {
        return ItemStack.of(Material.AIR);
    }

    /**
     * Returns the {@link QuestItemType} matching the given string id, or {@code null}.
     *
     * @param id the quest item type id string
     * @return the matching constant, or {@code null}
     */
    public static QuestItemType fromId(String id) {
        for (QuestItemType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }
}
