package btm.sword.system.action.skill.container;

import btm.sword.system.action.skill.SkillType;

public enum SkillSlot {

    UMBRAL_1(SkillType.UMBRAL),
    UMBRAL_2(SkillType.UMBRAL),
    UMBRAL_3(SkillType.UMBRAL),

    ACTIVE_1(SkillType.ACTIVE),
    ACTIVE_2(SkillType.ACTIVE),

    PASSIVE_CORE(SkillType.PASSIVE),

    PASSIVE_1(SkillType.PASSIVE),
    PASSIVE_2(SkillType.PASSIVE),
    PASSIVE_3(SkillType.PASSIVE);

    private final SkillType type;

    SkillSlot(SkillType type) {
        this.type = type;
    }

    public SkillType type() {
        return type;
    }
}
