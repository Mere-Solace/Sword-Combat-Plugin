package btm.sword.join.menu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.bukkit.Material;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.join.Destination;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.Menu;
import btm.sword.menu.character.CharacterMenu;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Top-level join-sequence menu shown to a player while they are held in the dark-room
 * staging slot.
 *
 * <h2>Responsibility</h2>
 * <p>Surfaces four destination buttons (hub, quick-join, adventure, roguelike) and a
 * player-stats opener. The menu is purely an intent-raising surface — when the player
 * clicks a destination button, the menu invokes the supplied {@code onSelect} callback
 * with the chosen {@link Destination}. The menu never teleports, runs countdowns, or
 * decides what the chosen destination means; that responsibility lies with the caller
 * (the join lifecycle owner).</p>
 *
 * <h2>Click idempotency</h2>
 * <p>Once any destination button is clicked the menu refuses subsequent destination
 * clicks until it is reopened. This prevents a single open menu from raising multiple
 * intents in quick succession (which would race in the caller's lifecycle code). The
 * player-stats button is exempt because it merely opens a sub-menu and does not
 * commit a routing decision.</p>
 */
public class JoinRouterMenu extends Menu {

    private final Consumer<Destination> onSelect;

    /**
     * Latches on the first destination click. Subsequent destination clicks on the same
     * menu instance are ignored. Reset by reopening: a new menu instance starts unset.
     */
    private final AtomicBoolean selected = new AtomicBoolean(false);

    /**
     * Constructs a join-router menu bound to {@code player} that raises destination intent
     * through {@code onSelect}.
     *
     * @param player   the player this menu belongs to; never null
     * @param onSelect callback invoked exactly once with the chosen {@link Destination}
     *                 the first time the player clicks a destination button. The callback
     *                 is responsible for closing the InvUI window and performing any
     *                 follow-up (countdown, teleport, gamemode change, etc.). Never null.
     */
    public JoinRouterMenu(SwordPlayer player, Consumer<Destination> onSelect) {
        super(player);
        this.onSelect = onSelect;
    }

    /**
     * Returns whether this menu instance has already raised a destination intent.
     * Intended for the menu re-display ticker so it can stop reopening a menu that has
     * already been "consumed" by a click.
     *
     * @return {@code true} if a destination button has been clicked on this instance
     */
    public boolean isSelected() {
        return selected.get();
    }

    @Override
    public void open() {
        SimpleItem playerInfo = new SimpleItem(
            swordPlayer.getPlayerHeadItemWithCustomText(
                Component.text("Player Stats"),
                List.of(Component.text("View your stats, loadout, and progress"))
            ),
            click -> InventoryMenuManager.openMenu(CharacterMenu.class, swordPlayer)
        );

        SimpleItem hub = destinationButton(
            Material.CAMPFIRE,
            "Hub",
            "The central social area",
            Destination.HUB
        );

        SimpleItem quickJoin = destinationButton(
            Material.IRON_SWORD,
            "Quick-Join",
            "Enter the matchmaking queue",
            Destination.QUICK_JOIN
        );

        SimpleItem adventure = destinationButton(
            Material.GRASS_BLOCK,
            "Adventure",
            "Enter the adventure world",
            Destination.ADVENTURE
        );

        SimpleItem roguelike = destinationButton(
            Material.WITHER_SKELETON_SKULL,
            "Roguelike",
            "Enter the roguelike world",
            Destination.ROGUELIKE
        );

        // TODO: edit settings (S)
        // TODO: edit saves (X)

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

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("MainMenu")
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Builds an inert-on-second-click destination button. The first click latches the
     * {@link #selected} flag and dispatches {@code destination} to the {@code onSelect}
     * callback. Subsequent clicks on this menu instance are ignored.
     */
    private SimpleItem destinationButton(Material material,
                                         String title,
                                         String description,
                                         Destination destination) {
        return new SimpleItem(
            ItemStackBuilder.of(material)
                .name(Component.text(title))
                .lore(List.of(Component.text(description)))
                .build(),
            click -> {
                if (!selected.compareAndSet(false, true)) return;
                onSelect.accept(destination);
            }
        );
    }
}
