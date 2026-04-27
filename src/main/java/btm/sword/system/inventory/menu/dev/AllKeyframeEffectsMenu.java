package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.visuals.ParticleDisplay;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.inventory.menu.Menu;
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
 * Paginated view of every {@link ParticleDisplay} attached to every keyframe in the current
 * editing session.
 *
 * <p>Each entry shows the keyframe index, display index, and shape type. Left-click or
 * SWAP opens that display in {@link ParticleDisplayEditorMenu}. SHIFT+LEFT deletes it.</p>
 */
public class AllKeyframeEffectsMenu extends Menu {

    /** @param player the player viewing the menu */
    public AllKeyframeEffectsMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        List<VolumeKeyframe> keyframes = session.getEditKeyframes();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new KeyframeVisualsMenu(swordPlayer).open());

        int totalEffects = 0;
        for (VolumeKeyframe kf : keyframes) {
            if (kf.effect() != null && kf.effect().displays() != null) {
                totalEffects += kf.effect().displays().size();
            }
        }

        SimpleItem info = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text("All Effects", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Total: " + totalEffects + " effect(s)", NamedTextColor.GRAY),
                    Component.text("Across " + keyframes.size() + " keyframe(s)", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("LEFT / SWAP: edit", NamedTextColor.YELLOW),
                    Component.text("SHIFT+LEFT: delete", NamedTextColor.RED)))
                .build());

        List<Item> entries = buildEffectEntries(session, keyframes);

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B . I . . . . . .",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< # # # # # # # >")
            .addIngredient('#', BORDER)
            .addIngredient('B', back)
            .addIngredient('I', info)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(entries)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("All Effects — " + session.getCurrentAttackName())
            .setGui(gui)
            .build()
            .open();
    }

    private List<Item> buildEffectEntries(AttackDevSession session, List<VolumeKeyframe> keyframes) {
        List<Item> items = new ArrayList<>();
        for (int kfIdx = 0; kfIdx < keyframes.size(); kfIdx++) {
            VolumeKeyframe kf = keyframes.get(kfIdx);
            if (kf.effect() == null || kf.effect().displays() == null) continue;
            List<ParticleDisplay> displays = kf.effect().displays();
            for (int dIdx = 0; dIdx < displays.size(); dIdx++) {
                final int finalKf = kfIdx;
                final int finalD = dIdx;
                ParticleDisplay d = displays.get(dIdx);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Keyframe: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("#" + kfIdx, NamedTextColor.GOLD)));
                lore.add(Component.text("Display:  ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("#" + dIdx, NamedTextColor.YELLOW)));
                lore.add(Component.text("Particles: " + d.getParticles().size(), NamedTextColor.GRAY));
                lore.add(Component.text("Repeat: " + d.getRepeatCount()
                    + " × " + d.getRepeatPeriodTicks() + "t", NamedTextColor.GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("LEFT / SWAP: edit", NamedTextColor.YELLOW));
                lore.add(Component.text("SHIFT+LEFT: delete", NamedTextColor.RED));

                items.add(new SimpleItem(
                    new ItemStackBuilder(iconFor(d))
                        .name(Component.text(d.shapeTypeLabel() + "  KF#" + kfIdx + "  D#" + dIdx,
                            NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .lore(lore)
                        .build(),
                    click -> {
                        ClickType type = click.getClickType();
                        if (type == ClickType.SHIFT_LEFT) {
                            session.setCurrentKeyframeIndex(finalKf);
                            session.removeKeyframeDisplay(finalKf, finalD);
                            open();
                            return;
                        }
                        session.setCurrentKeyframeIndex(finalKf);
                        new ParticleDisplayEditorMenu(swordPlayer, finalKf, finalD).open();
                    }));
            }
        }
        return items;
    }

    private static Material iconFor(ParticleDisplay d) {
        return switch (d.shapeTypeLabel()) {
            case "Line" -> Material.STICK;
            case "Sphere" -> Material.SLIME_BALL;
            case "Circle" -> Material.ENDER_PEARL;
            default -> Material.REDSTONE;
        };
    }
}
