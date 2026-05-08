package btm.sword.join;

/**
 * Closed lifecycle of a single {@link JoinSession}.
 *
 * <h2>Permitted transitions</h2>
 * <ul>
 *   <li>{@link #STAGING} → {@link #WAITING} — after the configurable load-wait elapses,
 *       the gate engages and the router menu opens.</li>
 *   <li>{@link #WAITING} → {@link #ROUTING} — the player clicks a destination button.</li>
 *   <li>{@link #ROUTING} → {@link #WAITING} — the player presses sneak to abort the
 *       countdown.</li>
 *   <li>{@link #ROUTING} → {@link #ACTIVE} — the routing countdown completes and the
 *       player is teleported to their chosen destination.</li>
 * </ul>
 *
 * <p>Any other transition attempt throws {@link IllegalStateException}. {@link #ACTIVE}
 * is terminal — once a session reaches it the session is evicted from the arbiter map
 * and no further transitions can be requested.</p>
 *
 * <p>Termination (player quit) is modelled separately on the {@link JoinSession} via a
 * {@code terminated} latch; this enum represents only the in-progress lifecycle.</p>
 */
public enum JoinPhase {

    /**
     * The player has just been placed in their dark-room slot. The waiting-phase gate
     * has not yet been engaged; no per-session tasks are running. This phase exists for
     * the brief window between {@code PlayerSpawnLocationEvent} and the deferred
     * {@link JoinSession#enterWaiting()} call so the client has time to receive the
     * spawn-location packet and load chunks.
     */
    STAGING,

    /**
     * The player is sitting in the dark-room slot with placeholder panes in their
     * inventory and the router menu open. The waiting ticker is zeroing their velocity
     * each tick and re-opening the menu if it gets closed. The structural input gate is
     * engaged via {@link btm.sword.input.trie.ActivationContext#WAITING}.
     */
    WAITING,

    /**
     * The player has selected a destination and the configurable countdown is running.
     * The router menu is closed; the waiting ticker continues zeroing velocity but no
     * longer re-opens the menu. The player can sneak to abort and return to
     * {@link #WAITING}.
     */
    ROUTING,

    /**
     * The countdown completed and the player has been teleported to their destination,
     * the gate has been disengaged, and the session has been evicted from the arbiter
     * map. Terminal — no further transitions are valid.
     */
    ACTIVE
}
