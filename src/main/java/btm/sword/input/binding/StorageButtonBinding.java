package btm.sword.input.binding;

import btm.sword.input.ItemInputBinding;
import btm.sword.item.core.StorageCategory;
import btm.sword.menu.ArtifactPouchMenu;
import btm.sword.menu.CurrencyMenu;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.MaterialPouchMenu;

/**
 * EARLY-phase binding that opens the corresponding pouch menu when the player interacts
 * with a storage button (currency, material, quest).
 *
 * <p>Replaces the storage-category branch of the legacy
 * {@code SwordPlayer.handleItemInteraction}.</p>
 */
public final class StorageButtonBinding implements ItemInputBinding {

    @Override
    public String id() {
        return "storage_button";
    }

    @Override
    public Phase phase() {
        return Phase.EARLY;
    }

    @Override
    public boolean matches(MatchContext ctx) {
        return StorageCategory.fromItem(ctx.item()) != null;
    }

    @Override
    public boolean dispatch(MatchContext ctx) {
        StorageCategory category = StorageCategory.fromItem(ctx.item());
        switch (category) {
            case MATERIAL -> InventoryMenuManager.openMenu(MaterialPouchMenu.class, ctx.player());
            case CURRENCY -> InventoryMenuManager.openMenu(CurrencyMenu.class, ctx.player());
            case QUEST -> InventoryMenuManager.openMenu(ArtifactPouchMenu.class, ctx.player());
            default -> { }
        }
        return true;
    }
}
