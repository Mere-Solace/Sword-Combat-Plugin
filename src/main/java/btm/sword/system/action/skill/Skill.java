package btm.sword.system.action.skill;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;

/**
 * Always register new skill implementations in {@link SkillRegistry}
 */
public interface Skill {

    SkillId id();          // Stable identifier
    SkillType type();

    ItemStack icon();
    ComponentWrapper name();
    List<ComponentWrapper> description();
}
