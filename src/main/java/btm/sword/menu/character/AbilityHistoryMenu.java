package btm.sword.menu.character;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.Material;

import btm.sword.action.skill.history.AbilityHistoryAction;
import btm.sword.action.skill.history.AbilityHistoryEntry;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.item.impl.SimpleItem;

/**
 * Paged menu displaying a player's ability event history — uses and surrenders.
 *
 * <p>Each entry shows the ability name, action type, and timestamp. Entries are
 * displayed newest-first. The menu is read-only; no interaction beyond navigation
 * and returning to the Character Menu is provided.
 *
 * <p><b>Layout</b> (6-row chest, 54 slots):</p>
 * <pre>
 * P # # # # # # # #
 * x x x x x x x x x
 * x x x x x x x x x
 * x x x x x x x x x
 * x x x x x x x x x
 * # # # # &lt; # &gt; # #
 * </pre>
 * {@code P} = back to Character Menu, {@code <}/{@code >} = page navigation.
 */
public class AbilityHistoryMenu extends Menu {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /** Constructs the ability history menu for the given player. */
    public AbilityHistoryMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
//        Player player = swordPlayer.player();
//        PlayerAbilityHistory history = swordPlayer.getAbilityHistory();
//
//        List<AbilityHistoryEntry> entries = history.entries();
//
//        List<Item> items = new ArrayList<>();
//        // Newest first
//        for (int i = entries.size() - 1; i >= 0; i--) {
//            items.add(buildEntryItem(entries.get(i)));
//        }
//
//        if (items.isEmpty()) {
//            items.add(new SimpleItem(
//                new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
//                    .name(Component.text("No ability events recorded yet.", NamedTextColor.GRAY))
//                    .build()
//            ));
//        }
//
//        SimpleItem back = new SimpleItem(
//            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
//                .name(Component.text("Back to Character Menu", NamedTextColor.GRAY))
//                .build(),
//            click -> new CharacterMenu(swordPlayer).open()
//        );
//
//        PagedGui<Item> gui = PagedGui.items()
//            .setStructure(
//                "P # # # # # # # #",
//                "x x x x x x x x x",
//                "x x x x x x x x x",
//                "x x x x x x x x x",
//                "x x x x x x x x x",
//                "# # # # < # > # #")
//            .addIngredient('#', BORDER)
//            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
//            .addIngredient('P', back)
//            .setContent(items)
//            .build();
//
//        Window window = Window.single()
//            .setViewer(player)
//            .setTitle("Ability History")
//            .setGui(gui)
//            .build();
//
//        window.open();
    }

    private SimpleItem buildEntryItem(AbilityHistoryEntry entry) {
        boolean surrendered = entry.action() == AbilityHistoryAction.SURRENDERED;

        Material icon = surrendered ? Material.BARRIER : Material.ECHO_SHARD;
        NamedTextColor actionColor = surrendered ? NamedTextColor.RED : NamedTextColor.AQUA;
        String actionLabel = surrendered ? "Surrendered" : "Used";

        String dateStr = DATE_FORMAT.format(new Date(entry.timestamp()));

        return new SimpleItem(
            new ItemStackBuilder(icon)
                .name(Component.text(entry.displayName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text(actionLabel, actionColor),
                    Component.text(dateStr, NamedTextColor.DARK_GRAY)
                ))
                .hideAll()
                .build()
        );
    }
}
