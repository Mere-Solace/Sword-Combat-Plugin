package btm.sword.menu.button;

import org.bukkit.Material;

import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

/** A paged GUI control item that returns to the previous page. */
public class PreviousItem extends PageItem {

    /** Constructs a backward page-navigation item. */
    public PreviousItem() {
        super(false);
    }

    @Override
    public ItemProvider getItemProvider(PagedGui<?> gui) {
        ItemBuilder builder = new ItemBuilder(Material.SPRUCE_TRAPDOOR);
        builder.setDisplayName("Previous page")
            .addLoreLines(gui.hasPreviousPage()
                ? "Go to page " + gui.getCurrentPage() + "/" + gui.getPageAmount()
                : "You can't go further back");

        return builder;
    }
}
