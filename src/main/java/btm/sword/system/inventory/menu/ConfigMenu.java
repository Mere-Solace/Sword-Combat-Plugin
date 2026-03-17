package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Config section browser. Groups all {@link Config.ConfigEntry} values by their
 * top-level YAML key (e.g. {@code "combat"}, {@code "physics"}) and presents one
 * button per section. Clicking a section opens a {@link ConfigSectionMenu} for that group.
 *
 * <h3>Save buttons</h3>
 * <ul>
 *   <li><b>Save to Server</b> — writes the in-memory config to
 *       {@code plugins/sword/config.yaml} on the running server.</li>
 *   <li><b>Save to Project</b> — writes the in-memory config to
 *       {@code src/main/resources/config.yaml} in the project source tree.
 *       Only works when running via {@code ./gradlew runServer}.</li>
 * </ul>
 */
public class ConfigMenu extends Menu {

    /** Material assigned to each config section button — keyed by first YAML path segment. */
    private static final Map<String, Material> SECTION_MATERIALS;
    static {
        SECTION_MATERIALS = new LinkedHashMap<>(); // TODO: Config these Materials! (And add the unregistered ones)
        SECTION_MATERIALS.put("umbralblade",       Material.HEAVY_CORE);
        SECTION_MATERIALS.put("hostile",       Material.REDSTONE);
        SECTION_MATERIALS.put("grab",     Material.RABBIT_FOOT);
        SECTION_MATERIALS.put("debug",      Material.ENDER_EYE);
        SECTION_MATERIALS.put("world",      Material.WATER_BUCKET);
        SECTION_MATERIALS.put("movement",     Material.FEATHER);
        SECTION_MATERIALS.put("entity",   Material.YELLOW_DYE);
        SECTION_MATERIALS.put("audio",       Material.NAUTILUS_SHELL);
        SECTION_MATERIALS.put("detection",      Material.SPYGLASS);
        SECTION_MATERIALS.put("display",    Material.CYAN_DYE);
        SECTION_MATERIALS.put("timing",       Material.CLOCK);
        SECTION_MATERIALS.put("combat",       Material.STONE_SWORD);
        SECTION_MATERIALS.put("physics",        Material.COMPARATOR);
        SECTION_MATERIALS.put("angle",     Material.REPEATER);
        SECTION_MATERIALS.put("color", Material.ORANGE_DYE);
    }

    public ConfigMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();

        // Group entries by section (first path segment before the first '.')
        Map<String, List<Config.ConfigEntry<?>>> sections = new LinkedHashMap<>();
        for (Config.ConfigEntry<?> entry : Config.ENTRIES) {
            String section = entry.path.contains(".")
                ? entry.path.substring(0, entry.path.indexOf('.'))
                : entry.path;
            sections.computeIfAbsent(section, k -> new ArrayList<>()).add(entry);
        }

        // Build one section button per group
        List<Item> sectionItems = new ArrayList<>();
        for (Map.Entry<String, List<Config.ConfigEntry<?>>> e : sections.entrySet()) {
            String section = e.getKey();
            List<Config.ConfigEntry<?>> entries = e.getValue();
            Material mat = SECTION_MATERIALS.getOrDefault(section.toLowerCase(), Material.PAPER);

            sectionItems.add(new SimpleItem(
                new ItemStackBuilder(mat)
                    .name(Component.text(capitalize(section), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .lore(List.of(Component.text(entries.size() + " entries", NamedTextColor.GRAY)))
                    .build(),
                click -> new ConfigSectionMenu(swordPlayer, section, entries).open()
            ));
        }

        SimpleItem saveServer = new SimpleItem(
            new ItemStackBuilder(Material.NETHER_STAR)
                .name(Component.text("Save to Server", NamedTextColor.GREEN, TextDecoration.BOLD))
                .build(),
            click -> {
                ConfigManager.getInstance().saveConfig();
                swordPlayer.message("Config saved to server.");
            }
        );

        SimpleItem saveProject = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Save to Project", NamedTextColor.AQUA, TextDecoration.BOLD))
                .build(),
            click -> {
                ConfigManager.getInstance().saveToProject();
                swordPlayer.message("Config saved to project source.");
            }
        );

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back to Dev Menu", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "# # # S # X # # #",
                "# # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "B # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('S', saveServer)
            .addIngredient('X', saveProject)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(sectionItems)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Config Sections  (" + sections.size() + " sections)")
            .setGui(gui)
            .build();

        window.open();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
