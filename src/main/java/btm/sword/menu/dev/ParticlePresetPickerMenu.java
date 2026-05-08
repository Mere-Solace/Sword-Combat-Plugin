package btm.sword.menu.dev;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Particle;

import btm.sword.combat.dev.AttackDevSession;
import btm.sword.combat.simulation.ParticleEffect;
import btm.sword.combat.visuals.ParticleDisplay;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.prefab.Prefab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged preset picker for individual {@link ParticleEffect}s.
 *
 * <p>Loads all static {@link ParticleWrapper} fields from {@link Prefab.Particles},
 * converts each to a {@link ParticleEffect} snapshot, deduplicates by content, and
 * presents them as clickable entries. Clicking an entry appends the effect to the
 * target display's particle list and returns to {@link ParticleListMenu}.</p>
 */
public class ParticlePresetPickerMenu extends Menu {

    private final int kfIndex;
    private final int displayIndex;

    /**
     * @param player       the player opening the menu
     * @param kfIndex      keyframe index within the edit session
     * @param displayIndex display index within the keyframe's display list
     */
    public ParticlePresetPickerMenu(SwordPlayer player, int kfIndex, int displayIndex) {
        super(player);
        this.kfIndex = kfIndex;
        this.displayIndex = displayIndex;
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        ParticleDisplay display = fetch(session);
        if (display == null) {
            new ParticleDisplayEditorMenu(swordPlayer, kfIndex, displayIndex).open();
            return;
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new ParticleListMenu(swordPlayer, kfIndex, displayIndex).open()
        );

        List<Item> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Field field : Prefab.Particles.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!Modifier.isFinal(field.getModifiers())) continue;
            if (!ParticleWrapper.class.isAssignableFrom(field.getType())) continue;
            try {
                ParticleWrapper wrapper = (ParticleWrapper) field.get(null);
                ParticleEffect effect = ParticleEffect.fromWrapper(wrapper);
                String key = dedupKey(effect);
                if (!seen.add(key)) continue;

                String fieldName = field.getName();
                List<Component> lore = buildEffectLore(effect);
                lore.add(Component.empty());
                lore.add(Component.text("Click to add to this display.", NamedTextColor.YELLOW));

                entries.add(new SimpleItem(
                    new ItemStackBuilder(iconFor(effect.type()))
                        .name(Component.text(fieldName, NamedTextColor.AQUA, TextDecoration.BOLD))
                        .lore(lore)
                        .build(),
                    click -> {
                        display.getParticles().add(effect);
                        new ParticleListMenu(swordPlayer, kfIndex, displayIndex).open();
                    }
                ));
            } catch (IllegalAccessException ignored) {
                // skip inaccessible fields
            }
        }

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B . . . . . . . .",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< # # # # # # # >")
            .addIngredient('#', BORDER)
            .addIngredient('.', BORDER)
            .addIngredient('B', back)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(entries)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Particle Presets")
            .setGui(gui)
            .build()
            .open();
    }

    private ParticleDisplay fetch(AttackDevSession session) {
        if (session.getEditKeyframes() == null
                || kfIndex < 0
                || kfIndex >= session.getEditKeyframes().size()) return null;
        var effect = session.getEditKeyframes().get(kfIndex).effect();
        if (effect == null || effect.displays() == null) return null;
        if (displayIndex < 0 || displayIndex >= effect.displays().size()) return null;
        return effect.displays().get(displayIndex);
    }

    private static String dedupKey(ParticleEffect e) {
        String dust = e.dustOptions() != null
            ? e.dustOptions().getColor().asRGB() + "_" + e.dustOptions().getSize()
            : "null";
        return e.type().name() + "|" + e.count()
            + "|" + e.spreadOffset().x + "," + e.spreadOffset().y + "," + e.spreadOffset().z
            + "|" + e.speed()
            + "|" + dust;
    }

    private static List<Component> buildEffectLore(ParticleEffect e) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Type: " + e.type().name(), NamedTextColor.GRAY));
        lore.add(Component.text("Count: " + e.count(), NamedTextColor.GRAY));
        lore.add(Component.text(String.format("Spread: %.2f / %.2f / %.2f",
            e.spreadOffset().x, e.spreadOffset().y, e.spreadOffset().z), NamedTextColor.GRAY));
        String speedLabel = e.speed() < 0 ? "default" : String.format("%.2f", e.speed());
        lore.add(Component.text("Speed: " + speedLabel, NamedTextColor.GRAY));
        if (e.dustOptions() != null) {
            lore.add(Component.text(String.format("Dust: #%06X  size %.1f",
                e.dustOptions().getColor().asRGB(), e.dustOptions().getSize()), NamedTextColor.DARK_GRAY));
        }
        return lore;
    }

    private static Material iconFor(Particle p) {
        String n = p.name();
        if (n.contains("FLAME") || n.contains("FIRE")) return Material.FLINT_AND_STEEL;
        if (n.contains("SMOKE") || n.contains("CLOUD") || n.contains("POOF")) return Material.COAL;
        if (n.contains("CRIT") || n.contains("ENCHANTED_HIT")) return Material.DIAMOND_SWORD;
        if (n.contains("ENCHANT")) return Material.ENCHANTED_BOOK;
        if (n.contains("DUST") || n.contains("REDSTONE")) return Material.REDSTONE;
        if (n.contains("SOUL")) return Material.SOUL_SAND;
        if (n.contains("PORTAL")) return Material.ENDER_EYE;
        if (n.contains("LAVA")) return Material.LAVA_BUCKET;
        if (n.contains("HEART")) return Material.PINK_DYE;
        if (n.contains("GUST") || n.contains("WIND")) return Material.FEATHER;
        if (n.contains("FLASH")) return Material.GLOWSTONE_DUST;
        return Material.FIREWORK_STAR;
    }
}
