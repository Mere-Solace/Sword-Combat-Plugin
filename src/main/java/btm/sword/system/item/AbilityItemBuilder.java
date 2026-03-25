package btm.sword.system.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.system.action.skill.SkillId;

/**
 * Utility for producing physical world items that represent abilities.
 *
 * <p>Every ability world item is tagged with the {@link KeyRegistry#ABILITY_ID_KEY} PDC key
 * so it can later be identified via {@link AbilityItemReader}. Implementations provide
 * the base {@link ItemStack} (visual appearance, name, lore) through the ability's own
 * {@link btm.sword.system.action.skill.type.AbilitySkill#buildWorldItem()} — this class
 * stamps the identity tag onto that stack.
 *
 * <h2>Usage</h2>
 * <pre>
 * // Inside an AbilitySkill implementation:
 * public ItemStack buildWorldItem() {
 *     ItemStack base = ItemStackBuilder.of(Material.ECHO_SHARD)
 *         .name(name())
 *         .lore(description())
 *         .build();
 *     return AbilityItemBuilder.tag(base, id());
 * }
 * </pre>
 */
public final class AbilityItemBuilder {

    private AbilityItemBuilder() {}

    /**
     * Tags the given {@link ItemStack} with the supplied {@link SkillId} as its ability identity.
     * The item is modified in-place and returned for chaining convenience.
     *
     * @param item the base item to stamp
     * @param id   the ability's stable identifier
     * @return the same item, now tagged with {@link KeyRegistry#ABILITY_ID_KEY}
     */
    public static ItemStack tag(ItemStack item, SkillId id) {
        KeyRegistry.setKeyField(item, KeyRegistry.ABILITY_ID_KEY, PersistentDataType.STRING, id.asString());
        return item;
    }
}
