package btm.sword.input.binding;

import java.util.function.Predicate;

import btm.sword.Sword;
import btm.sword.input.InputType;
import btm.sword.input.binding.ItemInputBinding.Phase;
import btm.sword.item.core.KeyRegistry;
import btm.sword.item.core.StorageCategory;
import btm.sword.menu.ArtifactPouchMenu;
import btm.sword.menu.CurrencyMenu;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.MainMenu;
import btm.sword.menu.MaterialPouchMenu;

/**
 * Single registration point for all built-in {@link ItemInputBinding}s.
 *
 * <p>Called once from {@link Sword#onEnable()} after the plugin's core systems
 * are initialised. Idempotent — repeat invocations clear the table and reinstall the
 * canonical set, so a {@code /reload} produces a clean state.</p>
 *
 * <h2>Authoring style</h2>
 * <ul>
 *   <li>Trivial bindings (a few lines of dispatch logic, no shared state) are written
 *       inline using {@link ItemInputBinding#of(String, Phase, Predicate, Predicate)}.</li>
 *   <li>Bindings with multistep logic, helper methods, or that need to cast the player
 *       to a subclass are authored as full classes in this package
 *       (see {@link AbilitySlotBinding}, {@link UmbralBladeTesterBinding}).</li>
 * </ul>
 *
 * <h2>Registration order</h2>
 * Bindings are checked in registration order with first-match-wins semantics. Place more
 * specific bindings before general ones if their predicates could overlap. The current
 * set has no overlap, so order is purely conventional.
 *
 * <h2>Future evolution</h2>
 * Domains that own their own enable hook may register bindings directly via
 * {@link ItemInputDispatchTable#register(ItemInputBinding)}. This file is the canonical
 * starting point but is not required to be the only one.
 */
public final class InputBindingsRegistrar {

    private InputBindingsRegistrar() {}

    /** Clears the dispatch table and registers the canonical built-in bindings. */
    public static void registerAll() {
        ItemInputDispatchTable.clear();

        // Main menu button — open MainMenu unless SHIFT (still consumed to suppress trie).
        ItemInputDispatchTable.register(ItemInputBinding.of(
            "menu_button", Phase.EARLY,
            ctx -> KeyRegistry.hasKey(ctx.item(), KeyRegistry.MAIN_MENU_BUTTON_KEY),
            ctx -> {
                if (ctx.input() != InputType.SHIFT) {
                    InventoryMenuManager.openMenu(MainMenu.class, ctx.player());
                }
                return true;
            }
        ));

        // Storage pouches — open the matching menu and consume.
        ItemInputDispatchTable.register(ItemInputBinding.of(
            "storage_button", Phase.EARLY,
            ctx -> StorageCategory.fromItem(ctx.item()) != null,
            ctx -> {
                StorageCategory category = StorageCategory.fromItem(ctx.item());
                switch (category) {
                    case MATERIAL -> InventoryMenuManager.openMenu(MaterialPouchMenu.class, ctx.player());
                    case CURRENCY -> InventoryMenuManager.openMenu(CurrencyMenu.class, ctx.player());
                    case QUEST -> InventoryMenuManager.openMenu(ArtifactPouchMenu.class, ctx.player());
                    case null, default -> { }
                }
                return true;
            }
        ));

        // Dev-only UmbralBlade lifecycle tester (BREEZE_ROD).
        ItemInputDispatchTable.register(new UmbralBladeTesterBinding());

        // Ability hotbar slots — LEFT click fires the equipped active skill.
        ItemInputDispatchTable.register(new AbilitySlotBinding());
    }
}
