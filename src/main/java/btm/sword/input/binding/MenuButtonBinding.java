package btm.sword.input.binding;

import btm.sword.input.InputType;
import btm.sword.input.ItemInputBinding;
import btm.sword.item.core.KeyRegistry;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.MainMenu;

/**
 * EARLY-phase binding that opens the {@link MainMenu} when the player interacts with the
 * Sword main-menu button. SHIFT is consumed without opening a menu so a sneaking player
 * can't accidentally trigger the menu while still suppressing the input from reaching
 * the trie.
 *
 * <p>Replaces the {@code MAIN_MENU_BUTTON_KEY} branch of the legacy
 * {@code SwordPlayer.handleItemInteraction}.</p>
 */
public final class MenuButtonBinding implements ItemInputBinding {

    @Override
    public String id() {
        return "menu_button";
    }

    @Override
    public Phase phase() {
        return Phase.EARLY;
    }

    @Override
    public boolean matches(MatchContext ctx) {
        return KeyRegistry.hasKey(ctx.item(), KeyRegistry.MAIN_MENU_BUTTON_KEY);
    }

    @Override
    public boolean dispatch(MatchContext ctx) {
        if (ctx.input() != InputType.SHIFT) {
            InventoryMenuManager.openMenu(MainMenu.class, ctx.player());
        }
        return true;
    }
}
