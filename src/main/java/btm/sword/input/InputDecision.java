package btm.sword.input;

/**
 * The router's response to an {@link InputIntent}: a single boolean instructing the
 * listener whether to cancel the originating Bukkit event.
 *
 * <p>Use the {@link #PASS} and {@link #CANCEL} constants in handlers so call sites read
 * declaratively rather than constructing booleans inline.</p>
 */
public record InputDecision(boolean cancelEvent) {
    /** Allow the originating Bukkit event to propagate normally. */
    public static final InputDecision PASS = new InputDecision(false);

    /** Cancel the originating Bukkit event after the router returns. */
    public static final InputDecision CANCEL = new InputDecision(true);
}
