package btm.sword.action.skill.container;

import btm.sword.action.skill.SkillType;

/**
 * All equip slots available to a player for their skill loadout.
 *
 * <p>Slots are grouped by {@link SkillType}: three {@code UMBRAL} slots (triggered by blade
 * input combos), two {@code ACTIVE} slots (triggered by hotbar item usage), and four
 * {@code PASSIVE} slots (one core, three standard).</p>
 */
public enum SkillSlot {

    UMBRAL_1(SkillType.UMBRAL,   "Umbral I — Swap · L · L"),
    UMBRAL_2(SkillType.UMBRAL,   "Umbral II — Swap · L · R"),
    UMBRAL_3(SkillType.UMBRAL,   "Umbral III — Swap · L · Swap"),

    ACTIVE_1(SkillType.ACTIVE,   "Active I — Slot 1"),
    ACTIVE_2(SkillType.ACTIVE,   "Active II — Slot 2"),

    PASSIVE_CORE(SkillType.PASSIVE, "Core Passive"),

    PASSIVE_1(SkillType.PASSIVE, "Passive I"),
    PASSIVE_2(SkillType.PASSIVE, "Passive II"),
    PASSIVE_3(SkillType.PASSIVE, "Passive III");

    private final SkillType type;
    private final String title;

    SkillSlot(SkillType type, String title) {
        this.type = type;
        this.title = title;
    }

    /** Returns the {@link SkillType} category this slot belongs to. */
    public SkillType type() {
        return type;
    }

    /** Menu title for this slot, including its input binding where applicable. */
    public String title() {
        return title;
    }
}
