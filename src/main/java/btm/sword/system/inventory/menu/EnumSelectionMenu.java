package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.List;

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
 * Left-click selects a value, saves it via {@link ConfigManager#setValue}, and
 * returns to the parent menu via the {@code reopenParent} callback.
 * </p>
 * <p>
 * For {@link SoundType} entries, right-clicking an item previews the sound at
 * the player's location without leaving the menu.
 * </p>
 */
public class EnumSelectionMenu extends Menu {

    private final Config.ConfigEntry<?> entry;
    private final Runnable reopenParent;

    /**
     * Constructs an enum selection menu for the given config entry.
     *
     * @param player       the viewing player
     * @param entry        the config entry whose enum type to browse
     * @param reopenParent called to reopen the parent menu after selection or back
     */
    public EnumSelectionMenu(SwordPlayer player, Config.ConfigEntry<?> entry, Runnable reopenParent) {
        super(player);
        this.entry = entry;
        this.reopenParent = reopenParent;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        FileConfiguration config = ConfigManager.getInstance().getConfig();
        Class<?> type = entry.type;
        boolean isSoundType = type == SoundType.class;

        // Determine currently stored value name for selection highlight
        String stored = config.getString(entry.path);
        String currentName = stored != null ? stored.toUpperCase()
            : (entry.defaultValue instanceof Enum<?> e ? e.name() : "");

        Object[] constants = type.getEnumConstants();
        List<Item> items = new ArrayList<>(constants.length);
        for (Object constant : constants) {
            Enum<?> value = (Enum<?>) constant;
            boolean selected = value.name().equalsIgnoreCase(currentName);
            items.add(buildValueItem(value, selected, isSoundType));
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
            .setContent(items)
            .build();

        String title = type.getSimpleName() + "  (" + constants.length + " values)";
        Window window = Window.single()
            .setViewer(player)
            .setTitle(title)
            .setGui(gui)
            .build();

        window.open();
    }

    /**
     * Builds a single clickable item for one enum constant.
     *
     * @param value       the enum constant this item represents
     * @param selected    whether this constant is the current config value
     * @param isSoundType whether right-click should preview as a sound
     * @return the constructed InvUI item
     */
    private Item buildValueItem(Enum<?> value, boolean selected, boolean isSoundType) {
        Material mat = resolveItemMaterial(value);

        return new AbstractItem() {
            @Override
            public ItemProvider getItemProvider() {
                String displayName = selected ? "§a✔ §f" + value.name() : "§7" + value.name();
                ItemBuilder builder = new ItemBuilder(mat).setDisplayName(displayName);
                if (selected) builder.addLoreLines("§a§o(currently selected)");
                if (isSoundType) {
                    builder.addLoreLines("§8L-click: select", "§8R-click: preview sound");
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

    /**
     * Picks the display material for a given enum constant.
     * <p>
     * {@link Material} constants use themselves as the item material so the
     * player can see exactly what each value looks like. {@link SoundType}
     * constants use a note block. Everything else uses paper.
     * </p>
     *
     * @param value the enum constant
     * @return the material to display
     */
    private static Material resolveItemMaterial(Enum<?> value) {
        if (value instanceof Material m) return m;
        if (value instanceof SoundType) return Material.NOTE_BLOCK;
        return Material.PAPER;
    }
}
