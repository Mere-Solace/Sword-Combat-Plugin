package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.ClickType;

import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.KeyframeEffect;
import btm.sword.system.attack.simulation.SoundCue;
import btm.sword.system.attack.visuals.ParticleDisplay;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Per-keyframe visuals editor — shows the {@link ParticleDisplay}s attached to the
 * current keyframe along with sound-cue controls and add-shape shortcuts.
 *
 * <p>Display entries respond to LEFT / SWAP_OFFHAND (edit in
 * {@link ParticleDisplayEditorMenu}) and SHIFT_LEFT (delete). Swap-click on a keyframe
 * item in {@link AttackEditorMenu} opens this menu for that keyframe.</p>
 */
public class KeyframeVisualsMenu extends Menu {

    private static final List<SoundCue> PRESET_SOUNDS = List.of(
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.8f, 1.2f),
        new SoundCue(Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 1.5f));

    /** @param player the player opening the menu */
    public KeyframeVisualsMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        int kfIdx = session.getCurrentKeyframeIndex();
        if (session.getEditKeyframes() == null || kfIdx < 0 || kfIdx >= session.getEditKeyframes().size()) {
            new AttackEditorMenu(swordPlayer).open();
            return;
        }
        KeyframeEffect current = session.getEditKeyframes().get(kfIdx).effect();
        List<ParticleDisplay> displays = current != null && current.displays() != null
            ? current.displays() : List.of();
        SoundCue currentSound = current != null ? current.sound() : null;

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY)).build(),
            click -> new AttackEditorMenu(swordPlayer).open());

        SimpleItem info = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text("Keyframe #" + kfIdx, NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Displays: " + displays.size(), NamedTextColor.GRAY),
                    Component.text("Sound: " + (currentSound != null ? soundKey(currentSound) : "none"),
                        NamedTextColor.GRAY)))
                .build());

        SimpleItem soundCycle = new SimpleItem(
            new ItemStackBuilder(currentSound != null ? Material.NOTE_BLOCK : Material.DEAD_BUSH)
                .name(Component.text("Sound", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text(currentSound != null ? soundKey(currentSound) : "none",
                        NamedTextColor.YELLOW),
                    Component.text("Click to cycle preset sounds.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                SoundCue next = nextSound(currentSound);
                session.setKeyframeEffect(kfIdx, new KeyframeEffect(
                    displays != null ? displays : new ArrayList<>(), next));
                open();
            });

        SimpleItem soundClear = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Clear Sound", NamedTextColor.RED, TextDecoration.BOLD)).build(),
            click -> {
                if (currentSound == null) return;
                session.setKeyframeEffect(kfIdx, new KeyframeEffect(
                    displays != null ? displays : new ArrayList<>(), null));
                open();
            });

        SimpleItem addPoint = addShape("Add Point", Material.REDSTONE, "POINT");
        SimpleItem addLine = addShape("Add Line", Material.STICK, "LINE");
        SimpleItem addSphere = addShape("Add Sphere", Material.SLIME_BALL, "SPHERE");
        SimpleItem addCircle = addShape("Add Circle", Material.ENDER_PEARL, "CIRCLE");

        Item[] slots = new Item[18];
        for (int i = 0; i < 18; i++) {
            if (i < displays.size()) {
                final int dIdx = i;
                ParticleDisplay d = displays.get(i);
                slots[i] = new SimpleItem(
                    new ItemStackBuilder(iconFor(d))
                        .name(Component.text(d.shapeTypeLabel() + " #" + i,
                            NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text("Particles: " + d.getParticles().size(), NamedTextColor.GRAY),
                            Component.text("Repeat: " + d.getRepeatCount()
                                + " × " + d.getRepeatPeriodTicks() + "t", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("LEFT / SWAP: edit", NamedTextColor.YELLOW),
                            Component.text("SHIFT+LEFT: delete", NamedTextColor.RED)))
                        .build(),
                    click -> {
                        ClickType type = click.getClickType();
                        if (type == ClickType.SHIFT_LEFT) {
                            session.removeKeyframeDisplay(kfIdx, dIdx);
                            open();
                            return;
                        }
                        new ParticleDisplayEditorMenu(swordPlayer, kfIdx, dIdx).open();
                    });
            } else {
                slots[i] = new SimpleItem(
                    new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.text("—", NamedTextColor.DARK_GRAY)).build());
            }
        }

        Gui gui = Gui.normal()
            .setStructure(
                "B . I . . . . C L",
                "1 2 3 4 5 6 7 8 9",
                "a b c d e f g h i",
                ". P . N . S . R .",
                "# # # # # # # # #",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('.', BORDER)
            .addIngredient('B', back)
            .addIngredient('I', info)
            .addIngredient('C', soundCycle)
            .addIngredient('L', soundClear)
            .addIngredient('1', slots[0]).addIngredient('2', slots[1]).addIngredient('3', slots[2])
            .addIngredient('4', slots[3]).addIngredient('5', slots[4]).addIngredient('6', slots[5])
            .addIngredient('7', slots[6]).addIngredient('8', slots[7]).addIngredient('9', slots[8])
            .addIngredient('a', slots[9]).addIngredient('b', slots[10]).addIngredient('c', slots[11])
            .addIngredient('d', slots[12]).addIngredient('e', slots[13]).addIngredient('f', slots[14])
            .addIngredient('g', slots[15]).addIngredient('h', slots[16]).addIngredient('i', slots[17])
            .addIngredient('P', addPoint).addIngredient('N', addLine)
            .addIngredient('S', addSphere).addIngredient('R', addCircle)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Visuals — Keyframe #" + kfIdx)
            .setGui(gui)
            .build()
            .open();
    }

    private SimpleItem addShape(String label, Material mat, String shape) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label, NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Appends a default " + shape.toLowerCase() + " display.",
                    NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
                int kfIdx = session.getCurrentKeyframeIndex();
                ParticleDisplay fresh = ParticleDisplayEditorMenu.createDefault(shape);
                session.addKeyframeDisplay(kfIdx, fresh);
                int newIndex = session.getEditKeyframes().get(kfIdx).effect().displays().size() - 1;
                new ParticleDisplayEditorMenu(swordPlayer, kfIdx, newIndex).open();
            });
    }

    private static Material iconFor(ParticleDisplay d) {
        return switch (d.shapeTypeLabel()) {
            case "Line" -> Material.STICK;
            case "Sphere" -> Material.SLIME_BALL;
            case "Circle" -> Material.ENDER_PEARL;
            default -> Material.REDSTONE;
        };
    }

    private static String soundKey(SoundCue current) {
        var key = Registry.SOUNDS.getKey(current.sound());
        return key != null ? key.getKey() : "unknown";
    }

    private static SoundCue nextSound(SoundCue current) {
        if (current == null) return PRESET_SOUNDS.getFirst();
        for (int i = 0; i < PRESET_SOUNDS.size(); i++) {
            if (PRESET_SOUNDS.get(i).sound() == current.sound()) {
                return PRESET_SOUNDS.get((i + 1) % PRESET_SOUNDS.size());
            }
        }
        return PRESET_SOUNDS.getFirst();
    }
}
