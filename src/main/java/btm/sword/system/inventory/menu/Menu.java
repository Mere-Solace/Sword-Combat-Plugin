package btm.sword.system.inventory.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.bukkit.Material;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public abstract class Menu {
    protected final SwordPlayer swordPlayer;

    protected static final SimpleItem BORDER = new SimpleItem(
        new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text("|[]|", Config.SwordColor.TEXT_COOL_DARK))
            .build()
    );

    protected static final SimpleItem WALL = new SimpleItem(
        new ItemStackBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            .name(Component.text("|||", Config.SwordColor.TEXT_COOL_DARK))
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

    /**
     * Builds a toggle {@link SimpleItem} that flips a boolean flag on click and
     * reopens this menu to reflect the updated state.
     *
     * @param label  display label shown in the item name
     * @param getter reads the current flag value
     * @param toggle flips the flag
     * @return a {@link SimpleItem} representing the toggle button
     */
    protected SimpleItem toggle(String label, BooleanSupplier getter, Runnable toggle) {
        Consumer<Click> onClick = click -> {
            toggle.run();
            this.open();
        };

        if (getter.getAsBoolean()) {
            return new SimpleItem(
                new ItemStackBuilder(Material.LIME_DYE)
                    .name(Component.text(label + ": ", NamedTextColor.GRAY)
                        .append(Component.text("ON", NamedTextColor.GREEN, TextDecoration.BOLD)))
                    .build(),
                onClick
            );
        } else {
            return new SimpleItem(
                new ItemStackBuilder(Material.GRAY_DYE)
                    .name(Component.text(label + ": ", NamedTextColor.GRAY)
                        .append(Component.text("OFF", NamedTextColor.RED, TextDecoration.BOLD)))
                    .build(),
                onClick
            );
        }
    }
}
