package btm.sword.menu.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import btm.sword.config.Config;
import btm.sword.config.ConfigManager;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import btm.sword.util.misc.ChatInputCapture;
import btm.sword.util.sound.SoundUtil;
import btm.sword.util.sound.SwordSoundType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for selecting an enum value.
 *
 * <p>Two entry points:</p>
 * <ul>
 *   <li>{@link #EnumSelectionMenu(SwordPlayer, Config.ConfigEntry, Runnable)} — adapter
 *       for {@link Config.ConfigEntry}. Picks a value and writes it through
 *       {@link ConfigManager#setValue}. Used by the config GUI.</li>
 *   <li>{@link #forEnum(SwordPlayer, Class, String, Supplier, Consumer, Runnable,
 *       EnumPickerOptions)} — generic factory for any enum. Caller supplies current
 *       value, commit callback, and optional presentation hooks.</li>
 * </ul>
 *
 * <p>When {@link EnumPickerOptions#groupKey()} is non-null (or the legacy path is
 * picking a {@link SwordSoundType}) a two-level browser is shown: group index first,
 * then the members of the chosen group. Right-clicking a value invokes
 * {@link EnumPickerOptions#onPreview()} if present.</p>
 */
public class EnumSelectionMenu extends Menu {

    private final PickContract contract;
    /** Non-null only when showing a filtered sub-list of one group. */
    private final String groupFilter;
    /** Non-null when a name filter is active. */
    private final String nameFilter;

    /**
     * Adapter constructor: wraps a {@link Config.ConfigEntry} in a {@link PickContract}
     * and opens the picker. Used by the in-game config GUI.
     */
    public EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry, Runnable reopenParent) {
        this(player, contractFromConfigEntry(player, entry, reopenParent), null, null);
    }

    private EnumSelectionMenu(SwordPlayer player, PickContract contract,
                              @Nullable String groupFilter, @Nullable String nameFilter) {
        super(player);
        this.contract = contract;
        this.groupFilter = groupFilter;
        this.nameFilter = nameFilter;
    }

    /**
     * Opens an enum picker for any enum type. All presentation hooks are
     * carried by {@code options}; pass {@link EnumPickerOptions#none()} for defaults.
     *
     * @param player       the player viewing the menu
     * @param enumType     the enum class being picked
     * @param titlePrefix  header shown in the window title
     * @param current      supplies the currently-selected constant (highlighted with a check)
     * @param onPick       invoked on selection; runs before {@code reopenParent}
     * @param reopenParent invoked to return to the parent menu after pick/back
     * @param options      optional presentation hooks; {@code null} treated as defaults
     * @param <E>          the enum type
     */
    public static <E extends Enum<E>> EnumSelectionMenu forEnum(
            SwordPlayer player,
            Class<E> enumType,
            String titlePrefix,
            Supplier<E> current,
            Consumer<E> onPick,
            Runnable reopenParent,
            @Nullable EnumPickerOptions<E> options) {
        PickContract c = contractFromGeneric(enumType, titlePrefix, current, onPick, reopenParent,
            options != null ? options : EnumPickerOptions.none());
        return new EnumSelectionMenu(player, c, null, null);
    }

    @Override
    public void open() {
        if (contract.groupKey != null && groupFilter == null) {
            openGroupBrowser();
        } else {
            openValueList();
        }
    }

    // -------------------------------------------------------------------------
    //  Group (top-level) browser
    // -------------------------------------------------------------------------

    private void openGroupBrowser() {
        Player player = swordPlayer.player();

        Map<String, List<Enum<?>>> byGroup = new LinkedHashMap<>();
        for (Enum<?> c : contract.values.get()) {
            if (contract.filter != null && !contract.filter.test(c)) continue;
            String g = contract.groupKey.apply(c);
            byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
        }

        Map<String, List<Enum<?>>> displayGroups = nameFilter == null ? byGroup
            : byGroup.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(nameFilter))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                    (a, b) -> a, LinkedHashMap::new));

        List<Item> groupItems = new ArrayList<>();
        for (Map.Entry<String, List<Enum<?>>> e : displayGroups.entrySet()) {
            String groupName = e.getKey();
            List<Enum<?>> members = e.getValue();
            Material mat = contract.groupMaterial != null
                ? contract.groupMaterial.apply(groupName) : Material.NOTE_BLOCK;

            groupItems.add(new SimpleItem(
                new ItemStackBuilder(mat)
                    .name(Component.text(groupName, NamedTextColor.GOLD))
                    .lore(List.of(Component.text(members.size() + " items", NamedTextColor.GRAY)))
                    .build(),
                click -> new EnumSelectionMenu(swordPlayer, contract, groupName, null).open()
            ));
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> contract.reopenParent.run()
        );

        SimpleItem search = buildSearchItem(() -> new EnumSelectionMenu(swordPlayer, contract, null, null).open(),
            input -> new EnumSelectionMenu(swordPlayer, contract, null, input.toLowerCase()).open(),
            "Filter categories (keyword or 'cancel'):");

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
            .setContent(groupItems)
            .build();

        String title = nameFilter != null
            ? contract.titlePrefix + "  [" + nameFilter + "]  ("
                + displayGroups.size() + "/" + byGroup.size() + ")"
            : contract.titlePrefix + " — " + byGroup.size() + " categories";

        Window.single()
            .setViewer(player)
            .setTitle(title)
            .setGui(gui)
            .build()
            .open();
    }

    // -------------------------------------------------------------------------
    //  Flat value list (all constants, or filtered by a group)
    // -------------------------------------------------------------------------

    private void openValueList() {
        Player player = swordPlayer.player();
        Enum<?> currentValue = contract.current.get();
        boolean hasPreview = contract.onPreview != null;

        List<? extends Enum<?>> constants = contract.values.get().stream()
            .filter(c -> contract.filter == null || contract.filter.test(c))
            .filter(c -> groupFilter == null
                || (contract.groupKey != null && contract.groupKey.apply(c).equals(groupFilter)))
            .filter(c -> nameFilter == null || c.name().toLowerCase().contains(nameFilter))
            .toList();

        List<Item> items = new ArrayList<>(constants.size());
        for (Enum<?> value : constants) {
            boolean selected = currentValue != null && value == currentValue;
            items.add(buildValueItem(value, selected, hasPreview));
        }

        Runnable goBack = contract.groupKey != null
            ? () -> new EnumSelectionMenu(swordPlayer, contract, null, null).open()
            : contract.reopenParent;

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> goBack.run()
        );

        SimpleItem search = buildSearchItem(
            () -> new EnumSelectionMenu(swordPlayer, contract, groupFilter, null).open(),
            input -> new EnumSelectionMenu(swordPlayer, contract, groupFilter, input.toLowerCase()).open(),
            "Filter values (keyword or 'cancel'):");

        int totalInScope = (int) contract.values.get().stream()
            .filter(c -> contract.filter == null || contract.filter.test(c))
            .filter(c -> groupFilter == null
                || (contract.groupKey != null && contract.groupKey.apply(c).equals(groupFilter)))
            .count();
        String scopeLabel = groupFilter != null ? groupFilter : contract.enumType.getSimpleName();
        String title = nameFilter != null
            ? contract.titlePrefix + "  |  " + scopeLabel + "  [" + nameFilter + "]  ("
                + constants.size() + "/" + totalInScope + ")"
            : contract.titlePrefix + "  |  " + scopeLabel + " (" + constants.size() + ")";

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
    //  Item builders & helpers
    // -------------------------------------------------------------------------

    private Item buildValueItem(Enum<?> value, boolean selected, boolean hasPreview) {
        Material mat = contract.material != null ? contract.material.apply(value)
            : resolveDefaultMaterial(value);
        String labelText = contract.label != null ? contract.label.apply(value) : value.name();

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                Component name = selected
                    ? Component.text("\u2714 ", NamedTextColor.GREEN)
                        .append(Component.text(labelText, NamedTextColor.WHITE))
                    : Component.text(labelText, NamedTextColor.GRAY);
                List<Component> lore = new ArrayList<>();
                if (selected) {
                    lore.add(Component.text("(currently selected)", NamedTextColor.GREEN)
                        .decorate(TextDecoration.ITALIC));
                }
                if (hasPreview) {
                    lore.add(Component.text("L-click: select", NamedTextColor.DARK_GRAY));
                    lore.add(Component.text("R-click: preview", NamedTextColor.DARK_GRAY));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.DARK_GRAY));
                }
                return new ItemWrapper(new ItemStackBuilder(mat).name(name).lore(lore).build());
            }

            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player p,
                                    @NotNull InventoryClickEvent event) {
                if (hasPreview && clickType == ClickType.RIGHT) {
                    contract.onPreview.accept(value);
                    return;
                }
                contract.onPick.accept(value);
                contract.reopenParent.run();
            }
        };
    }

    private SimpleItem buildSearchItem(Runnable onClear, Consumer<String> onInput, String prompt) {
        return new SimpleItem(
            new ItemStackBuilder(nameFilter != null ? Material.FILLED_MAP : Material.MAP)
                .name(nameFilter != null
                    ? Component.text("Filter: " + nameFilter, NamedTextColor.YELLOW)
                    : Component.text("Search", NamedTextColor.GRAY))
                .lore(nameFilter != null
                    ? List.of(Component.text("Shift-click to clear", NamedTextColor.DARK_GRAY))
                    : List.of(Component.text("Click to filter by name", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (click.getClickType().isShiftClick() && nameFilter != null) {
                    onClear.run();
                    return;
                }
                ChatInputCapture.prompt(swordPlayer.player(),
                    Component.text(prompt, NamedTextColor.YELLOW),
                    input -> {
                        if (input.equalsIgnoreCase("cancel")) onClear.run();
                        else onInput.accept(input);
                    });
            }
        );
    }

    // -------------------------------------------------------------------------
    //  PickContract — internal abstraction over both entry points
    // -------------------------------------------------------------------------

    private static final class PickContract {
        final Class<? extends Enum<?>> enumType;
        final String titlePrefix;
        final Supplier<? extends Enum<?>> current;
        final Consumer<Enum<?>> onPick;
        final Runnable reopenParent;
        final Supplier<List<? extends Enum<?>>> values;
        @Nullable final Predicate<Enum<?>> filter;
        @Nullable final Function<Enum<?>, String> label;
        @Nullable final Consumer<Enum<?>> onPreview;
        @Nullable final Function<Enum<?>, String> groupKey;
        @Nullable final Function<String, Material> groupMaterial;
        @Nullable final Function<Enum<?>, Material> material;

        PickContract(Class<? extends Enum<?>> enumType, String titlePrefix,
                     Supplier<? extends Enum<?>> current, Consumer<Enum<?>> onPick,
                     Runnable reopenParent, Supplier<List<? extends Enum<?>>> values,
                     @Nullable Predicate<Enum<?>> filter,
                     @Nullable Function<Enum<?>, String> label,
                     @Nullable Consumer<Enum<?>> onPreview,
                     @Nullable Function<Enum<?>, String> groupKey,
                     @Nullable Function<String, Material> groupMaterial,
                     @Nullable Function<Enum<?>, Material> material) {
            this.enumType = enumType;
            this.titlePrefix = titlePrefix;
            this.current = current;
            this.onPick = onPick;
            this.reopenParent = reopenParent;
            this.values = values;
            this.filter = filter;
            this.label = label;
            this.onPreview = onPreview;
            this.groupKey = groupKey;
            this.groupMaterial = groupMaterial;
            this.material = material;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static PickContract contractFromConfigEntry(SwordPlayer player,
                                                        Config.ConfigEntry<?> entry,
                                                        Runnable reopenParent) {
        Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) entry.type();
        boolean isMaterial = type == Material.class;
        boolean isSoundType = type == SwordSoundType.class;

        Supplier<List<? extends Enum<?>>> values = isMaterial
            ? () -> Registry.MATERIAL.stream().toList()
            : () -> Arrays.asList(type.getEnumConstants());

        Predicate<Enum<?>> filter = isMaterial
            ? e -> ((Material) e).isItem() && !((Material) e).isAir()
            : null;

        Supplier<Enum<?>> current = () -> {
            FileConfiguration config = ConfigManager.getInstance().getConfig();
            String stored = config.getString(entry.path());
            String name = stored != null ? stored.toUpperCase()
                : (entry.defaultValue() instanceof Enum<?> e ? e.name() : null);
            if (name == null) return null;
            try {
                return Enum.valueOf((Class) type, name);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        };

        Consumer<Enum<?>> onPick = value -> {
            ConfigManager.getInstance().setValue((Config.ConfigEntry) entry, value);
            player.message(Component.text("Set ", NamedTextColor.GREEN)
                .append(Component.text(entry.path(), NamedTextColor.WHITE))
                .append(Component.text(" = ", NamedTextColor.GREEN))
                .append(Component.text(value.name(), NamedTextColor.YELLOW)));
        };

        Consumer<Enum<?>> onPreview = isSoundType
            ? v -> SoundUtil.playSound(player.player(), (SwordSoundType) v, 1.0f, 1.0f)
            : null;
        Function<Enum<?>, String> groupKey = isSoundType
            ? v -> prefixOf(v.name())
            : null;
        Function<String, Material> groupMaterial = isSoundType
            ? EnumSelectionMenu::soundPrefixMaterial
            : null;

        return new PickContract(type, entry.path(), current, onPick, reopenParent, values,
            filter, null, onPreview, groupKey, groupMaterial,
            EnumSelectionMenu::resolveDefaultMaterial);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Enum<E>> PickContract contractFromGeneric(
            Class<E> enumType, String titlePrefix,
            Supplier<E> current, Consumer<E> onPick, Runnable reopenParent,
            EnumPickerOptions<E> options) {

        Supplier<List<? extends Enum<?>>> values =
            () -> Arrays.asList(enumType.getEnumConstants());

        Predicate<Enum<?>> filter = options.filter() == null ? null
            : e -> ((Predicate) options.filter()).test(e);
        Function<Enum<?>, String> label = options.label() == null ? null
            : e -> ((Function) options.label()).apply(e).toString();
        Consumer<Enum<?>> onPreview = options.onPreview() == null ? null
            : e -> ((Consumer) options.onPreview()).accept(e);
        Function<Enum<?>, String> groupKey = options.groupKey() == null ? null
            : e -> ((Function) options.groupKey()).apply(e).toString();
        Function<Enum<?>, Material> material = options.material() == null
            ? EnumSelectionMenu::resolveDefaultMaterial
            : e -> (Material) ((Function) options.material()).apply(e);

        return new PickContract(enumType, titlePrefix,
            (Supplier<? extends Enum<?>>) current,
            e -> ((Consumer) onPick).accept(e),
            reopenParent, values, filter, label, onPreview,
            groupKey, null, material);
    }

    // -------------------------------------------------------------------------
    //  Default material-resolution helpers
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
     * Picks a default display material for any enum constant. {@link Material} self-maps
     * (filtered to items), {@link SwordSoundType} uses prefix-based matching, {@link Particle}
     * uses keyword matching, everything else falls back to paper.
     */
    private static Material resolveDefaultMaterial(Enum<?> value) {
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
            String candidate = String.join("_", Arrays.copyOf(parts, len));
            try {
                Material mat = Material.valueOf(candidate);
                if (!mat.isAir() && mat.isItem()) return mat;
            } catch (IllegalArgumentException ignored) { }
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
            String candidate = String.join("_", Arrays.copyOf(parts, len));
            try {
                Material mat = Material.valueOf(candidate);
                if (mat.isItem() && !mat.isAir()) return mat;
            } catch (IllegalArgumentException ignored) { }
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
            String candidate = String.join("_", Arrays.copyOf(parts, len)) + "_SPAWN_EGG";
            try {
                return Material.valueOf(candidate);
            } catch (IllegalArgumentException ignored) { }
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
