package btm.sword.system.inventory.menu;

import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.Debug;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Submenu of {@link DevMenu} listing all boolean debug and world toggles.
 * <p>
 * Toggles take effect immediately without a server restart and are not persisted across restarts.
 * </p>
 */
public class TogglesMenu extends Menu {

    /**
     * Creates a new TogglesMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public TogglesMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem verboseDebug = toggle(
            "Debug (general)",
            () -> Config.Debug.LOGGING_VERBOSE_DEBUG,
            () -> Config.Debug.LOGGING_VERBOSE_DEBUG = !Config.Debug.LOGGING_VERBOSE_DEBUG
        );

        SimpleItem verboseCombat = toggle(
            "Combat",
            () -> Config.Debug.LOGGING_VERBOSE_COMBAT,
            () -> Config.Debug.LOGGING_VERBOSE_COMBAT = !Config.Debug.LOGGING_VERBOSE_COMBAT
        );

        SimpleItem verboseMovement = toggle(
            "Movement",
            () -> Config.Debug.LOGGING_VERBOSE_MOVEMENT,
            () -> Config.Debug.LOGGING_VERBOSE_MOVEMENT = !Config.Debug.LOGGING_VERBOSE_MOVEMENT
        );

        SimpleItem verboseInventory = toggle(
            "Inventory",
            () -> Config.Debug.LOGGING_VERBOSE_INVENTORY,
            () -> Config.Debug.LOGGING_VERBOSE_INVENTORY = !Config.Debug.LOGGING_VERBOSE_INVENTORY
        );

        SimpleItem verboseSystem = toggle(
            "System (FSM / tasks)",
            () -> Config.Debug.LOGGING_VERBOSE_SYSTEM,
            () -> Config.Debug.LOGGING_VERBOSE_SYSTEM = !Config.Debug.LOGGING_VERBOSE_SYSTEM
        );

        SimpleItem verboseUmbral = toggle(
            "Umbral",
            () -> Config.Debug.LOGGING_VERBOSE_UMBRAL,
            () -> Config.Debug.LOGGING_VERBOSE_UMBRAL = !Config.Debug.LOGGING_VERBOSE_UMBRAL
        );

        SimpleItem verboseHostile = toggle(
            "Hostile",
            () -> Config.Debug.LOGGING_VERBOSE_HOSTILE,
            () -> Config.Debug.LOGGING_VERBOSE_HOSTILE = !Config.Debug.LOGGING_VERBOSE_HOSTILE
        );

        SimpleItem verboseListener = toggle(
            "Listener",
            () -> Config.Debug.LOGGING_VERBOSE_LISTENER,
            () -> Config.Debug.LOGGING_VERBOSE_LISTENER = !Config.Debug.LOGGING_VERBOSE_LISTENER
        );

        SimpleItem specialItemChecks = toggle(
            "Special item checks",
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED,
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED = !Debug.SPECIAL_ITEM_CHECKS_ENABLED
        );

        SimpleItem blockPlacing = toggle(
            "Block placing",
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING,
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = !Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING
        );

        SimpleItem verboseAnimation = toggle(
            "Animation (DEU hook)",
            () -> Config.Debug.LOGGING_VERBOSE_ANIMATION,
            () -> Config.Debug.LOGGING_VERBOSE_ANIMATION = !Config.Debug.LOGGING_VERBOSE_ANIMATION
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A B C D E . . #",
                "# F G H I J K . #",
                "# . . . . . . . #",
                "< # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', verboseDebug)
            .addIngredient('B', verboseCombat)
            .addIngredient('C', verboseMovement)
            .addIngredient('D', verboseInventory)
            .addIngredient('E', verboseSystem)
            .addIngredient('F', verboseUmbral)
            .addIngredient('G', verboseHostile)
            .addIngredient('H', verboseListener)
            .addIngredient('I', specialItemChecks)
            .addIngredient('J', blockPlacing)
            .addIngredient('K', verboseAnimation)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Dev Toggles")
            .setGui(gui)
            .build();

        window.open();
    }
}
