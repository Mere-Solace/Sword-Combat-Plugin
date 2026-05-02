package btm.sword.entity.player;

/**
 * Encodes the mutually exclusive inventory interaction modes of a {@link SwordPlayer}.
 *
 * <p>Replaces the three boolean fields ({@code swappingInInv}, {@code droppingInInv},
 * {@code inInventorySession}) that previously allowed illegal combinations.</p>
 */
public enum InventoryMode {
    /** No inventory interaction is active. */
    NONE,
    /** The player is momentarily swapping items (clears after ~1 tick). */
    SWAPPING,
    /** The player is momentarily dropping items (clears after ~2 ticks). */
    DROPPING,
    /** The player has an inventory screen open. */
    SESSION
}
