package btm.sword.system.input;

/**
 * Defines the context in which a player's {@link InputExecutionTree} operates.
 * <p>
 * Contexts allow the same physical input tree to expose or suppress different action paths
 * depending on the player's current gameplay state. Nodes use
 * predicates that check the player's active context via
 * {@link btm.sword.system.entity.impl.SwordPlayer#getActivationContext()}.
 * </p>
 *
 * <p>Example — suppressing movement during stun:</p>
 * <pre>{@code
 * new InputNodeBuilder(root, List.of(InputKey.of(InputType.SWAP), InputKey.of(InputType.SWAP)))
 *     .action(() -> InputAction.builder()...build())
 *     .visibleIf(p -> p.getActivationContext() == ActivationContext.NORMAL)
 *     .build();
 * }</pre>
 */
public enum ActivationContext {

    /**
     * Default state — all inputs are accessible based on their own conditions.
     * Most nodes operate in this context unless restricted by a {@code visibleIf} predicate.
     */
    NORMAL,

    /**
     * Player is stunned (grabbed, incapacitated, or otherwise suppressed).
     * Nodes restricted to {@link #NORMAL} are hidden; a limited escape subset may be shown.
     */
    INCAPACITATED,

    THROWING,

    /**
     * Player is channeling a healing ability. Most inputs are suppressed;
     * taking damage sets an interrupt flag to cancel the channel.
     */
    CHANNELING
}
