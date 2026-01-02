package btm.sword.system.action.skill.container;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.action.skill.SkillType;

public final class SkillSlotRules {
    public static boolean canEquip(Skill skill, SkillSlot slot) {
        return switch (slot.type()) {
            case UMBRAL -> skill.type() == SkillType.UMBRAL;
            case ACTIVE -> skill.type() == SkillType.ACTIVE;
            case PASSIVE -> skill.type() == SkillType.PASSIVE;
        };
    }
}
