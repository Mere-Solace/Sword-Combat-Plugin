package btm.sword.system.inventory.menu;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.scene.StaticSceneController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * In-game developer menu providing access to diagnostic utilities, dev tools, and the
 * {@link TogglesMenu} for runtime boolean flags. Accessible from the {@link MainMenu} for op players.
 * <p>
 * All changes take effect immediately without a server restart and are not persisted across restarts.
 * </p>
 */
public class DevMenu extends Menu {

    /**
     * Creates a new DevMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public DevMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem toggles = new SimpleItem(
            new ItemStackBuilder(Material.LEVER)
                .name(Component.text("Toggles", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Debug flags and world toggles", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new TogglesMenu(swordPlayer).open()
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

        SimpleItem woodenAxe = giveItem(Material.WOODEN_AXE, "Wooden Axe");
        SimpleItem witherSkeletonEgg = giveItem(Material.WITHER_SKELETON_SPAWN_EGG, "Wither Skeleton Spawn Egg");

        SimpleItem creativeInventory = new SimpleItem(
            new ItemStackBuilder(Material.CHEST)
                .name(Component.text("Creative Inventory", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .build(),
            click -> new CreativeInventoryMenu(swordPlayer).open()
        );

        SimpleItem reloadInventoryButtons = new SimpleItem(
            new ItemStackBuilder(Material.CLOCK)
                .name(Component.text("Reload Inventory Buttons", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build(),
            click -> {
                swordPlayer.reloadInventoryButtons();
                swordPlayer.message(Component.text("Inventory buttons reloaded.", NamedTextColor.GREEN));
            }
        );

        SimpleItem staticScene = new SimpleItem(
            new ItemStackBuilder(Material.SPYGLASS)
                .name(Component.text("Static Scene Test", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Fix camera behind you, freeze input", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                Player p = click.getPlayer();
                Location playerLoc = p.getLocation();
                Location camLoc = playerLoc.clone()
                    .subtract(playerLoc.getDirection().multiply(3))
                    .add(0, 1, 0);
                camLoc.setDirection(playerLoc.toVector().subtract(camLoc.toVector()));
                new StaticSceneController(camLoc).start(swordPlayer);
            }
        );

        SimpleItem deuTools = new SimpleItem(
            new ItemStackBuilder(Material.ITEM_FRAME)
                .name(Component.text("DisplayEntityUtils", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Animations, groups, despawn tools", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DEUBDEMenu(swordPlayer).open()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# J N S . . . C #",
                "# . . . . . . E #",
                "# R I . T . . W #",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('T', toggles)
            .addIngredient('R', reloadInventoryButtons)
            .addIngredient('J', configEditor)
            .addIngredient('N', deuTools)
            .addIngredient('S', staticScene)
            .addIngredient('C', creativeInventory)
            .addIngredient('W', woodenAxe)
            .addIngredient('E', witherSkeletonEgg)
            .addIngredient('I', reloadProfile)
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
}
