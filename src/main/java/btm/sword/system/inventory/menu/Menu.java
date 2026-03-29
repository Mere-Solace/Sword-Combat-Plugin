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

/**
 * Abstract base class for all InvUI-backed Sword menus.
 * <p>
 * Provides shared decorative items ({@link #BORDER}, {@link #WALL}), navigation helpers
 * ({@link #generatePreviousButtonOrDefault()}, {@link #generateForwardPreviousButtonOrDefault()}),
 * and the {@link #toggle(String, BooleanSupplier, Runnable)} factory for boolean-toggle items.
 * Concrete subclasses implement {@link #open()} to build and display their specific GUI.
 * </p>
 */
public abstract class Menu {

    /** The player this menu instance belongs to. */
    protected final SwordPlayer swordPlayer;

    /** Shared black stained-glass-pane border item used as inactive GUI decoration. */
    public static final SimpleItem BORDER = new SimpleItem(
        new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(Component.text("|[]|", Config.SwordColor.TEXT_COOL_DARK))
            .build()
    );

    public static final SimpleItem WALL = new SimpleItem(
        new ItemStackBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            .name(Component.text("|||", Config.SwordColor.TEXT_COOL_DARK))
            .build()
    );

    /**
     * Creates a Menu instance bound to the given player.
     *
     * @param player the player this menu belongs to
     */
    public Menu(SwordPlayer player) {
        this.swordPlayer = player;
    }

    /** Opens this menu for the bound player. Implementations build and display the InvUI window. */
    public abstract void open();

    /**
     * Returns a "Go back" navigation button that opens the previous menu in the player's history.
     *
     * @return a {@link SimpleItem} wired to {@link btm.sword.system.inventory.PlayerMenuManager#openPreviousMenu()}
     */
    protected SimpleItem generatePreviousButtonOrDefault() {
        return new SimpleItem(
                new ItemBuilder(Material.WAXED_COPPER_TRAPDOOR)
                    .clearItemFlags()
                    .setDisplayName("Go back"),
                click -> swordPlayer.getPlayerMenuManager().openPreviousMenu()
            );
    }

    /**
     * Returns a "Go forward" navigation button that re-opens the next menu in the player's history.
     *
     * @return a {@link SimpleItem} wired to {@link btm.sword.system.inventory.PlayerMenuManager#openForwardPreviousMenu()}
     */
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
