package btm.sword.action.skill;

/**
 * Describes how an ability's uses are consumed when activated.
 *
 * <ul>
 *   <li>{@link #STACK} — each use decrements the item's stack count; depleted at zero.</li>
 *   <li>{@link #DURABILITY} — each use damages the item's durability bar; depleted when broken.</li>
 *   <li>{@link #COOLDOWN} — the item is never consumed; activation only triggers a cooldown.</li>
 * </ul>
 */
public enum AbilityUseType {

    /** Item stack count decrements each use; depleted at zero. */
    STACK,

    /** Single item whose durability bar decreases; depleted when broken. */
    DURABILITY,

    /** Never consumed — activation only triggers a cooldown. */
    COOLDOWN
}
