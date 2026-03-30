package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import btm.sword.utility.sound.SoundUtil;
import btm.sword.utility.sound.SwordSoundType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for selecting an enum value for a {@link Config.ConfigEntry}.
 * <p>
 * All constants of the entry's enum type are shown as individual items.
 * The currently selected value is highlighted with a green checkmark prefix.
 * Left-click selects a value and returns to the parent menu. For
 * {@link SwordSoundType} entries the browser first shows a prefix category page
 * (AMBIENT, BLOCK, ENTITY, …); right-clicking a sound item previews it.
 * </p>
 */
public class EnumSelectionMenu extends Menu {

    private final Config.ConfigEntry<?> entry;
    private final Runnable reopenParent;
    /** Non-null only when showing a filtered sub-list of SoundType values. */
    private final String soundPrefix;
    /** Non-null when a name filter is active. */
    private final String filter;

    /**
     * Opens the top-level browser for the entry's enum type. For {@link SwordSoundType}
     * this shows prefix categories; for all other enums it shows a flat value list.
     */
    public EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry, Runnable reopenParent) {
        this(player, entry, reopenParent, null, null);
    }

    private EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry,
                               Runnable reopenParent, String soundPrefix, String filter) {
        super(player);
        this.entry = entry;
        this.reopenParent = reopenParent;
        this.soundPrefix = soundPrefix;
        this.filter = filter;
    }

    @Override
    public void open() {
        if (entry.type() == SwordSoundType.class && soundPrefix == null) {
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
        Map<String, List<SwordSoundType>> byPrefix = new LinkedHashMap<>();
        for (SwordSoundType st : SwordSoundType.values()) {
            String prefix = prefixOf(st.name());
            byPrefix.computeIfAbsent(prefix, k -> new ArrayList<>()).add(st);
        }

        Map<String, List<SwordSoundType>> displayPrefixes = filter == null ? byPrefix
            : byPrefix.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(filter))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (a, b) -> a, LinkedHashMap::new));

        List<Item> prefixItems = new ArrayList<>();
        for (Map.Entry<String, List<SwordSoundType>> e : displayPrefixes.entrySet()) {
            String prefix = e.getKey();
            List<SwordSoundType> sounds = e.getValue();
            Material mat = soundPrefixMaterial(prefix);

            prefixItems.add(new SimpleItem(
                new ItemStackBuilder(mat)
                    .name(Component.text(prefix, NamedTextColor.GOLD))
                    .lore(List.of(Component.text(sounds.size() + " sounds", NamedTextColor.GRAY)))
                    .build(),
                click -> new EnumSelectionMenu(swordPlayer, entry, reopenParent, prefix, null).open()
            ));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> reopenParent.run()
        );

        SimpleItem search = new SimpleItem(
            new ItemStackBuilder(filter != null ? Material.FILLED_MAP : Material.MAP)
                .name(filter != null
                    ? Component.text("Filter: " + filter, NamedTextColor.YELLOW)
                    : Component.text("Search", NamedTextColor.GRAY))
                .lore(filter != null
                    ? List.of(Component.text("Shift-click to clear", NamedTextColor.DARK_GRAY))
                    : List.of(Component.text("Click to filter by name", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (click.getClickType().isShiftClick() && filter != null) {
                    new EnumSelectionMenu(swordPlayer, entry, reopenParent, null, null).open();
                    return;
                }
                ChatInputCapture.prompt(swordPlayer.player(),
                    Component.text("Filter categories (keyword or 'cancel'):", NamedTextColor.YELLOW),
                    input -> {
                        if (input.equalsIgnoreCase("cancel")) {
                            new EnumSelectionMenu(swordPlayer, entry, reopenParent, null, null).open();
                        } else {
                            new EnumSelectionMenu(swordPlayer, entry, reopenParent, null, input.toLowerCase()).open();
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
            .setContent(prefixItems)
            .build();

        String title = filter != null
            ? entry.path() + "  |  SoundType  [" + filter + "]  (" + displayPrefixes.size() + "/" + byPrefix.size() + ")"
            : entry.path() + "  |  SoundType — " + byPrefix.size() + " categories";

        Window.single()
            .setViewer(player)
            .setTitle(title)
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
        Class<?> type = entry.type();
        boolean isSoundType = type == SwordSoundType.class;

        // Determine currently stored value name for selection highlight
        String stored = config.getString(entry.path());
        String currentName = stored != null ? stored.toUpperCase()
            : (entry.defaultValue() instanceof Enum<?> e ? e.name() : "");

        // Filter constants: by sound prefix, then by item validity for Material, then by search filter.
        // For Material, use Registry.MATERIAL to exclude legacy (LEGACY_*) entries.
        List<Object> allNonLegacy = type == Material.class
            ? new ArrayList<>(Registry.MATERIAL.stream().toList())
            : Arrays.asList(type.getEnumConstants());
        List<Object> constants = allNonLegacy.stream()
            .filter(c -> soundPrefix == null || prefixOf(((Enum<?>) c).name()).equals(soundPrefix))
            .filter(c -> type != Material.class || (((Material) c).isItem() && !((Material) c).isAir()))
            .filter(c -> filter == null || ((Enum<?>) c).name().toLowerCase().contains(filter))
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

        SimpleItem search = new SimpleItem(
            new ItemStackBuilder(filter != null ? Material.FILLED_MAP : Material.MAP)
                .name(filter != null
                    ? Component.text("Filter: " + filter, NamedTextColor.YELLOW)
                    : Component.text("Search", NamedTextColor.GRAY))
                .lore(filter != null
                    ? List.of(Component.text("Shift-click to clear", NamedTextColor.DARK_GRAY))
                    : List.of(Component.text("Click to filter by name", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (click.getClickType().isShiftClick() && filter != null) {
                    new EnumSelectionMenu(swordPlayer, entry, reopenParent, soundPrefix, null).open();
                    return;
                }
                ChatInputCapture.prompt(swordPlayer.player(),
                    Component.text("Filter values (keyword or 'cancel'):", NamedTextColor.YELLOW),
                    input -> {
                        if (input.equalsIgnoreCase("cancel")) {
                            new EnumSelectionMenu(swordPlayer, entry, reopenParent, soundPrefix, null).open();
                        } else {
                            new EnumSelectionMenu(swordPlayer, entry, reopenParent, soundPrefix, input.toLowerCase()).open();
                        }
                    });
            }
        );

        int totalInScope = (int) allNonLegacy.stream()
            .filter(c -> soundPrefix == null || prefixOf(((Enum<?>) c).name()).equals(soundPrefix))
            .filter(c -> type != Material.class || (((Material) c).isItem() && !((Material) c).isAir()))
            .count();
        String title = filter != null
            ? entry.path() + "  |  " + (soundPrefix != null ? soundPrefix : type.getSimpleName())
                + "  [" + filter + "]  (" + constants.size() + "/" + totalInScope + ")"
            : soundPrefix != null
                ? entry.path() + "  |  " + soundPrefix + " (" + constants.size() + ")"
                : entry.path() + "  |  " + type.getSimpleName() + " (" + constants.size() + ")";

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
                Component name = selected
                    ? Component.text("✔ ", NamedTextColor.GREEN).append(Component.text(value.name(), NamedTextColor.WHITE))
                    : Component.text(value.name(), NamedTextColor.GRAY);
                List<Component> lore = new java.util.ArrayList<>();
                if (selected) {
                    lore.add(Component.text("(currently selected)", NamedTextColor.GREEN)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC));
                }
                if (isSoundType) {
                    lore.add(Component.text("L-click: select", NamedTextColor.DARK_GRAY));
                    lore.add(Component.text("R-click: preview", NamedTextColor.DARK_GRAY));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.DARK_GRAY));
                }
                return new ItemWrapper(new ItemStackBuilder(mat).name(name).lore(lore).build());
            }

            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                if (isSoundType && clickType == ClickType.RIGHT) {
                    SoundUtil.playSound(p, (SwordSoundType) value, 1.0f, 1.0f);
                    return;
                }
                ConfigManager.getInstance().setValue((Config.ConfigEntry) entry, value);
                swordPlayer.message(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(entry.path(), NamedTextColor.WHITE))
                    .append(Component.text(" = ", NamedTextColor.GREEN))
                    .append(Component.text(value.name(), NamedTextColor.YELLOW)));
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
     * {@link Material} constants use themselves; {@link SwordSoundType} dispatches to
     * {@link #soundTypeMaterial(SwordSoundType)} for prefix-aware matching; {@link Particle}
     * constants use {@link #particleMaterial(Particle)} for keyword-based matching;
     * everything else uses paper.
     */
    private static Material resolveItemMaterial(Enum<?> value) {
        if (value instanceof Material m) return m.isItem() && !m.isAir() ? m : Material.PAPER;
        if (value instanceof SwordSoundType st) return soundTypeMaterial(st);
        if (value instanceof Particle p) return particleMaterial(p);
        return Material.PAPER;
    }

    /**
     * Maps a {@link Particle} constant to a representative display {@link Material} by
     * inspecting its name for common keywords.
     */
    private static Material particleMaterial(Particle particle) {
        String name = particle.name();
        if (name.contains("FLAME") || name.contains("FIRE")) return Material.FLINT_AND_STEEL;
        if (name.contains("SMOKE") || name.contains("CLOUD") || name.contains("POOF")) return Material.COAL;
        if (name.contains("CRIT") || name.contains("ENCHANTED_HIT")) return Material.DIAMOND_SWORD;
        if (name.contains("ENCHANT")) return Material.ENCHANTED_BOOK;
        if (name.contains("BLOCK") || name.contains("DUST_PILLAR")) return Material.STONE;
        if (name.contains("DUST") || name.contains("REDSTONE")) return Material.REDSTONE;
        if (name.contains("DRIP") || name.contains("FALLING")) return Material.WATER_BUCKET;
        if (name.contains("LAVA")) return Material.LAVA_BUCKET;
        if (name.contains("SOUL")) return Material.SOUL_SAND;
        if (name.contains("GUST") || name.contains("WIND")) return Material.FEATHER;
        if (name.contains("PORTAL")) return Material.ENDER_EYE;
        if (name.contains("HEART")) return Material.PINK_DYE;
        if (name.contains("VILLAGER")) return Material.EMERALD;
        if (name.contains("TRIAL")) return Material.TRIAL_KEY;
        if (name.contains("SONIC")) return Material.SCULK_SENSOR;
        if (name.contains("ELECTRIC") || name.contains("SPARK")) return Material.LIGHTNING_ROD;
        if (name.contains("SPORE")) return Material.SPORE_BLOSSOM;
        if (name.contains("CHERRY")) return Material.CHERRY_LEAVES;
        return Material.FIREWORK_STAR;
    }

    /**
     * Resolves a display {@link Material} for a {@link SwordSoundType} constant by inspecting
     * its name prefix. BLOCK_ sounds attempt to match a block material, ENTITY_ sounds
     * attempt a spawn egg, AMBIENT_ sounds map to biome-representative blocks.
     */
    private static Material soundTypeMaterial(SwordSoundType st) {
        String name = st.name();
        if (name.startsWith("BLOCK_")) return blockSoundMaterial(name);
        if (name.startsWith("ENTITY_")) return entitySoundMaterial(name);
        if (name.startsWith("AMBIENT_")) return ambientSoundMaterial(name);
        if (name.startsWith("ITEM_")) return itemSoundMaterial(name);
        return Material.NOTE_BLOCK;
    }

    /**
     * Tries to match a BLOCK_ sound name to a real {@link Material} by progressively
     * stripping underscore-delimited segments from the right until a valid obtainable item is found.
     * Requires {@link Material#isItem()} to avoid non-obtainable blocks like BAMBOO_SAPLING.
     */
    private static Material blockSoundMaterial(String name) {
        String rest = name.substring("BLOCK_".length());
        String[] parts = rest.split("_");
        for (int len = parts.length - 1; len >= 1; len--) {
            String candidate = String.join("_", java.util.Arrays.copyOf(parts, len));
            try {
                Material mat = Material.valueOf(candidate);
                if (!mat.isAir() && mat.isItem()) return mat;
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.STONE;
    }

    /**
     * Tries to match an ITEM_ sound name to a real item {@link Material} by progressively
     * stripping underscore-delimited segments from the right.
     */
    private static Material itemSoundMaterial(String name) {
        String rest = name.substring("ITEM_".length());
        String[] parts = rest.split("_");
        for (int len = parts.length - 1; len >= 1; len--) {
            String candidate = String.join("_", java.util.Arrays.copyOf(parts, len));
            try {
                Material mat = Material.valueOf(candidate);
                if (mat.isItem() && !mat.isAir()) return mat;
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.CHEST;
    }

    /**
     * Tries to match an ENTITY_ sound name to a spawn egg by progressively stripping
     * trailing segments and appending {@code _SPAWN_EGG}.
     */
    private static Material entitySoundMaterial(String name) {
        String rest = name.substring("ENTITY_".length());
        String[] parts = rest.split("_");
        for (int len = parts.length - 1; len >= 1; len--) {
            String candidate = String.join("_", java.util.Arrays.copyOf(parts, len)) + "_SPAWN_EGG";
            try {
                return Material.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.SKELETON_SKULL;
    }

    /**
     * Maps an AMBIENT_ sound name to a biome-representative block material based on
     * keyword matching.
     */
    private static Material ambientSoundMaterial(String name) {
        if (name.contains("UNDERWATER")) return Material.WATER_BUCKET;
        if (name.contains("BASALT_DELTAS")) return Material.BASALT;
        if (name.contains("CRIMSON")) return Material.CRIMSON_NYLIUM;
        if (name.contains("SOUL_SAND_VALLEY")) return Material.SOUL_SAND;
        if (name.contains("WARPED_FOREST")) return Material.WARPED_NYLIUM;
        if (name.contains("NETHER_WASTES")) return Material.NETHERRACK;
        if (name.contains("CAVE")) return Material.MOSSY_COBBLESTONE;
        return Material.FERN;
    }
}
