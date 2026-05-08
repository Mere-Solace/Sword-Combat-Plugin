package btm.sword.join.menu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.section.JoinSequenceConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.Menu;
import btm.sword.menu.character.CharacterMenu;
import btm.sword.runtime.scheduler.TimeArbiter;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Top-level join-sequence menu shown to a player while they are held in the dark-room
 * staging slot. Surfaces the four destination buttons (hub, quick-join, adventure,
 * roguelike) and a player-stats opener. Each destination button closes the window,
 * marks the selection, runs a configurable countdown title, then teleports the player
 * to the configured {@link Location}.
 *
 * <p>The countdown duration and tick period come from
 * {@link JoinSequenceConfig#ROUTING_DURATION_MS} and
 * {@link JoinSequenceConfig#ROUTING_TICK_PERIOD_MS}.</p>
 */
public class JoinRouterMenu extends Menu {
    private final AtomicBoolean madeSelection;

    /**
     * Creates a Menu instance bound to the given player.
     *
     * @param player
     *         the player this menu belongs to
     */
    public JoinRouterMenu(SwordPlayer player, AtomicBoolean madeSelection) {
        super(player);
        this.madeSelection = madeSelection;
    }

    /**
     *
     * @return
     */
    public boolean isSelectionMade() {
        return madeSelection.get();
    }

    /**
     *
     */
    public void makeSelection() {
        madeSelection.set(true);
    }

    /**
     *
     */
    @Override
    public void open() {
        Player player = swordPlayer.player();
        AtomicReference<Window> window = new AtomicReference<>();

        SimpleItem playerInfo = new SimpleItem(
            swordPlayer.getPlayerHeadItemWithCustomText(
                Component.text("Player Stats"),
                List.of(Component.text("View your stats, loadout, and progress"))
            ),
            click -> InventoryMenuManager.openMenu(CharacterMenu.class, swordPlayer)
        );

        // go to hub area
        SimpleItem hub = destinationButton(
            Material.CAMPFIRE,
            "Hub",
            "The central social area",
            JoinSequenceConfig.DESTINATION_HUB,
            window
        );

        // quick join a match (tp to waiting room)
        SimpleItem quickJoin = destinationButton(
            Material.IRON_SWORD,
            "Quick-Join",
            "Enter the matchmaking queue",
            JoinSequenceConfig.DESTINATION_QUICK_JOIN,
            window
        );

        // join adventure world
        SimpleItem adventure = destinationButton(
            Material.GRASS_BLOCK,
            "Adventure",
            "Enter the adventure world",
            JoinSequenceConfig.DESTINATION_ADVENTURE,
            window
        );

        // join roguelike world
        SimpleItem roguelike = destinationButton(
            Material.WITHER_SKELETON_SKULL,
            "Roguelike",
            "Enter the roguelike world",
            JoinSequenceConfig.DESTINATION_ROGUELIKE,
            window
        );

        // TODO: edit settings (S)
        // TODO: edit saves (X)
        // TODO: view stats — currently mapped to playerInfo head (P)

        Gui.Builder.Normal builder = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# H . A . R . M #",
                ". . . . . . . . .",
                "# S . . P . . X #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('H', hub)
            .addIngredient('A', adventure)
            .addIngredient('R', roguelike)
            .addIngredient('M', quickJoin)
            .addIngredient('P', playerInfo);

        Gui gui = builder.build();

        window.set(Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("MainMenu")
            .setGui(gui)
            .build()
        );
        window.get().open();
    }

    /**
     * Builds a destination button. Clicking the button closes the window, marks the menu's
     * selection flag, runs the configured countdown title, and finally teleports the player
     * to {@code destination}. Logic is duplicated across all four destinations so this
     * helper exists to keep the {@link #open()} body readable; it is replaced in the next
     * lifecycle iteration when {@link JoinSession} owns the routing sequence.
     *
     * @param material    item material for the button face
     * @param title       display name shown in the item tooltip
     * @param description single-line lore description
     * @param destination world location to teleport to on countdown completion
     * @param window      reference holder for the open InvUI window — captured so the
     *                    button can close the window before the countdown begins
     * @return an InvUI {@link SimpleItem} configured with the click behaviour
     */
    private SimpleItem destinationButton(Material material,
                                         String title,
                                         String description,
                                         Location destination,
                                         AtomicReference<Window> window) {
        return new SimpleItem(
            ItemStackBuilder.of(material)
                .name(Component.text(title))
                .lore(List.of(Component.text(description)))
                .build(),
            click -> startRoutingCountdown(destination, window)
        );
    }

    /**
     * Closes the menu window, marks the selection, and starts the configurable countdown
     * task that teleports the player to {@code destination} on completion.
     *
     * <p>Iteration count is derived from
     * {@link JoinSequenceConfig#ROUTING_DURATION_MS} and
     * {@link JoinSequenceConfig#ROUTING_TICK_PERIOD_MS} as
     * {@code (duration / period) - 1} so the lastIterationCallback fires at exactly
     * {@code duration} ms after the click.</p>
     */
    private void startRoutingCountdown(Location destination, AtomicReference<Window> window) {
        if (window.get() != null) window.get().close();
        makeSelection();

        AtomicInteger ai = new AtomicInteger(0);
        int periodMs = Math.max(1, JoinSequenceConfig.ROUTING_TICK_PERIOD_MS);
        int iterations = Math.max(0,
            (JoinSequenceConfig.ROUTING_DURATION_MS / periodMs) - 1);

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> swordPlayer.displayTitle(
                Component.text(ai.incrementAndGet()),
                Component.text("Preparing to teleport..."),
                0L, periodMs + 100L, 0L
            ),
            0, periodMs, iterations,
            JoinRouterMenu.class, "startRoutingCountdown",
            () -> {
                swordPlayer.displayTitle(
                    Component.text(ai.incrementAndGet()),
                    Component.text("Teleporting"),
                    0L, periodMs + 100L, 0L
                );
                swordPlayer.teleport(destination);
            }
        );
    }
}
