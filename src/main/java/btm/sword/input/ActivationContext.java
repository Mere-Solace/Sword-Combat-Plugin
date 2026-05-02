package btm.sword.input;

import java.util.function.Predicate;

import btm.sword.entity.player.SwordPlayer;

/**
 * Defines the context in which a player's {@link InputExecutionTree} operates.
 * <p>
 * Contexts allow the same physical input tree to expose or suppress different action paths
 * depending on the player's current gameplay state. Nodes use {@link InputExecutionTree.InputNodeBuilder#visibleIf}
 * predicates that check the player's active context via
 * {@link btm.sword.entity.player.SwordPlayer#getActivationContext()}.
 * </p>
 *
 * <p>Example — restricting a node to {@link #NORMAL} only:</p>
 * <pre>{@code
 * new InputNodeBuilder(root, List.of(InputType.SWAP, InputType.SWAP))
 *     .action(() -> InputAction.builder()...build())
 *     .visibleIf(ActivationContext.onlyIn(ActivationContext.NORMAL))
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
    CHANNELING,

    /**
     * Player is in a camera-driven cutscene or animation.
     * All combat and movement inputs are blocked; only scene-exit inputs are accepted.
     */
    CUTSCENE,

    /**
     * Player is in building/world-editing mode.
     * Only dashing (F→F, S→S) and the debug kill (D→D) remain accessible;
     * all combat, skill, and blade inputs are suppressed.
     */
    BUILDING;

    /**
     * Returns a {@link Predicate} that passes only when the player's current context
     * is one of the given {@code allowed} values. Use with
     * {@link InputExecutionTree.InputNodeBuilder#visibleIf} to gate tree nodes to
     * specific contexts.
     *
     * @param allowed the contexts in which the predicate returns {@code true}
     * @return a predicate over {@link SwordPlayer}
     */
    public static Predicate<SwordPlayer> onlyIn(ActivationContext... allowed) {
        return player -> {
            ActivationContext current = player.getActivationContext();
            for (ActivationContext ctx : allowed) {
                if (current == ctx) return true;
            }
            return false;
        };
    }
}
