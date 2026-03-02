package btm.sword.system.inventory.menu;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Menu displaying all base moveset actions with their input sequences,
 * and a toggle button to enable or disable gated movement (dash) inputs.
 */
public class MovesetMenu extends Menu {

    public MovesetMenu(SwordPlayer player) {
        super(player);
    }

    /**
     * Opens the moveset reference menu for the player.
     * Each base move is shown as a named item with its input sequence and a brief description.
     * The movement toggle button reflects the current dash-enabled state and can be clicked
     * to toggle it on or off.
     */
    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem basicAttack = new SimpleItem(
            new ItemStackBuilder(Material.IRON_SWORD)
                .hideAll()
                .name(Component.text("Basic Attack Chain", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Left Click (× 3)", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Chain up to 3 slashes.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem grab = new SimpleItem(
            new ItemStackBuilder(Material.LEAD)
                .hideAll()
                .name(Component.text("Grab", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Shift + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Short-range control tool.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem throwItem = new SimpleItem(
            new ItemStackBuilder(Material.ARROW)
                .hideAll()
                .name(Component.text("Throw Weapon", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Drop + Right Click, hold to release", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Deals Toughness / Shards damage on hit.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem bladeToggle = new SimpleItem(
            new ItemStackBuilder(Material.ECHO_SHARD)
                .hideAll()
                .name(Component.text("Blade: Toggle Standby / Sheathed", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Shift + Swap", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Standby: blade hovers and awaits commands.", Config.SwordColor.TEXT_ITEM_BASE),
                    Component.text("  Sheathed: blade returns to your hip.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem bladeWield = new SimpleItem(
            new ItemStackBuilder(Material.NETHERITE_SWORD)
                .hideAll()
                .name(Component.text("Blade: Wield", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Shift + Drop", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Equips the Blade directly into your hand.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem bladeQuickAttacks = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .hideAll()
                .name(Component.text("Blade: Quick Attack Chain", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Left Click (× 3)  (Standby, costs Soulfire)", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Chain up to 3 remote slashes.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem bladeHeavySweep = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .hideAll()
                .name(Component.text("Blade: Heavy Sweep", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Drop + Left Click (× 3)", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Repeated Left clicks increase sweep force.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem bladeLunge = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .hideAll()
                .name(Component.text("Blade: Lunge", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Drop + Right Click  (Standby, costs Soulfire)", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Propel the blade at your target.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        SimpleItem umbralSkills = new SimpleItem(
            new ItemStackBuilder(Material.BOOK)
                .hideAll()
                .name(Component.text("Umbral Skill Slots", Config.SwordColor.TEXT_ITEM_NAME))
                .lore(List.of(
                    Component.text("Swap + Left + Left / Right / Swap", Config.SwordColor.TEXT_ITEM_CONTROLS),
                    Component.text("  Activate Umbral Skill slots 1, 2, or 3.", Config.SwordColor.TEXT_ITEM_BASE)
                ))
                .build()
        );

        boolean movementOn = swordPlayer.isMovementEnabled();
        SimpleItem movementToggle = new SimpleItem(
            buildMovementToggleItem(movementOn),
            click -> {
//                swordPlayer.toggleMovementInputs();
                this.open();
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A B C . D E . #",
                "# . . . . . . . #",
                "# F G H . I . . #",
                "# . . . . . M . #",
                "# # # # # # # < #")
            .addIngredient('#', BORDER)
            .addIngredient('A', basicAttack)
            .addIngredient('B', grab)
            .addIngredient('C', throwItem)
            .addIngredient('D', bladeToggle)
            .addIngredient('E', bladeWield)
            .addIngredient('F', bladeQuickAttacks)
            .addIngredient('G', bladeHeavySweep)
            .addIngredient('H', bladeLunge)
            .addIngredient('I', umbralSkills)
            .addIngredient('M', movementToggle)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Combat Reference")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Builds the movement toggle item with the correct color and label based on the enabled state.
     *
     * @param enabled whether movement inputs are currently enabled
     * @return the colored leather boots ItemStack
     */
    private ItemStack buildMovementToggleItem(boolean enabled) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(enabled ? Color.LIME : Color.RED);

        return new ItemStackBuilder(Material.LEATHER_BOOTS)
            .setMeta(meta)
            .hideAll()
            .name(Component.text(
                "Movement: " + (enabled ? "ON" : "OFF"),
                enabled ? Config.SwordColor.TEXT_ITEM_HEADER : Config.SwordColor.TEXT_COOL_DARK,
                TextDecoration.BOLD))
            .lore(List.of(Component.text("Click to toggle dash inputs.", Config.SwordColor.TEXT_ITEM_BASE)))
            .build();
    }
}
