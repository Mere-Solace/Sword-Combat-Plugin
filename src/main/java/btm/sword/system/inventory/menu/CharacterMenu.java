package btm.sword.system.inventory.menu;


import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.system.entity.impl.SwordPlayer;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class CharacterMenu extends Menu {

    public CharacterMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        Gui gui = Gui.normal()
            .setStructure(
                "# # # . . . # # #",
                "# . . . S . . . #",
                "< . . . . . . . .",
                "> . . . . . . . .",
                "# . . . . . . . #",
                "# # # . . . # # #")
            .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
            .addIngredient('S', swordPlayer.getPlayerHead())
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault())
            .addIngredient('!', backButton())
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
