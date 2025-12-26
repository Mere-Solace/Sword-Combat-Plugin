package btm.sword.system.inventory.menu;

import org.bukkit.Material;

import btm.sword.system.entity.types.SwordPlayer;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public abstract class Menu {
    protected final SwordPlayer swordPlayer;
    public Menu(SwordPlayer player) { this.swordPlayer = player; }
    public abstract void open();
    public void close() {}

    protected static final SimpleItem defaultSimpleItem = new SimpleItem(
        new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .setDisplayName("| | |")
            .clearItemFlags()
    );

    protected SimpleItem generatePreviousButtonOrDefault(SimpleItem defaultItem) {
        return swordPlayer.getPlayerMenuManager().noPreviousMenu() ?
            defaultItem :
            new SimpleItem(
                new ItemBuilder(Material.OAK_TRAPDOOR)
                    .clearItemFlags()
                    .setDisplayName("Go back"),
                click -> swordPlayer.getPlayerMenuManager().openPreviousMenu()
            );
    }

    protected SimpleItem generateForwardPreviousButtonOrDefault(SimpleItem defaultItem) {
        return swordPlayer.getPlayerMenuManager().noPreviousMenu() ?
            defaultItem :
            new SimpleItem(
                new ItemBuilder(Material.OAK_TRAPDOOR)
                    .clearItemFlags()
                    .setDisplayName("Go forward"),
                click -> swordPlayer.getPlayerMenuManager().openForwardPreviousMenu()
            );
    }
}
