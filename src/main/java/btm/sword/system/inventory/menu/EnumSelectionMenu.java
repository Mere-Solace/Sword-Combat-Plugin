package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.sound.SoundType;
import btm.sword.utility.sound.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for selecting an enum value for a {@link Config.ConfigEntry}.
 * <p>
 * All constants of the entry's enum type are shown as individual items.
 * The currently selected value is highlighted with a green checkmark prefix.
 * Left-click selects a value and returns to the parent menu. For
 * {@link SoundType} entries the browser first shows a prefix category page
 * (AMBIENT, BLOCK, ENTITY, …); right-clicking a sound item previews it.
 * </p>
 */
public class EnumSelectionMenu extends Menu {

    private final Config.ConfigEntry<?> entry;
    private final Runnable reopenParent;
    /** Non-null only when showing a filtered sub-list of SoundType values. */
    private final String soundPrefix;

    /**
     * Opens the top-level browser for the entry's enum type. For {@link SoundType}
     * this shows prefix categories; for all other enums it shows a flat value list.
     */
    public EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry, Runnable reopenParent) {
        this(player, entry, reopenParent, null);
    }

    private EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry,
                               Runnable reopenParent, String soundPrefix) {
        super(player);
        this.entry = entry;
        this.reopenParent = reopenParent;
        this.soundPrefix = soundPrefix;
    }

    @Override
    public void open() {
        if (entry.type == SoundType.class && soundPrefix == null) {
            openSoundPrefixBrowser();
        } else {
            openValueList();
        }
    }

    // -------------------------------------------------------------------------
    //  Sound prefix browser
    // -------------------------------------------------------------------------

    private void openSoundPrefixBrowser() {
        Player player = swordPlayer.player();

        // Group SoundType constants by their first underscore-delimited segment
        Map<String, List<SoundType>> byPrefix = new LinkedHashMap<>();
        for (SoundType st : SoundType.values()) {
            String prefix = prefixOf(st.name());
            byPrefix.computeIfAbsent(prefix, k -> new ArrayList<>()).add(st);
        }

        List<Item> prefixItems = new ArrayList<>();
        for (Map.Entry<String, List<SoundType>> e : byPrefix.entrySet()) {
            String prefix = e.getKey();
            List<SoundType> sounds = e.getValue();
            Material mat = soundPrefixMaterial(prefix);

            prefixItems.add(new SimpleItem(
                new ItemStackBuilder(mat)
                    .name(Component.text(prefix, NamedTextColor.GOLD))
                    .lore(List.of(Component.text(sounds.size() + " sounds", NamedTextColor.GRAY)))
                    .build(),
                click -> new EnumSelectionMenu(swordPlayer, entry, reopenParent, prefix).open()
            ));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> reopenParent.run()
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "# # # B # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(prefixItems)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("SoundType — " + byPrefix.size() + " categories")
            .setGui(gui)
            .build()
            .open();
    }

    // -------------------------------------------------------------------------
    //  Flat value list (all enums, or filtered SoundType by prefix)
    // -------------------------------------------------------------------------

    private void openValueList() {
        Player player = swordPlayer.player();
        FileConfiguration config = ConfigManager.getInstance().getConfig();
        Class<?> type = entry.type;
        boolean isSoundType = type == SoundType.class;

        // Determine currently stored value name for selection highlight
        String stored = config.getString(entry.path);
        String currentName = stored != null ? stored.toUpperCase()
            : (entry.defaultValue instanceof Enum<?> e ? e.name() : "");

        // Filter constants when a sound prefix is active
        Object[] allConstants = type.getEnumConstants();
        List<Object> constants = soundPrefix == null ? Arrays.asList(allConstants)
            : Arrays.stream(allConstants)
                .filter(c -> prefixOf(((Enum<?>) c).name()).equals(soundPrefix))
                .toList();

        List<Item> items = new ArrayList<>(constants.size());
        for (Object constant : constants) {
            Enum<?> value = (Enum<?>) constant;
            boolean selected = value.name().equalsIgnoreCase(currentName);
            items.add(buildValueItem(value, selected, isSoundType));
        }

        // Back: go to prefix browser for SoundType, otherwise to parent
        Runnable goBack = isSoundType
            ? () -> new EnumSelectionMenu(swordPlayer, entry, reopenParent).open()
            : reopenParent;

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> goBack.run()
        );

        String title = soundPrefix != null
            ? soundPrefix + " sounds  (" + constants.size() + ")"
            : type.getSimpleName() + "  (" + constants.size() + " values)";

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "# # # B # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('B', back)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(items)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle(title)
            .setGui(gui)
            .build()
            .open();
    }

    // -------------------------------------------------------------------------
    //  Item builder per enum constant
    // -------------------------------------------------------------------------

    private Item buildValueItem(Enum<?> value, boolean selected, boolean isSoundType) {
        Material mat = resolveItemMaterial(value);

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                String displayName = selected ? "§a✔ §f" + value.name() : "§7" + value.name();
                ItemBuilder builder = new ItemBuilder(mat).setDisplayName(displayName);
                if (selected) builder.addLoreLines("§a§o(currently selected)");
                if (isSoundType) {
                    builder.addLoreLines("§8L-click: select", "§8R-click: preview");
                } else {
                    builder.addLoreLines("§8Click to select");
                }
                return builder;
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public void handleClick(ClickType clickType, Player p, InventoryClickEvent event) {
                if (isSoundType && clickType == ClickType.RIGHT) {
                    SoundUtil.playSound(p, (SoundType) value, 1.0f, 1.0f);
                    return;
                }
                ConfigManager.getInstance().setValue((Config.ConfigEntry) entry, value);
                swordPlayer.message("§aSet §f" + entry.path + " §a= §e" + value.name());
                reopenParent.run();
            }
        };
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /** Returns the first underscore-delimited segment of an enum constant name. */
    private static String prefixOf(String name) {
        int idx = name.indexOf('_');
        return idx < 0 ? name : name.substring(0, idx);
    }

    /**
     * Maps a SoundType prefix to a representative display material.
     * Falls back to {@link Material#NOTE_BLOCK} for unknown prefixes.
     */
    private static Material soundPrefixMaterial(String prefix) {
        return switch (prefix.toUpperCase()) {
            case "AMBIENT"  -> Material.FERN;
            case "BLOCK"    -> Material.STONE;
            case "ENTITY"   -> Material.SKELETON_SKULL;
            case "MUSIC"    -> Material.JUKEBOX;
            case "WEATHER"  -> Material.WATER_BUCKET;
            case "UI"       -> Material.PAPER;
            case "ITEM"     -> Material.CHEST;
            case "EVENT"    -> Material.BEACON;
            case "RANDOM"   -> Material.COMPASS;
            default         -> Material.NOTE_BLOCK;
        };
    }

    /**
     * Picks the display material for a given enum constant.
     * {@link Material} constants use themselves; {@link SoundType} uses a note block;
     * everything else uses paper.
     */
    private static Material resolveItemMaterial(Enum<?> value) {
        if (value instanceof Material m) return m;
        if (value instanceof SoundType) return Material.NOTE_BLOCK;
        return Material.PAPER;
    }
}
