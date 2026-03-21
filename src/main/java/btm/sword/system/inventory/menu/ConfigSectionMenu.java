package btm.sword.system.inventory.menu;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ConfigEntryItem;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged in-game editor for all {@link Config.ConfigEntry} values within a single config section.
 * <p>
 * Opened from {@link ConfigMenu} when the player selects a section. Each entry is
 * represented by a {@link ConfigEntryItem}. Left/right click adjusts numeric values
 * in-place; shift+left-click prompts the player to type a value in chat, then
 * reopens this menu automatically.
 * </p>
 */
public class ConfigSectionMenu extends Menu {

    private final String section;
    private final List<Config.ConfigEntry<?>> entries;
    private final String filter;

    /**
     * Constructs a section menu for the given config section.
     *
     * @param player  the viewing player
     * @param section the section identifier (first path segment, e.g. {@code "combat"})
     * @param entries the config entries belonging to this section
     */
    public ConfigSectionMenu(SwordPlayer player, String section, List<Config.ConfigEntry<?>> entries) {
        this(player, section, entries, null);
    }

    private ConfigSectionMenu(SwordPlayer player, String section, List<Config.ConfigEntry<?>> entries, String filter) {
        super(player);
        this.section = section;
        this.entries = entries;
        this.filter = filter;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        List<Config.ConfigEntry<?>> displayed = filter == null ? entries
            : entries.stream()
                .filter(e -> e.path.toLowerCase().contains(filter))
                .toList();

        List<Item> entryItems = displayed.stream()
            .map(entry -> (Item) new ConfigEntryItem(entry, swordPlayer, this::open))
            .toList();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back to Sections", NamedTextColor.GRAY))
                .build(),
            click -> new ConfigMenu(swordPlayer).open()
        );

        SimpleItem search = new SimpleItem(
            new ItemStackBuilder(filter != null ? Material.FILLED_MAP : Material.MAP)
                .name(filter != null
                    ? Component.text("Filter: " + filter, NamedTextColor.YELLOW)
                    : Component.text("Search Entries", NamedTextColor.GRAY))
                .lore(filter != null
                    ? List.of(Component.text("Shift-click to clear", NamedTextColor.DARK_GRAY))
                    : List.of(Component.text("Click to filter by path", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (click.getClickType().isShiftClick() && filter != null) {
                    new ConfigSectionMenu(swordPlayer, section, entries).open();
                    return;
                }
                ChatInputCapture.prompt(swordPlayer.player(),
                    Component.text("Filter entries (keyword or 'cancel'):", NamedTextColor.YELLOW),
                    input -> {
                        if (input.equalsIgnoreCase("cancel")) {
                            new ConfigSectionMenu(swordPlayer, section, entries, null).open();
                        } else {
                            new ConfigSectionMenu(swordPlayer, section, entries, input.toLowerCase()).open();
                        }
                    });
            }
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "# # # # Q # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "B # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('Q', search)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(entryItems)
            .build();

        String title = filter != null
            ? capitalize(section) + "  [" + filter + "]  (" + displayed.size() + "/" + entries.size() + ")"
            : capitalize(section) + " Config  (" + entries.size() + " entries)";
        Window window = Window.single()
            .setViewer(player)
            .setTitle(title)
            .setGui(gui)
            .build();

        window.open();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
