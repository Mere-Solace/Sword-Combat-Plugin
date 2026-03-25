package btm.sword.system.action.skill.history;

import btm.sword.system.action.skill.SkillId;

/**
 * An immutable record of a single ability event in a player's history.
 *
 * @param id          the ability's stable {@link SkillId}
 * @param displayName the human-readable name captured at the time of the event
 * @param action      whether the ability was {@link AbilityHistoryAction#USED} or
 *                    {@link AbilityHistoryAction#SURRENDERED}
 * @param timestamp   epoch-millis when the event occurred
 */
public record AbilityHistoryEntry(
    SkillId id,
    String displayName,
    AbilityHistoryAction action,
    long timestamp
) {}
