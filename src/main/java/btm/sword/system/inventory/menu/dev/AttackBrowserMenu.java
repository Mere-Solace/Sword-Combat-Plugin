package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import btm.sword.system.attack.def.AttackDef;
import btm.sword.system.attack.dev.AnimationMode;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.dev.DevMode;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.entity.impl.DevSwordPlayer;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
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
 * Dev-only attack browser menu.
 *
 * <p>Lists all attacks currently registered in {@link btm.sword.system.attack.def.AttackRegistry}.
 * Clicking a VOLUME attack opens {@link AttackEditorMenu} with that attack loaded into the editing
 * session. SWEEP attacks are shown but cannot be edited via the menu.</p>
 *
 * <p>A "New Recording" button closes the menu and prompts the player to use the volume-attack
 * wand (SHIFT+LEFT) to begin capturing a new sweep path.</p>
 */
public class AttackBrowserMenu extends Menu {

    /**
     * Creates a new browser for the given player.
     *
     * @param player the player opening this menu
     */
    public AttackBrowserMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        // Stop any active viewing session so the player can re-open the browser cleanly
        AttackDevSession existing = AttackDevSession.get(swordPlayer.player().getUniqueId());
        if (existing != null && existing.getMode() == DevMode.VIEWING) {
            existing.stopViewing();
        }

        List<Item> attackItems = buildAttackItems();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new DevMenu(swordPlayer).open()
        );

        SimpleItem newRecording = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("New Recording", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Hold the volume wand and press", NamedTextColor.DARK_GRAY),
                    Component.text("SHIFT+LEFT to start recording.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                swordPlayer.player().closeInventory();
                swordPlayer.message(Component.text(
                    "[Dev] Hold the volume-attack wand and press SHIFT+LEFT to start recording.",
                    NamedTextColor.YELLOW));
            }
        );

        SimpleItem giveWand = giveItem(Prefab.Items.testVolumeWand());

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "P # # # # # # # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "N G # < # > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('P', back)
            .addIngredient('N', newRecording)
            .addIngredient('G', giveWand)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .setContent(attackItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Attack Browser")
            .setGui(gui)
            .build()
            .open();
    }

    private List<Item> buildAttackItems() {
        List<AttackDef> sorted = btm.sword.system.attack.def.AttackRegistry.getAll().stream()
            .sorted(Comparator.comparing(AttackDef::getId))
            .toList();

        List<Item> items = new ArrayList<>(sorted.size());
        for (AttackDef def : sorted) {
            items.add(buildAttackItem(def));
        }
        return items;
    }

    private Item buildAttackItem(AttackDef def) {
        boolean editable = def.getTrajectory() instanceof KeyframedTrajectory;
        int frameCount = editable
            ? ((KeyframedTrajectory) def.getTrajectory()).getKeyframes().size()
            : -1;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(def.getType().name(), NamedTextColor.GRAY)));
        lore.add(Component.text("Duration: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(def.getDurationMs() + "ms", NamedTextColor.GRAY)));
        if (frameCount >= 0) {
            lore.add(Component.text("Keyframes: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.valueOf(frameCount), NamedTextColor.GRAY)));
        }
        lore.add(Component.empty());
        if (editable) {
            lore.add(Component.text("Left-click to edit", NamedTextColor.YELLOW));
            lore.add(Component.text("Right-click to visualize", NamedTextColor.AQUA));
            lore.add(Component.text("Shift-click to enter Animation Mode", NamedTextColor.GOLD));
        } else {
            lore.add(Component.text("SWEEP attacks cannot be edited here", NamedTextColor.RED));
        }

        Material icon = editable ? Material.PAPER : Material.BOOK;
        ItemStackBuilder builder = new ItemStackBuilder(icon)
            .name(Component.text(def.getId(), NamedTextColor.GOLD, TextDecoration.BOLD))
            .lore(lore);

        if (!editable) {
            return new SimpleItem(builder.build(), click -> swordPlayer.message(
                Component.text("SWEEP attacks cannot be edited via the menu.", NamedTextColor.RED)));
        }

        return new SimpleItem(builder.build(), click -> {
            KeyframedTrajectory kt = (KeyframedTrajectory) def.getTrajectory();
            AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
            if (click.getClickType() == ClickType.SHIFT_LEFT) {
                if (!(swordPlayer instanceof DevSwordPlayer dev)) {
                    swordPlayer.message(Component.text("[Dev] Not a dev player — cannot enter Animation Mode.", NamedTextColor.RED));
                    return;
                }
                session.startEditing(def.getId(), kt.getKeyframes(), def.getDurationMs(), def.getHitValue());
                swordPlayer.player().closeInventory();
                AnimationMode.enter(dev, session);
            } else if (click.getClickType().isRightClick()) {
                session.startViewing(def.getId(), kt.getKeyframes());
                swordPlayer.player().closeInventory();
                swordPlayer.message(Component.text(
                    "[Dev] Visualizing '" + def.getId() + "' — close your inventory to stop.",
                    NamedTextColor.YELLOW));
            } else {
                session.startEditing(def.getId(), kt.getKeyframes(), def.getDurationMs(), def.getHitValue());
                new AttackEditorMenu(swordPlayer).open();
            }
        });
    }
}
