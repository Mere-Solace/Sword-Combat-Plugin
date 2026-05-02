package btm.sword.action.skill.history;

/**
 * The action that produced an entry in the player's ability history log.
 */
public enum AbilityHistoryAction {
    /** The ability's physical item was consumed on use. */
    USED,
    /** The ability was surrendered as a quest turn-in. */
    SURRENDERED
}
