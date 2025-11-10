package btm.sword.system.inventory;

import java.util.Objects;

import org.bukkit.Material;

import btm.sword.system.entity.types.SwordPlayer;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class InventoryManager {
    public static void createBasic(SwordPlayer swordPlayer) {
        Gui gui = Gui.normal() // Creates the GuiBuilder for a normal GUI
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # # # # # # #")
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
                .build();

        Window window = Window.single()
                .setViewer(Objects.requireNonNull(swordPlayer.player()))
                .setTitle("InvUI")
                .setGui(gui)
                .build();

        window.open();
    }
}
