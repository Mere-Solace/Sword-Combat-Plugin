package btm.sword.action.skill;

/**
 * Broad category of a {@link Skill}, determining which {@link btm.sword.action.skill.container.SkillSlot}
 * types it can occupy.
 *
 * <ul>
 *   <li>{@link #UMBRAL} — blade-input combos; equips to {@code UMBRAL_1/2/3} slots.</li>
 *   <li>{@link #ACTIVE} — player-triggered abilities; equips to {@code ACTIVE_1/2} slots.</li>
 *   <li>{@link #PASSIVE} — always-on modifiers; equips to {@code PASSIVE_*} slots.</li>
 * </ul>
 */
public enum SkillType {
    UMBRAL,
    ACTIVE,
    PASSIVE
}
