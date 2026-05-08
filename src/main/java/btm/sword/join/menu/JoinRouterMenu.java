package btm.sword.join.menu;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
 *
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
        SimpleItem hub = new SimpleItem(
            ItemStackBuilder.of(Material.CAMPFIRE)
                .build(),
            click -> {
                window.get().close();
                makeSelection();

                AtomicInteger ai = new AtomicInteger(0);
                TimeArbiter.runFixedIterationTaskTimer(
                    null,
                    () -> {
                        swordPlayer.displayTitle(
                            Component.text(ai.incrementAndGet()),
                            Component.text("Preparing to teleport..."),
                            0L, 1100L, 0L
                        );
                    },
                    0, 1000, 4,
                    JoinRouterMenu.class, "open",
                    () -> {
                        swordPlayer.displayTitle(
                            Component.text(ai.incrementAndGet()),
                            Component.text("Teleporting"),
                            0L, 1100L, 0L
                        );
                        swordPlayer.teleport(JoinSequenceConfig.HUB_SPAWN);
                    }
                );
            }
        );

        // quick join a match (tp to waiting room)

        // join adventure world

        // join roguelike world


        // edit settings

        // edit saves

        // view stats



        Gui.Builder.Normal builder = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# H . A . R . M #",
                ". . . . . . . . .",
                "# S . . P . . X #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('H', hub);

        Gui gui = builder.build();

        window.set(Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("MainMenu")
            .setGui(gui)
            .build()
        );
        window.get().open();
    }
}
