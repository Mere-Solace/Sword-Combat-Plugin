package btm.sword.system.inventory.menu.dev;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.ItemLibraryMenu;
import btm.sword.system.inventory.menu.MainMenu;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
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

        SimpleItem testing = new SimpleItem(
            new ItemStackBuilder(Material.ARMOR_STAND)
                .name(Component.text("Testing Actions", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build(),
            click -> new TestingMenu(swordPlayer).open()
        );

        SimpleItem woodenAxe = giveItem(Material.WOODEN_AXE, "Wooden Axe");
        SimpleItem witherSkeletonEgg = giveItem(Material.WITHER_SKELETON_SPAWN_EGG, "Wither Skeleton Spawn Egg");
        SimpleItem blockDisplayEntitySpawnEgg = giveItem(Material.PILLAGER_SPAWN_EGG, "Pillager Spawn Egg");

        SimpleItem creativeMode;
        if (swordPlayer.isInCreativeDevMode()) {
            creativeMode = new SimpleItem(
                new ItemStackBuilder(Material.GRASS_BLOCK)
                    .name(Component.text("Return to Survival", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .lore(List.of(Component.text("Restore inventory and re-enable special items", NamedTextColor.DARK_GRAY)))
                    .build(),
                click -> {
                    swordPlayer.exitCreativeDevMode();
                    new DevMenu(swordPlayer).open();
                }
            );
        } else {
            creativeMode = new SimpleItem(
                new ItemStackBuilder(Material.DIAMOND_PICKAXE)
                    .name(Component.text("Enter Creative Mode", NamedTextColor.AQUA, TextDecoration.BOLD))
                    .lore(List.of(Component.text("Save inventory, disable special items, enable block placing", NamedTextColor.DARK_GRAY)))
                    .build(),
                click -> {
                    swordPlayer.enterCreativeDevMode();
                    new DevMenu(swordPlayer).open();
                }
            );
        }

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

        SimpleItem deuTools = new SimpleItem(
            new ItemStackBuilder(Material.ITEM_FRAME)
                .name(Component.text("DisplayEntityUtils", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Animations, groups, despawn tools", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DEUBDEMenu(swordPlayer).open()
        );

        SimpleItem skillEquip = new SimpleItem(
            new ItemStackBuilder(Material.KNOWLEDGE_BOOK)
                .name(Component.text("Skill Equip", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Force-equip any skill to any slot", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new DevSkillEquipMenu(swordPlayer).open()
        );

        SimpleItem itemLibrary = new SimpleItem(
            new ItemStackBuilder(Material.BOOKSHELF)
                .name(Component.text("Item Library", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(Component.text("Browse all registered game items", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new ItemLibraryMenu(swordPlayer).open()
        );

        SimpleItem weaponDisplay = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("Weapon Display", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Tweak per-material weapon slot transforms", NamedTextColor.DARK_GRAY),
                    Component.text("Hold the item you want to configure", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> new WeaponDisplayEditorMenu(swordPlayer).open()
        );

        SimpleItem attackEditor = new SimpleItem(
            new ItemStackBuilder(Material.GOLDEN_SWORD)
                .name(Component.text("Attack Editor", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(Component.text("Browse and edit VOLUME attack definitions", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new AttackBrowserMenu(swordPlayer).open()
        );


        Gui gui = Gui.normal()
            .setStructure(
                "# # # . . . # # #",
                "# J N . T . L C #",
                ". H V . ? . . E .",
                ". . . . A . . P .",
                "# R I . . . M W #",
                "< > # . . . # # #")
            .addIngredient('#', BORDER)
            .addIngredient('T', toggles)
            .addIngredient('R', reloadInventoryButtons)
            .addIngredient('J', configEditor)
            .addIngredient('?', testing)
            .addIngredient('N', deuTools)
            .addIngredient('A', skillEquip)
            .addIngredient('L', itemLibrary)
            .addIngredient('C', creativeInventory)
            .addIngredient('W', woodenAxe)
            .addIngredient('E', witherSkeletonEgg)
            .addIngredient('P', blockDisplayEntitySpawnEgg)
            .addIngredient('I', reloadProfile)
            .addIngredient('M', creativeMode)
            .addIngredient('H', weaponDisplay)
            .addIngredient('V', attackEditor)
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
            click -> click.getPlayer().getInventory().addItem(ItemStack.of(material))
        );
    }
}
