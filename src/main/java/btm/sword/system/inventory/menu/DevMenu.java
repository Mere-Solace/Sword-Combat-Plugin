package btm.sword.system.inventory.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.Debug;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * In-game developer menu providing runtime toggles for debug flags and
 * diagnostic utilities. Accessible from the {@link MainMenu} for op players.
 * <p>
 * Toggles take effect immediately without a server restart. They are not
 * persisted — all flags reset to their defaults on server restart.
 * </p>
 *
 * <p>Row 1 (verbose flags): Debug, Combat, Movement, Inventory, System, Umbral, Hostile</p>
 * <p>Row 2 (utilities): Special Item Checks, Reload Profile, Config Editor</p>
 */
public class DevMenu extends Menu {

    public DevMenu(SwordPlayer player) {
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

        SimpleItem specialItemChecks = toggle(
            "Special item checks",
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED,
            () -> Debug.SPECIAL_ITEM_CHECKS_ENABLED = !Debug.SPECIAL_ITEM_CHECKS_ENABLED
        );

        SimpleItem reloadProfile = new SimpleItem(
            new ItemStackBuilder(Material.RECOVERY_COMPASS)
                .name(Component.text("Reload Combat Profile", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build(),
            click -> {
                swordPlayer.getCombatProfile().reloadFromConfig();
                swordPlayer.getAspects().reloadFromProfile(swordPlayer.getCombatProfile());
                swordPlayer.message(Component.text("Combat profile reloaded from config.", NamedTextColor.GREEN));
            }
        );

        SimpleItem configEditor = new SimpleItem(
            new ItemStackBuilder(Material.COMPARATOR)
                .name(Component.text("Config Editor", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build(),
            click -> new ConfigMenu(swordPlayer).open()
        );

        SimpleItem blockPlacing = toggle(
            "Block placing",
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING,
            () -> Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = !Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING
        );

        SimpleItem woodenAxe = giveItem(Material.WOODEN_AXE, "Wooden Axe");
        SimpleItem witherSkeletonEgg = giveItem(Material.WITHER_SKELETON_SPAWN_EGG, "Wither Skeleton Spawn Egg");

        SimpleItem creativeInventory = new SimpleItem(
            new ItemStackBuilder(Material.CHEST)
                .name(Component.text("Creative Inventory", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .build(),
            click -> new CreativeInventoryMenu(swordPlayer).open()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "A B C D H E F . J",
                ". . . . . . . . I",
                "W S L . . . . . K",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', verboseDebug)
            .addIngredient('B', verboseCombat)
            .addIngredient('C', verboseMovement)
            .addIngredient('D', verboseInventory)
            .addIngredient('E', verboseSystem)
            .addIngredient('F', verboseUmbral)
            .addIngredient('G', verboseHostile)
            .addIngredient('H', specialItemChecks)
            .addIngredient('I', reloadProfile)
            .addIngredient('J', configEditor)
            .addIngredient('L', blockPlacing)
            .addIngredient('W', woodenAxe)
            .addIngredient('S', witherSkeletonEgg)
            .addIngredient('K', creativeInventory)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Dev Menu")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Builds a {@link SimpleItem} that gives the player one stack of the given material on click.
     *
     * @param material the material to give
     * @param label    display name for the item button
     * @return a {@link SimpleItem} that gives the item on click
     */
    private SimpleItem giveItem(Material material, String label) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label, NamedTextColor.WHITE))
                .build(),
            click -> click.getPlayer().getInventory().addItem(new ItemStack(material))
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
    private SimpleItem toggle(String label, BooleanSupplier getter, Runnable toggle) {
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
        }
        else {
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
