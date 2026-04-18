package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import btm.sword.system.attack.HitPacketPreset;
import btm.sword.system.attack.HitPacketRegistry;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged browser for all {@link HitPacketPreset}s registered in {@link HitPacketRegistry}.
 *
 * <p>Left-click assigns the preset to the calling player's {@link AttackDevSession} as
 * the current edit hit-value and closes the menu. Right-click opens
 * {@link HitPacketEditorMenu} for that preset. The "New Preset" button prompts the
 * player for an id via an anvil dialog, registers a default preset, and opens the editor.</p>
 */
public class HitPacketLibraryMenu extends Menu {

    /**
     * Creates a new library browser for the given player.
     *
     * @param player the player opening the menu
     */
    public HitPacketLibraryMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        List<Item> items = buildPresetItems();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        SimpleItem newPreset = new SimpleItem(
            new ItemStackBuilder(Material.LIME_TERRACOTTA)
                .name(Component.text("New Preset", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Enter a unique id to create", NamedTextColor.DARK_GRAY),
                    Component.text("a new hit-packet preset.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> ChatInputCapture.prompt(
                swordPlayer.player(),
                Component.text("Type a display name for the new preset:", NamedTextColor.YELLOW),
                text -> {
                    if (text.equalsIgnoreCase("cancel") || text.isEmpty()) {
                        open();
                        return;
                    }
                    String base = HitPacketPreset.slugify(text);
                    if (base.isEmpty()) {
                        swordPlayer.message(Component.text(
                            "[Dev] Name must contain at least one alphanumeric character.",
                            NamedTextColor.RED));
                        open();
                        return;
                    }
                    String id = HitPacketEditorMenu.uniqueId(base, null);
                    HitPacketPreset preset = HitPacketPreset.newDefault(id).withDisplayName(text);
                    new HitPacketEditorMenu(swordPlayer, preset).open();
                }
            )
        );

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "N # # < # > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('P', back)
            .addIngredient('N', newPreset)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .setContent(items)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Hit Packet Library")
            .setGui(gui)
            .build()
            .open();
    }

    private List<Item> buildPresetItems() {
        List<HitPacketPreset> presets = HitPacketRegistry.allSorted();
        List<Item> items = new ArrayList<>(presets.size());
        for (HitPacketPreset preset : presets) {
            items.add(buildPresetItem(preset));
        }
        return items;
    }

    private Item buildPresetItem(HitPacketPreset preset) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("id: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(preset.id(), NamedTextColor.AQUA)));
        lore.add(Component.empty());
        lore.add(Component.text("shard: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(String.valueOf(preset.shardDamage()), NamedTextColor.WHITE)));
        lore.add(Component.text("tough: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(fmt2(preset.toughnessDamage()), NamedTextColor.WHITE)));
        lore.add(Component.text("soul loss: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(fmt2(preset.soulfireLoss()), NamedTextColor.WHITE)));
        lore.add(Component.text("reaped: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(fmt2(preset.reapedSoulfire()), NamedTextColor.WHITE)));
        lore.add(Component.text("invul: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(preset.invulnerableTicks() + "t", NamedTextColor.WHITE)));
        lore.add(Component.text("block: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(preset.blockability().name(), NamedTextColor.WHITE)));
        lore.add(Component.text("bypass: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(fmt2(preset.bypassPower()), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Left: select for current attack", NamedTextColor.YELLOW));
        lore.add(Component.text("Right: edit preset", NamedTextColor.AQUA));

        return new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(preset.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(lore)
                .build(),
            click -> {
                if (click.getClickType() == ClickType.RIGHT || click.getClickType() == ClickType.SHIFT_RIGHT) {
                    new HitPacketEditorMenu(swordPlayer, preset).open();
                } else {
                    AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
                    session.setEditHitValue(preset.toHitValuePacket());
                    swordPlayer.message(Component.text(
                        "[Dev] Hit packet set to '" + preset.id() + "'.", NamedTextColor.GREEN));
                    new AttackEditorMenu(swordPlayer).open();
                }
            }
        );
    }

    private static String fmt2(float v) {
        return String.format("%.2f", v);
    }
}
