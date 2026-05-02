package btm.sword.input;

import java.util.ArrayList;
import java.util.List;

import btm.sword.input.ItemInputBinding.MatchContext;
import btm.sword.input.ItemInputBinding.Phase;

/**
 * Static, ordered registry of {@link ItemInputBinding}s.
 *
 * <p>The single source of truth for "what does this item do when the player presses
 * input X". The router consults the table in {@link Phase#EARLY} before trie traversal;
 * {@link btm.sword.entity.player.SwordPlayer#act(InputType) SwordPlayer.act} consults it
 * in {@link Phase#AT_ROOT} only while the trie is at root.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>{@link #register(ItemInputBinding)} appends a binding. Idempotent in the sense
 *       that registering the same binding instance twice is a no-op (id-based).</li>
 *   <li>{@link #clear()} empties the table. Called from
 *       {@link btm.sword.Sword#onDisable()} so a {@code /reload} produces a clean state.</li>
 *   <li>{@link #dispatch(MatchContext, Phase)} performs ordered first-match dispatch and
 *       returns whether the input was consumed.</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>The dispatch list is iterated in registration order — first match wins.</li>
 *   <li>Empty or {@code null} item stacks short-circuit to {@code false} without invoking
 *       any binding's {@link ItemInputBinding#matches(MatchContext) matches}.</li>
 *   <li>A binding never sees a phase other than its own.</li>
 *   <li>The table is owned by the input layer; only {@link InputBindingsRegistrar} (the
 *       canonical registration point) and the {@link btm.sword.Sword} bootstrap mutate it.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * All mutations and dispatches occur on the Bukkit main thread. The internal list is a
 * plain {@link ArrayList}; do not call {@link #register(ItemInputBinding)} from any other
 * thread.
 */
public final class ItemInputDispatchTable {

    private ItemInputDispatchTable() {}

    private static final List<ItemInputBinding> BINDINGS = new ArrayList<>();

    /**
     * Appends a binding to the registry. Re-registering the same {@link ItemInputBinding#id() id}
     * replaces the existing binding (last-write-wins by id, preserving its position).
     */
    public static void register(ItemInputBinding binding) {
        for (int i = 0; i < BINDINGS.size(); i++) {
            if (BINDINGS.get(i).id().equals(binding.id())) {
                BINDINGS.set(i, binding);
                return;
            }
        }
        BINDINGS.add(binding);
    }

    /** Removes all registered bindings. Safe to call from any state. */
    public static void clear() {
        BINDINGS.clear();
    }

    /** Number of registered bindings. */
    public static int size() {
        return BINDINGS.size();
    }

    /**
     * Iterates bindings in registration order; for each binding whose {@link ItemInputBinding#phase() phase}
     * matches and whose {@link ItemInputBinding#matches(MatchContext) matches} returns {@code true},
     * invokes {@link ItemInputBinding#dispatch(MatchContext) dispatch} and returns its result.
     * Returns {@code false} if no binding matched or the item was empty.
     *
     * @param ctx dispatch context
     * @param phase the phase being processed
     * @return {@code true} if a binding consumed the input
     */
    public static boolean dispatch(MatchContext ctx, Phase phase) {
        if (ctx.item() == null || ctx.item().isEmpty()) return false;
        for (ItemInputBinding binding : BINDINGS) {
            if (binding.phase() != phase) continue;
            if (!binding.matches(ctx)) continue;
            return binding.dispatch(ctx);
        }
        return false;
    }
}
