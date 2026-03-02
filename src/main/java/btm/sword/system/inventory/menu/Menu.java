package btm.sword.system.inventory.menu;

import org.bukkit.Material;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public abstract class Menu {
    protected final SwordPlayer swordPlayer;

    protected static final SimpleItem BORDER = new SimpleItem(
        new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text("|[]|", Config.SwordColor.TEXT_COOL_DARK))
            .build()
    );

    public Menu(SwordPlayer player) {
        this.swordPlayer = player;
    }

    public abstract void open();

    protected SimpleItem generatePreviousButtonOrDefault() {
        return new SimpleItem(
                new ItemBuilder(Material.WAXED_COPPER_TRAPDOOR)
                    .clearItemFlags()
                    .setDisplayName("Go back"),
                click -> swordPlayer.getPlayerMenuManager().openPreviousMenu()
            );
    }

    protected SimpleItem generateForwardPreviousButtonOrDefault() {
        return new SimpleItem(
                new ItemBuilder(Material.WAXED_COPPER_TRAPDOOR)
                    .clearItemFlags()
                    .setDisplayName("Go forward"),
                click -> swordPlayer.getPlayerMenuManager().openForwardPreviousMenu()
            );
    }
}
