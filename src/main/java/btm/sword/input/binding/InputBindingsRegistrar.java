package btm.sword.input.binding;

import btm.sword.input.ItemInputDispatchTable;

/**
 * Single registration point for all built-in {@link btm.sword.input.ItemInputBinding}s.
 *
 * <p>Called once from {@link btm.sword.Sword#onEnable()} after the plugin's core systems
 * are initialised. Idempotent — repeat invocations clear the table and reinstall the
 * canonical set, so a {@code /reload} produces a clean state.</p>
 *
 * <h2>Registration order</h2>
 * Bindings are checked in registration order with first-match-wins semantics. Place more
 * specific bindings before general ones if their predicates could overlap. The current
 * set has no overlap (each binding matches a disjoint set of items), so order is purely
 * conventional.
 */
public final class InputBindingsRegistrar {

    private InputBindingsRegistrar() {}

    /** Clears the dispatch table and registers the canonical built-in bindings. */
    public static void registerAll() {
        ItemInputDispatchTable.clear();
        ItemInputDispatchTable.register(new MenuButtonBinding());
        ItemInputDispatchTable.register(new StorageButtonBinding());
        ItemInputDispatchTable.register(new AbilitySlotBinding());
    }
}
