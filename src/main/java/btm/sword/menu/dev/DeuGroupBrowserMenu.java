package btm.sword.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import net.donnypz.displayentityutils.events.GroupSpawnedEvent;
import net.donnypz.displayentityutils.managers.DisplayGroupManager;
import net.donnypz.displayentityutils.managers.LoadMethod;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayEntityGroup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for all locally available DEU display entity groups ({@code .deg} files).
 * <p>
 * Groups are discovered by scanning {@code plugins/DisplayEntityUtils/groups/} for
 * {@code .deg} files. Clicking a group spawns it at the player's current location as real
 * world entities. Use the <em>Despawn All Displays</em> button in {@link DEUBDEMenu} to clean up.
 * </p>
 *
 * <h2>Layout</h2>
 * <pre>
 *   # # # # # # # # #
 *   x x x x x x x x x
 *   x x x x x x x x x
 *   x x x x x x x x x
 *   B # # &lt; . &gt; # # #
 * </pre>
 */
public class DeuGroupBrowserMenu extends Menu {

    // LoadMethod.LOCAL stores .deg files under savedentities/, not groups/
    private static final File GROUPS_DIR = new File("plugins/DisplayEntityUtils/savedentities");

    /**
     * Creates a DEU group browser for the given player.
     *
     * @param player the owning sword player
     */
    public DeuGroupBrowserMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        List<Item> groupItems = new ArrayList<>();
        File[] degFiles = GROUPS_DIR.exists()
            ? GROUPS_DIR.listFiles((dir, name) -> name.endsWith(".deg"))
            : null;

        if (degFiles != null) {
            for (File file : degFiles) {
                String tag = file.getName().replace(".deg", "");
                groupItems.add(new SimpleItem(
                    new ItemStackBuilder(Material.ITEM_FRAME)
                        .name(Component.text(tag, NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text("Click to spawn at your location", NamedTextColor.DARK_GRAY)))
                        .build(),
                    click -> spawnGroup(player, tag)
                ));
            }
        }

        if (groupItems.isEmpty()) {
            groupItems.add(new SimpleItem(
                new ItemStackBuilder(Material.BARRIER)
                    .name(Component.text("No groups found", NamedTextColor.RED))
                    .lore(List.of(
                        Component.text("Convert .zip packs in the Animations menu", NamedTextColor.DARK_GRAY)))
                    .build()
            ));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DEUBDEMenu(swordPlayer).open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "# # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "B # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(groupItems)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("DEU Groups  (" + groupItems.size() + " found)")
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Loads the given group tag from disk and spawns it at the player's current location.
     *
     * @param player the player requesting the spawn
     * @param tag    the DEU group tag (base name of the {@code .deg} file)
     */
    private static void spawnGroup(Player player, String tag) {
        DisplayEntityGroup group = DisplayGroupManager.getGroup(LoadMethod.LOCAL, tag);
        if (group == null) {
            player.sendMessage(Component.text("[Sword] Group not found: " + tag, NamedTextColor.RED));
            return;
        }
        player.closeInventory();
        group.spawn(player.getLocation(), GroupSpawnedEvent.SpawnReason.CUSTOM);
        player.sendMessage(Component.text("[Sword] Spawned group: " + tag, NamedTextColor.GREEN));
    }
}
