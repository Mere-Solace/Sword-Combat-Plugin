package btm.sword.system.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.system.entity.types.SwordPlayer;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

// barrier is good material for cancel
// Remember the other types of windows!

public class InvInterfaceManager {
    public static void displayMainMenu(SwordPlayer swordPlayer) {
        Player player = swordPlayer.player();

        SimpleItem queueForCTF = new SimpleItem(
            new ItemBuilder(Material.GUSTER_BANNER_PATTERN).setDisplayName("Join the Queue for Capture the Flag"),
            click ->  click.getPlayer().sendMessage("Starting!")
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# . Q . . . . . #",
                "# . . . H . . . #",
                "# . . . . . . . #",
                "# . . . . . . . #",
                "# # # # # # # # #")
            .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
            .addIngredient('Q', queueForCTF)
            .addIngredient('H', swordPlayer.getPlayerHead())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("InvUI")
            .setGui(gui)
            .build();

        window.open();
    }
}
