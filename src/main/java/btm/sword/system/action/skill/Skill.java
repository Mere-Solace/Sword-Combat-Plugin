package btm.sword.system.action.skill;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;

/**
 * Common contract for all skills — passives, actives, and ability items.
 *
 * <p>Every implementation must be registered in {@link SkillRegistry} so it can be
 * looked up by {@link SkillId} at runtime. Use the appropriate base class:
 * {@link btm.sword.system.action.skill.type.PassiveSkill},
 * {@link btm.sword.system.action.skill.type.ActiveSkill}, or one of the {@code AbilitySkill}
 * subtypes for physical-item abilities.</p>
 */
public interface Skill {

    /**
     * Returns the stable identifier used to look this skill up in {@link SkillRegistry}.
     *
     * @return the skill's unique {@link SkillId}
     */
    SkillId id();

    /**
     * Returns the broad category of this skill.
     *
     * @return one of {@link SkillType#ACTIVE}, {@link SkillType#PASSIVE}, or {@link SkillType#UMBRAL}
     */
    SkillType type();

    /**
     * Returns the inventory icon shown in skill-selection menus.
     *
     * @return the icon {@link ItemStack}
     */
    ItemStack icon();

    /**
     * Returns the display name of this skill.
     *
     * @return the skill name {@link Component}
     */
    Component name();

    /**
     * Returns the description lines shown in skill-selection menu tooltips.
     *
     * @return an ordered list of description {@link Component}s
     */
    List<Component> description();
}
