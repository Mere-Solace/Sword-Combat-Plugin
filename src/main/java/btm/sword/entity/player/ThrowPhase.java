package btm.sword.entity.player;

/**
 * Encodes the mutually exclusive phases of a {@link Combatant}'s throw lifecycle.
 *
 * <p>Replaces the three boolean fields ({@code attemptingThrow}, {@code throwCancelled},
 * {@code throwSuccessful}) that previously allowed illegal combinations.</p>
 */
public enum ThrowPhase {
    /** No throw is in progress. */
    IDLE,
    /** A throw has been initiated but not yet released or cancelled. */
    THROWING,
    /** The throw was interrupted before the item left the hand. */
    CANCELLED,
    /** The item was successfully released. */
    SUCCESS
}
