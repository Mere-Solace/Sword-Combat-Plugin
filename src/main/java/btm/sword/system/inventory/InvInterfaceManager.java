package btm.sword.system.inventory;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.types.Dummy;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

// barrier is good material for cancel
// Remember the other types of windows!

public class InvInterfaceManager {
    public static final List<Component> HOW_TO_PLAY = List.of(
        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Combat Basics", Config.SwordColor.TEXT_ITEM_HEADER, TextDecoration.ITALIC),

        Component.text("Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Basic Attack Chain", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("  • SLASH1 → SLASH2 → SLASH3", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Swap + Swap", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Forward Dash", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("  • Can be used in air", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Swap + Swap + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Dash Attack", Config.SwordColor.TEXT_ITEM_BASE)),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Shift + Shift", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Backward Dash", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Shift + Shift + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Back Dash Attack", Config.SwordColor.TEXT_ITEM_BASE)),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Shift + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Grab", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Shift + Left Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
            .append(Component.text(" – Grab", Config.SwordColor.TEXT_ITEM_BASE)),


        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Drop + Right Click", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Ready Throw", Config.SwordColor.TEXT_ITEM_BASE)),
        Component.text("Drop + Right Hold", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Throw Item", Config.SwordColor.TEXT_ITEM_BASE)),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Right Click + Hold + Drop", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Bullet Time", Config.SwordColor.TEXT_ITEM_BASE))
        .append(Component.text(" (slow-mo)", Config.SwordColor.TEXT_ITEM_HEADER)),

        Component.text("", Config.SwordColor.TEXT_ITEM_BASE),

        Component.text("Drop + Drop", Config.SwordColor.TEXT_ITEM_CONTROLS)
        .append(Component.text(" – Emergency Reset", Config.SwordColor.TEXT_ITEM_BASE))
        );

    public static final ItemStack HOW_TO_PLAY_ITEM = ItemStackBuilder
        .of(Material.KNOWLEDGE_BOOK)
        .hideAll()
        .name(Component.text("Input Instructions", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
        .lore(HOW_TO_PLAY)
        .build();

    public static void displayMainMenu(SwordPlayer swordPlayer) {
        Player player = swordPlayer.player();

        SimpleItem queueForCTF = new SimpleItem(
            new ItemBuilder(Material.GUSTER_BANNER_PATTERN)
                .setDisplayName("Join the Queue for Capture the Flag"),
            click ->  click.getPlayer().sendMessage("Starting!")
        );

        SimpleItem spawnDummy = new SimpleItem(
            new ItemBuilder(Material.ARMOR_STAND)
                .setDisplayName("Spawn a Training Dummy (max: 3 per player)"),
            click ->  {
                if (swordPlayer.getCurNumDummies() >= swordPlayer.getMaxNumDummies()) {
                    swordPlayer.message("You have the max number of dummies active!");
                    return;
                }
                ArmorStand dummy = (ArmorStand) swordPlayer.world().spawnEntity(swordPlayer.locFromEyeDir(2), EntityType.ARMOR_STAND);
                dummy.addScoreboardTag("dummy");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Dummy swordDummy = (Dummy) SwordEntityArbiter.getOrAdd(dummy);
                        if (swordDummy == null || swordDummy.isInvalid()) {
                            cancel();
                            return;
                        }
                        swordDummy.setOwner(swordPlayer);
                        swordPlayer.getYourDummies().add(swordDummy);
                        swordPlayer.incrementNumDummies();
                    }
                }.runTaskLater(Sword.getInstance(), 2L);
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# . Q . . . . . #",
                "# . . . H . . . #",
                "# . . . P . . . #",
                "# . . . . . D . #",
                "# # # # # # # # #")
            .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
            .addIngredient('Q', queueForCTF)
            .addIngredient('H', swordPlayer.getPlayerHead())
            .addIngredient('P', HOW_TO_PLAY_ITEM)
            .addIngredient('D', spawnDummy)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("InvUI")
            .setGui(gui)
            .build();

        window.open();
    }
}
