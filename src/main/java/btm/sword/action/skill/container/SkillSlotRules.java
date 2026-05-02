package btm.sword.action.skill.container;

import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillType;

/**
 * Validation rules for equipping a {@link Skill} into a {@link SkillSlot}.
 *
 * <p>Each slot type accepts only skills of the matching {@link SkillType}: umbral slots take
 * umbral skills, active slots take active skills, passive slots take passive skills.</p>
 */
public final class SkillSlotRules {

    private SkillSlotRules() {}

    /**
     * Returns {@code true} if the given skill's type matches the slot's accepted type.
     *
     * @param skill the skill to validate
     * @param slot  the target slot
     * @return {@code true} if the skill can be equipped in the slot
     */
    public static boolean canEquip(Skill skill, SkillSlot slot) {
        return switch (slot.type()) {
            case UMBRAL -> skill.type() == SkillType.UMBRAL;
            case ACTIVE -> skill.type() == SkillType.ACTIVE;
            case PASSIVE -> skill.type() == SkillType.PASSIVE;
        };
    }
}
