package btm.sword.action.skill.type;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.action.skill.Skill;
import btm.sword.item.core.ItemStackBuilder;
import net.kyori.adventure.text.Component;

/**
 * Base class for passive skills that are always active while equipped.
 *
 * <p>Provides a default {@link #icon()} placeholder. Subclasses should override
 * {@code icon()} with an appropriate item and implement any passive effect hooks.
 * For passive skills that also exist as physical world items, extend
 * {@link PassiveAbilitySkill} instead.</p>
 */
public abstract class PassiveSkill implements Skill {
    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.AMETHYST_SHARD)
            .name(Component.text("Yet-to-be-implemented Active Skill"))
            .hideAll()
            .build();
    }
}
