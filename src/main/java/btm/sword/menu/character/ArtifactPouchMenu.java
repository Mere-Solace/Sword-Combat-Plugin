package btm.sword.menu.character;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.section.ColorConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Pouch menu displaying the player's held quest/artifact items.
 *
 * <p><b>Layout</b> (3-row chest, 27 slots):</p>
 * <pre>
 * # # # # # # # # #
 * &lt; . . . P . . . #
 * # # # # # # # # #
 * </pre>
 *
 * <ul>
 *   <li>{@code P} — placeholder; shows a message when no quest item types are defined.
 *       Will be replaced with per-type slots as {@link btm.sword.item.quest.QuestItemType}
 *       entries are added.
 *   <li>{@code &lt;} — back navigation.
 * </ul>
 */
public class ArtifactPouchMenu extends Menu {

    /** Constructs the artifact pouch menu for the given player. */
    public ArtifactPouchMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        SimpleItem placeholder = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .hideAll()
                .name(Component.text("Quest Items", ColorConfig.TEXT_COOL, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                    Component.empty(),
                    Component.text("No quest items yet.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Quest and artifact types will appear here", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("as content is developed.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                ))
                .build()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "# # # # # # # # #",
                "< . . . P . . . #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('<', generatePreviousButtonOrDefault())
            .addIngredient('P', placeholder)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Artifact Pouch")
            .setGui(gui)
            .build()
            .open();
    }
}
