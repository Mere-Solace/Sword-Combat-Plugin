package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.joml.Vector3f;

import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.KeyframeEffect;
import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.system.attack.simulation.SoundCue;
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
 * Per-keyframe effects editor menu.
 *
 * <p>Shows the particle effects and optional sound cue attached to the currently selected keyframe.
 * Effects can be added from a preset list or removed individually. The sound cue cycles through
 * a preset list or can be cleared.</p>
 *
 * <h2>Layout (4 rows × 9)</h2>
 * <pre>
 * Row 0: Back | — — — | Info | — — — —
 * Row 1: existing effects (up to 9, removable) or GRAY placeholders
 * Row 2: preset add buttons (up to 8 presets) | —
 * Row 3: — — — | Sound cycle | Sound clear | — — — —
 * </pre>
 */
public class KeyframeEffectsMenu extends Menu {

    /** Preset particle effects available for quick-add. */
    private static final List<PresetParticle> PRESETS = List.of(
        new PresetParticle("Crit", Material.DIAMOND_SWORD,
            new ParticleEffect(Particle.CRIT, 8, new Vector3f(), 0.3f, null)),
        new PresetParticle("Sweep", Material.IRON_SWORD,
            new ParticleEffect(Particle.SWEEP_ATTACK, 1, new Vector3f(), 0f, null)),
        new PresetParticle("Enchant", Material.ENCHANTING_TABLE,
            new ParticleEffect(Particle.ENCHANT, 12, new Vector3f(), 0.3f, null)),
        new PresetParticle("Soul Fire", Material.SOUL_LANTERN,
            new ParticleEffect(Particle.SOUL_FIRE_FLAME, 5, new Vector3f(), 0.2f, null)),
        new PresetParticle("Smoke", Material.COAL,
            new ParticleEffect(Particle.SMOKE, 6, new Vector3f(), 0.2f, null)),
        new PresetParticle("Explosion", Material.TNT,
            new ParticleEffect(Particle.EXPLOSION, 2, new Vector3f(), 0.1f, null)),
        new PresetParticle("Flash", Material.GLOWSTONE_DUST,
            new ParticleEffect(Particle.FLASH, 1, new Vector3f(), 0f, null)),
        new PresetParticle("Dust (Orange)", Material.ORANGE_DYE,
            new ParticleEffect(Particle.DUST, 10, new Vector3f(), 0.2f,
                new Particle.DustOptions(Color.fromRGB(255, 120, 0), 1.0f)))
    );

    /** Preset sound cues available for cycling. Ordered list; {@code null} = no sound. */
    private static final List<SoundCue> PRESET_SOUNDS = List.of(
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 1.0f),
        new SoundCue(Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.8f, 1.2f),
        new SoundCue(Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.5f, 1.5f)
    );

    /**
     * Creates an effects editor for the given player's currently selected keyframe.
     *
     * @param player the player owning the editing session
     */
    public KeyframeEffectsMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        int kfIdx = session.getCurrentKeyframeIndex();
        KeyframeEffect current = session.getEditKeyframes().get(kfIdx).effect();
        List<ParticleEffect> currentParticles = current != null ? current.particles() : List.of();
        SoundCue currentSound = current != null ? current.sound() : null;

        // ── Row 0: Back + Info ────────────────────────────────────────────────

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new AttackEditorMenu(swordPlayer).open()
        );

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Frame #" + kfIdx, NamedTextColor.AQUA));
        infoLore.add(Component.text("Particles: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(String.valueOf(currentParticles.size()), NamedTextColor.YELLOW)));
        infoLore.add(Component.text("Sound: ", NamedTextColor.DARK_GRAY)
            .append(currentSound != null
                ? Component.text(currentSound.sound().key().value(), NamedTextColor.GREEN)
                : Component.text("none", NamedTextColor.GRAY)));
        SimpleItem info = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text("Keyframe Effects", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(infoLore)
                .build()
        );

        // ── Row 1: Existing particle effect slots ─────────────────────────────

        Item[] effectSlots = new Item[9];
        for (int i = 0; i < 9; i++) {
            if (i < currentParticles.size()) {
                final int peIdx = i;
                ParticleEffect pe = currentParticles.get(i);
                effectSlots[i] = new SimpleItem(
                    new ItemStackBuilder(Material.BLAZE_POWDER)
                        .name(Component.text(pe.type().name(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text("Count: " + pe.count(), NamedTextColor.GRAY),
                            Component.text("Spread: " + pe.spread(), NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("Click to remove", NamedTextColor.RED)
                        ))
                        .build(),
                    click -> {
                        List<ParticleEffect> updated = new ArrayList<>(currentParticles);
                        updated.remove(peIdx);
                        session.setKeyframeEffect(kfIdx, new KeyframeEffect(updated, currentSound));
                        new KeyframeEffectsMenu(swordPlayer).open();
                    }
                );
            } else {
                effectSlots[i] = new SimpleItem(
                    new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.text("No Effect", NamedTextColor.DARK_GRAY))
                        .build()
                );
            }
        }

        // ── Row 2: Preset add buttons ─────────────────────────────────────────

        Item[] presetSlots = new Item[9];
        for (int i = 0; i < 9; i++) {
            if (i < PRESETS.size()) {
                final int presetIdx = i;
                PresetParticle preset = PRESETS.get(i);
                presetSlots[i] = new SimpleItem(
                    new ItemStackBuilder(preset.icon())
                        .name(Component.text("+ " + preset.label(), NamedTextColor.GREEN, TextDecoration.BOLD))
                        .lore(List.of(
                            Component.text(preset.effect().type().name(), NamedTextColor.GRAY),
                            Component.text("Count: " + preset.effect().count(), NamedTextColor.GRAY),
                            Component.text("Spread: " + preset.effect().spread(), NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("Click to add", NamedTextColor.YELLOW)
                        ))
                        .build(),
                    click -> {
                        if (currentParticles.size() >= 9) {
                            swordPlayer.message(
                                Component.text("Maximum of 9 particle effects per keyframe.", NamedTextColor.RED));
                            return;
                        }
                        List<ParticleEffect> updated = new ArrayList<>(currentParticles);
                        updated.add(PRESETS.get(presetIdx).effect());
                        session.setKeyframeEffect(kfIdx, new KeyframeEffect(updated, currentSound));
                        new KeyframeEffectsMenu(swordPlayer).open();
                    }
                );
            } else {
                presetSlots[i] = new SimpleItem(
                    new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name(Component.text("—", NamedTextColor.DARK_GRAY))
                        .build()
                );
            }
        }

        // ── Row 3: Sound cycle + clear ────────────────────────────────────────

        String soundName = currentSound != null ? currentSound.sound().key().value() : "None";
        SimpleItem soundCycle = new SimpleItem(
            new ItemStackBuilder(currentSound != null ? Material.NOTE_BLOCK : Material.DEAD_BUSH)
                .name(Component.text("Sound: " + soundName, NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Click to cycle through preset sounds.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                SoundCue next = nextSound(currentSound);
                session.setKeyframeEffect(kfIdx, new KeyframeEffect(currentParticles, next));
                new KeyframeEffectsMenu(swordPlayer).open();
            }
        );

        SimpleItem soundClear = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Clear Sound", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Remove the sound cue from this keyframe.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (currentSound == null) return;
                session.setKeyframeEffect(kfIdx, new KeyframeEffect(currentParticles, null));
                new KeyframeEffectsMenu(swordPlayer).open();
            }
        );

        // ── Build GUI ─────────────────────────────────────────────────────────

        Gui gui = Gui.normal()
            .setStructure(
                "B # # # I # # # #",
                "0 1 2 3 4 5 6 7 8",
                "a b c d e f g h #",
                "# # # S s # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('B', back)
            .addIngredient('I', info)
            .addIngredient('0', effectSlots[0])
            .addIngredient('1', effectSlots[1])
            .addIngredient('2', effectSlots[2])
            .addIngredient('3', effectSlots[3])
            .addIngredient('4', effectSlots[4])
            .addIngredient('5', effectSlots[5])
            .addIngredient('6', effectSlots[6])
            .addIngredient('7', effectSlots[7])
            .addIngredient('8', effectSlots[8])
            .addIngredient('a', presetSlots[0])
            .addIngredient('b', presetSlots[1])
            .addIngredient('c', presetSlots[2])
            .addIngredient('d', presetSlots[3])
            .addIngredient('e', presetSlots[4])
            .addIngredient('f', presetSlots[5])
            .addIngredient('g', presetSlots[6])
            .addIngredient('h', presetSlots[7])
            .addIngredient('S', soundCycle)
            .addIngredient('s', soundClear)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Keyframe #" + kfIdx + " Effects")
            .setGui(gui)
            .build()
            .open();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static SoundCue nextSound(SoundCue current) {
        if (current == null) return PRESET_SOUNDS.getFirst();
        for (int i = 0; i < PRESET_SOUNDS.size(); i++) {
            if (PRESET_SOUNDS.get(i).sound() == current.sound()) {
                return (i + 1 < PRESET_SOUNDS.size()) ? PRESET_SOUNDS.get(i + 1) : null;
            }
        }
        return PRESET_SOUNDS.getFirst();
    }

    private record PresetParticle(String label, Material icon, ParticleEffect effect) {}
}
