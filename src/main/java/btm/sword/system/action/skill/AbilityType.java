package btm.sword.system.action.skill;

/**
 * Classifies the physical and interaction behavior of an ability.
 *
 * <ul>
 *   <li>{@link #ACTIVATABLE} — Press to activate; has a cooldown; never consumed.</li>
 *   <li>{@link #THROWABLE} — Aimed-throw on hold; the physical item is consumed on each throw.</li>
 *   <li>{@link #CONSUMABLE} — One-shot activation; the physical item is consumed on use.</li>
 *   <li>{@link #PASSIVE} — Always active while the slot is equipped; no activation required.</li>
 *   <li>{@link #QUEST} — Fully usable ability <em>and</em> surrenderable as a quest turn-in.</li>
 * </ul>
 */
public enum AbilityType {
    ACTIVATABLE,
    THROWABLE,
    CONSUMABLE,
    PASSIVE,
    QUEST
}
