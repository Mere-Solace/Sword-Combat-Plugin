package btm.sword.system.inventory.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.system.entity.types.SwordPlayer;
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
                "B . . . . . . . .",
                "F . . . . . . . .",
                "# . . . . . . . .",
                "# . . . . . . . .",
                "# . . . . . . . .",
                "# . . . . . . . .")
            .addIngredient('#', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)))
            .addIngredient('B', generatePreviousButtonOrDefault(defaultSimpleItem))
            .addIngredient('F', generateForwardPreviousButtonOrDefault(defaultSimpleItem))
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("MainMenu")
            .setGui(gui)
            .build();

        window.open();
    }
}
