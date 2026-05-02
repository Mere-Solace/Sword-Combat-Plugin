package btm.sword.menu;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;

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

    public static final SimpleItem EMPTY = new SimpleItem(
        new ItemStackBuilder(Material.AIR)
            .name(Component.text("]|["))
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
     * @return a {@link SimpleItem} wired to {@link btm.sword.menu.PlayerMenuManager#openPreviousMenu()}
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
     * @return a {@link SimpleItem} wired to {@link btm.sword.menu.PlayerMenuManager#openForwardPreviousMenu()}
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
     * Builds a {@link SimpleItem} that gives the player one stack of the given material on click.
     *
     * @param material the material to give
     * @param label    display name for the item button
     * @return a {@link SimpleItem} that gives the item on click
     */
    protected SimpleItem giveItem(Material material, String label) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label, NamedTextColor.WHITE))
                .build(),
            click -> click.getPlayer().getInventory().addItem(ItemStack.of(material))
        );
    }

    protected SimpleItem giveItem(ItemStack stack) {
        return new SimpleItem(
            stack,
            click -> click.getPlayer().getInventory().addItem(stack)
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
    /**
     * Opens an anvil window that accepts a {@code float} value.
     *
     * <p>Slot 0: paper showing the current/typed value. Slot 1: wool validity indicator.
     * Slot 2: barrier cancel button. On close the parsed value is passed to {@code onAccept}
     * (only if valid and not cancelled) and then {@code returnTo} is called.</p>
     *
     * @param label    displayed as the window title prefix and in the paper lore
     * @param current  initial value pre-filled in the paper name
     * @param onAccept called with the parsed value on valid accept
     * @param returnTo called after the window closes regardless of outcome
     */
    protected void openFloatAnvil(String label, float current, Consumer<Float> onAccept, Runnable returnTo) {
        openNumericAnvil(label, String.valueOf(current),
            s -> { try { Float.parseFloat(s); return true; } catch (NumberFormatException e) { return false; } },
            s -> onAccept.accept(Float.parseFloat(s)),
            returnTo);
    }

    /**
     * Opens an anvil window that accepts a {@code double} value.
     *
     * @param label    displayed as the window title prefix and in the paper lore
     * @param current  initial value pre-filled in the paper name
     * @param onAccept called with the parsed value on valid accept
     * @param returnTo called after the window closes regardless of outcome
     */
    protected void openDoubleAnvil(String label, double current, Consumer<Double> onAccept, Runnable returnTo) {
        openNumericAnvil(label, String.valueOf(current),
            s -> { try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; } },
            s -> onAccept.accept(Double.parseDouble(s)),
            returnTo);
    }

    /**
     * Opens an anvil window that accepts an {@code int} value.
     *
     * @param label    displayed as the window title prefix and in the paper lore
     * @param current  initial value pre-filled in the paper name
     * @param onAccept called with the parsed value on valid accept
     * @param returnTo called after the window closes regardless of outcome
     */
    protected void openIntAnvil(String label, int current, Consumer<Integer> onAccept, Runnable returnTo) {
        openNumericAnvil(label, String.valueOf(current),
            s -> { try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; } },
            s -> onAccept.accept(Integer.parseInt(s)),
            returnTo);
    }

    private void openNumericAnvil(String label, String initialValue,
                                   Predicate<String> isValid,
                                   Consumer<String> onValidAccept,
                                   Runnable returnTo) {
        Player player = swordPlayer.player();
        Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
            String[] lastTyped = {initialValue};
            boolean[] cancelled = {false};

            AbstractItem indicator = new AbstractItem() {
                @Override
                public ItemProvider getItemProvider() {
                    String text = lastTyped[0];
                    if (isValid.test(text)) {
                        return new ItemWrapper(new ItemStackBuilder(Material.LIME_WOOL)
                            .name(Component.text("Valid: " + text, NamedTextColor.GREEN, TextDecoration.BOLD))
                            .lore(List.of(Component.text("Click paper or Esc to accept", NamedTextColor.DARK_GRAY)))
                            .build());
                    }
                    return new ItemWrapper(new ItemStackBuilder(Material.RED_WOOL)
                        .name(Component.text("Invalid: not a number", NamedTextColor.RED, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text("\"" + text + "\" cannot be parsed", NamedTextColor.DARK_GRAY),
                            Component.text("Type a valid number", NamedTextColor.DARK_GRAY)))
                        .build());
                }

                @Override
                public void handleClick(@NotNull ClickType ct, @NotNull Player p,
                                        @NotNull InventoryClickEvent e) { }
            };

            AbstractItem paperInput = new AbstractItem() {
                @Override
                public ItemProvider getItemProvider() {
                    return new ItemWrapper(new ItemStackBuilder(Material.PAPER)
                        .name(Component.text(lastTyped[0], NamedTextColor.GOLD, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text(label, NamedTextColor.DARK_GRAY),
                            Component.text("Click to accept if valid  |  Esc to accept & close",
                                NamedTextColor.DARK_GRAY)))
                        .build());
                }

                @Override
                public void handleClick(@NotNull ClickType ct, @NotNull Player p,
                                        @NotNull InventoryClickEvent e) {
                    if (isValid.test(lastTyped[0])) p.closeInventory();
                }
            };

            AbstractItem backArrow = new AbstractItem() {
                @Override
                public ItemProvider getItemProvider() {
                    return new ItemWrapper(new ItemStackBuilder(Material.BARRIER)
                        .name(Component.text("Cancel", NamedTextColor.RED, TextDecoration.BOLD))
                        .lore(List.of(Component.text("Close without accepting", NamedTextColor.DARK_GRAY)))
                        .build());
                }

                @Override
                public void handleClick(@NotNull ClickType ct, @NotNull Player p,
                                        @NotNull InventoryClickEvent e) {
                    cancelled[0] = true;
                    p.closeInventory();
                }
            };

            Gui anvilGui = Gui.normal()
                .setStructure("X M B")
                .addIngredient('X', paperInput)
                .addIngredient('M', indicator)
                .addIngredient('B', backArrow)
                .build();

            AnvilWindow anvilWindow = AnvilWindow.single()
                .setViewer(player)
                .setTitle("Input a value | Esc. to accept")
                .setGui(anvilGui)
                .addRenameHandler(text -> {
                    lastTyped[0] = text;
                    indicator.notifyWindows();
                    paperInput.notifyWindows();
                })
                .build();

            anvilWindow.addCloseHandler(() -> {
                String result = lastTyped[0];
                boolean wasCancelled = cancelled[0];
                Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                    if (!wasCancelled && isValid.test(result)) onValidAccept.accept(result);
                    returnTo.run();
                });
            });

            anvilWindow.open();
        });
    }

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
