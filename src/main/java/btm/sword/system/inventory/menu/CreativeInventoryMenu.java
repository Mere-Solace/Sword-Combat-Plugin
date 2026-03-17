package btm.sword.system.inventory.menu;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import btm.sword.utility.Prefab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged creative inventory browser. Displays every obtainable item material
 * alphabetically — clicking any entry gives the player a full stack of that item.
 * A search button at the top filters results by name substring.
 * Accessible from the {@link DevMenu}.
 */
public class CreativeInventoryMenu extends Menu {

    private final String query;

    /** Opens the full unfiltered creative inventory. */
    public CreativeInventoryMenu(SwordPlayer player) {
        this(player, null);
    }

    /** Opens with an active search filter. {@code null} or blank shows all items. */
    public CreativeInventoryMenu(SwordPlayer player, String query) {
        super(player);
        this.query = (query != null && !query.isBlank()) ? query.toLowerCase() : null;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        List<Item> items = Arrays.stream(Prefab.MODERN_MATERIALS)
            .filter(m -> query == null || m.name().toLowerCase().contains(query))
            .map(m -> (Item) new SimpleItem(
                new ItemStackBuilder(m)
                    .name(Component.text(formatName(m), NamedTextColor.WHITE))
                    .build(),
                click -> click.getPlayer().getInventory().addItem(new ItemStack(m, m.getMaxStackSize()))
            ))
            .collect(Collectors.toList());

        SimpleItem searchButton = new SimpleItem(
            new ItemStackBuilder(Material.SPYGLASS)
                .name(query == null
                    ? Component.text("Search...", NamedTextColor.GRAY, TextDecoration.ITALIC)
                    : Component.text("Search: ", NamedTextColor.GRAY)
                        .append(Component.text(query, NamedTextColor.YELLOW, TextDecoration.ITALIC)))
                .lore(List.of(Component.text("Click to filter by name", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> ChatInputCapture.prompt(
                click.getPlayer(),
                Component.text("Enter search term (blank to clear):", NamedTextColor.YELLOW),
                input -> new CreativeInventoryMenu(swordPlayer, input.isBlank() ? null : input).open()
            )
        );

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back to Dev Menu", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
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
            .addIngredient('Q', searchButton)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(items)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle(query == null ? "Creative Inventory" : "Search: " + query)
            .setGui(gui)
            .build();

        window.open();
    }

    private static String formatName(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
