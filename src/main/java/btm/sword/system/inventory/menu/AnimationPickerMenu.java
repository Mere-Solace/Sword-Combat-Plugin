package btm.sword.system.inventory.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
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
import btm.sword.system.scene.animation.AnimationDef;
import btm.sword.system.scene.animation.AnimationRegistry;
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
 * A paged picker menu listing all registered {@link AnimationDef} entries from
 * {@link AnimationRegistry}. Clicking an entry sets the owning
 * {@link Config.ConfigEntry}{@code <String>} to the selected anim key, saves the config,
 * and returns to the parent menu via {@code reopenMenu}.
 *
 * <p>Opened by {@link btm.sword.system.inventory.item.ConfigEntryItem} when the user
 * clicks a {@code String}-typed config entry.</p>
 */
public class AnimationPickerMenu extends Menu {

    private final Config.ConfigEntry<String> entry;
    private final Runnable reopenMenu;

    /**
     * Creates an animation picker for the given string config entry.
     *
     * @param player     the player viewing the picker
     * @param entry      the {@code String} config entry to update on selection
     * @param reopenMenu called after a selection to reopen the parent config section menu
     */
    public AnimationPickerMenu(SwordPlayer player, Config.ConfigEntry<String> entry, Runnable reopenMenu) {
        super(player);
        this.entry = entry;
        this.reopenMenu = reopenMenu;
    }

    @Override
    public void open() {
        Player player = swordPlayer.player();
        String currentKey = ConfigManager.getInstance().getConfig().getString(entry.path(), entry.defaultValue());

        AnimationRegistry.syncFromLocalStorage();

        List<Item> animItems = new ArrayList<>();
        for (AnimationDef def : AnimationRegistry.all()) {
            boolean selected = def.key().equals(currentKey);
            animItems.add(new AbstractItem() {
                @Override
                public ItemProvider getItemProvider() {
                    return new ItemWrapper(new ItemStackBuilder(Material.RECOVERY_COMPASS)
                        .name(Component.text(def.key(), selected ? NamedTextColor.GREEN : NamedTextColor.AQUA, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text("Group: ", NamedTextColor.GRAY).append(Component.text(def.groupTag(), NamedTextColor.WHITE)),
                            Component.text("Anim:  ", NamedTextColor.GRAY).append(Component.text(def.animTag(), NamedTextColor.WHITE)),
                            Component.text("Loop:  ", NamedTextColor.GRAY).append(AnimationRegistry.isLooping(def.key())
                                ? Component.text("yes", NamedTextColor.GREEN)
                                : Component.text("no", NamedTextColor.RED)),
                            selected
                                ? Component.text("Current selection", NamedTextColor.GREEN)
                                : Component.text("Click to select", NamedTextColor.DARK_GRAY)
                        ))
                        .build());
                }

                @Override
                public void handleClick(@NotNull ClickType clickType, @NotNull Player p, @NotNull InventoryClickEvent event) {
                    if (clickType != ClickType.LEFT) return;
                    ConfigManager.getInstance().setValue(entry, def.key());
                    reopenMenu.run();
                }
            });
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> reopenMenu.run()
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
            .setContent(animItems)
            .build();

        Window window = Window.single()
            .setViewer(player)
            .setTitle("Pick Animation  (" + animItems.size() + " registered)")
            .setGui(gui)
            .build();

        window.open();
    }
}
