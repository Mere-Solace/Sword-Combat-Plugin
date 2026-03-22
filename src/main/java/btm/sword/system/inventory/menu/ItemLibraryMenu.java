package btm.sword.system.inventory.menu;

import java.util.List;

import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.LibraryCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Top-level item library menu presenting all {@link LibraryCategory} values as selectable buttons.
 *
 * <p>Clicking a category button opens the corresponding {@link ItemLibraryCategoryMenu}
 * for that category. Registered in {@link btm.sword.system.inventory.InventoryMenuManager}.</p>
 */
public class ItemLibraryMenu extends Menu {

    /**
     * Creates a new ItemLibraryMenu for the given player.
     *
     * @param player the player opening this menu
     */
    public ItemLibraryMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem armor = categoryButton(LibraryCategory.ARMOR);
        SimpleItem weapons = categoryButton(LibraryCategory.WEAPONS);
        SimpleItem materials = categoryButton(LibraryCategory.MATERIALS);
        SimpleItem quest = categoryButton(LibraryCategory.QUEST);

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "# A W M Q . . . #",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('A', armor)
            .addIngredient('W', weapons)
            .addIngredient('M', materials)
            .addIngredient('Q', quest)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('>', generateForwardPreviousButtonOrDefault())
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Item Library")
            .setGui(gui)
            .build()
            .open();
    }

    /**
     * Builds a category selection button for the given {@link LibraryCategory}.
     *
     * @param cat the category to represent
     * @return a {@link SimpleItem} button that opens the category menu on click
     */
    private SimpleItem categoryButton(LibraryCategory cat) {
        int count = cat.getItems().size();
        Component countLine = count == 0
            ? Component.text("No items yet", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            : Component.text(count + " item" + (count == 1 ? "" : "s"), Config.SwordColor.TEXT_COOL)
                .decoration(TextDecoration.ITALIC, false);

        return new SimpleItem(
            new ItemStackBuilder(cat.icon())
                .hideAll()
                .name(cat.displayNameComponent())
                .lore(List.of(Component.empty(), countLine))
                .build(),
            click -> new ItemLibraryCategoryMenu(swordPlayer, cat).open()
        );
    }
}
