package btm.sword.system.action.skill;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

/**
 * Always register new skill implementations in {@link SkillRegistry}
 */
public interface Skill {

    SkillId id();          // Stable identifier
    SkillType type();

    ItemStack icon();
    Component name();
    List<Component> description();
}
