package btm.sword.input.binding;

import java.util.function.Predicate;

import org.bukkit.inventory.ItemStack;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.input.InputType;
import btm.sword.input.intent.InputIntent;
import btm.sword.input.intent.InputRouter;

/**
 * Registers a per-item input intercept into the {@link ItemInputDispatchTable}.
 *
 * <p>A binding declares <em>when</em> it runs ({@link Phase}), <em>what items</em> it
 * applies to ({@link #matches(MatchContext)}), and <em>what to do</em> when it does
 * ({@link #dispatch(MatchContext)}). The dispatch table iterates registered bindings in
 * insertion order and fires the first match for the requested phase.</p>
 *
 * <h2>Two phases</h2>
 * <ul>
 *   <li>{@link Phase#EARLY} — invoked by {@link InputRouter} immediately after Bukkit-event
 *       intent construction, before any combo trie traversal. Used for items that fully
 *       suppress normal input routing (menu buttons, storage pouches, BLOCKED items).</li>
 *   <li>{@link Phase#AT_ROOT} — invoked by {@link SwordPlayer#act(InputType)} only while
 *       the trie is at root. Used for items that intercept specific inputs without
 *       suppressing the trie globally (ability hotbar slots).</li>
 * </ul>
 *
 * <h2>Semantics</h2>
 * <ul>
 *   <li>{@link #dispatch(MatchContext)} returns {@code true} to indicate the binding
 *       <em>consumed</em> the input. The caller must skip downstream processing for that
 *       input. Whether the source Bukkit event is cancelled is the caller's policy, not
 *       the binding's — the router maps consumed bindings to its own intent-specific
 *       cancel rules.</li>
 *   <li>Bindings must not throw checked exceptions. Runtime exceptions propagate out of
 *       the table and abort the current input.</li>
 *   <li>Bindings should be stateless or own their state explicitly. Binding instances are
 *       shared across all players.</li>
 * </ul>
 *
 * <h2>Adding a new binding</h2>
 * <ol>
 *   <li>Implement {@code ItemInputBinding} in your domain package.</li>
 *   <li>Register it from
 *       {@link btm.sword.input.binding.InputBindingsRegistrar#registerAll()} (or your own
 *       enable hook that calls {@link ItemInputDispatchTable#register(ItemInputBinding)}).</li>
 *   <li>Verify ordering: the first binding registered is the first checked. Order
 *       matters when multiple bindings could match the same item.</li>
 * </ol>
 */
public interface ItemInputBinding {

    /** Stable identifier for diagnostics, log lines, and registry de-duplication. */
    String id();

    /** Selects the dispatch phase at which this binding is invoked. */
    Phase phase();

    /** Returns {@code true} if this binding applies to the given context. */
    boolean matches(MatchContext ctx);

    /**
     * Performs the binding's effect.
     *
     * @return {@code true} if the input was consumed and downstream processing should
     *         be skipped; {@code false} to fall through to the next binding (or to the
     *         trie / vanilla handling if no further binding matches).
     */
    boolean dispatch(MatchContext ctx);

    /** When in the input pipeline a binding fires. */
    enum Phase {
        /** Before trie traversal, invoked from {@link InputRouter}. */
        EARLY,
        /** Inside {@link SwordPlayer#act(InputType)} while the trie is at root. */
        AT_ROOT
    }

    /**
     * Immutable per-call dispatch context.
     *
     * @param player the player who produced the input
     * @param input the discrete input being routed
     * @param item the relevant item stack — main-hand for most intents; the dropped
     *             stack for {@link InputIntent.Drop}
     */
    record MatchContext(SwordPlayer player, InputType input, ItemStack item) {}

    /**
     * Inline factory for trivial bindings — pairs a match predicate with a dispatch
     * predicate and wraps them in an anonymous {@link ItemInputBinding}.
     *
     * <p>Use this when the binding fits in a few lines of lambda. Bindings with
     * non-trivial logic, helper methods, or per-binding state should be authored as a
     * full class (e.g. {@code AbilitySlotBinding}).</p>
     *
     * @param id stable identifier; reusing an id replaces the prior binding
     * @param phase dispatch phase
     * @param matches predicate that selects which inputs the binding applies to
     * @param dispatch predicate that performs the binding's effect and returns
     *                 {@code true} if the input was consumed
     * @return an immutable binding ready to register
     */
    static ItemInputBinding of(String id,
                                Phase phase,
                                Predicate<MatchContext> matches,
                                Predicate<MatchContext> dispatch) {
        return new ItemInputBinding() {
            @Override public String id() { return id; }
            @Override public Phase phase() { return phase; }
            @Override public boolean matches(MatchContext ctx) { return matches.test(ctx); }
            @Override public boolean dispatch(MatchContext ctx) { return dispatch.test(ctx); }
            @Override public String toString() { return "ItemInputBinding[" + id + "/" + phase + "]"; }
        };
    }
}
