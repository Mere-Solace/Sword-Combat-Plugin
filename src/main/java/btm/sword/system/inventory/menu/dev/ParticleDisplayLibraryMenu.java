package btm.sword.system.inventory.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

import btm.sword.Sword;
import btm.sword.system.attack.def.AttackRegistry;
import btm.sword.system.attack.def.ParticleDisplayLibrary;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.visuals.OriginAnchor;
import btm.sword.system.attack.visuals.ParticleDisplay;
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
 * Paged library picker for {@link ParticleDisplay} presets.
 *
 * <p>Aggregates four sources in order:
 * <ol>
 *   <li><b>Library presets</b> — named entries from {@code particles.yml} via
 *       {@link ParticleDisplayLibrary}; can be deleted from this menu.</li>
 *   <li><b>Current attack</b> — every display attached to any keyframe in the
 *       active editing session.</li>
 *   <li><b>Other registered attacks</b> — displays from all other attacks in
 *       {@link AttackRegistry}.</li>
 *   <li><b>Prefab starters</b> — static {@link ParticleWrapper} fields from
 *       {@link Prefab.Particles} snapshotted as {@link PointDisplay}s.</li>
 * </ol>
 *
 * <p>Left-click or SWAP copies the selected display to the current keyframe and
 * opens {@link ParticleDisplayEditorMenu}. Library entries also support
 * SHIFT+LEFT to delete from the library and save.</p>
 */
public class ParticleDisplayLibraryMenu extends Menu {

    private final Runnable returnTo;

    /**
     * @param player   the player opening the menu
     * @param returnTo called when the player clicks Back
     */
    public ParticleDisplayLibraryMenu(SwordPlayer player, Runnable returnTo) {
        super(player);
        this.returnTo = returnTo;
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> returnTo.run());

        SimpleItem saveNew = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Save Current Display to Library", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Opens the current display editor to choose", NamedTextColor.DARK_GRAY),
                    Component.text("a display, then prompts for a name.", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("Or: use Save to Library inside the display editor.", NamedTextColor.GRAY)))
                .build());

        List<Item> entries = new ArrayList<>();
        buildLibraryEntries(entries, session);
        buildCurrentAttackEntries(entries, session);
        buildOtherAttackEntries(entries, session);

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B . . . . . . . W",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< # # # # # # # >")
            .addIngredient('#', BORDER)
            .addIngredient('B', back)
            .addIngredient('W', saveNew)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('<', new PreviousItem())
            .addIngredient('>', new ForwardItem())
            .setContent(entries)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Particle Display Library")
            .setGui(gui)
            .build()
            .open();
    }

    // ── Section builders ──────────────────────────────────────────────────────

    private void buildLibraryEntries(List<Item> out, AttackDevSession session) {
        Map<String, ParticleDisplay> presets = ParticleDisplayLibrary.getAll();
        if (presets.isEmpty()) return;
        out.add(sectionHeader("Library Presets  (" + presets.size() + ")", Material.BOOKSHELF));
        for (Map.Entry<String, ParticleDisplay> entry : presets.entrySet()) {
            String name = entry.getKey();
            ParticleDisplay d = entry.getValue();
            List<Component> lore = buildDisplayLore(d);
            lore.add(Component.empty());
            lore.add(Component.text("LEFT / SWAP: copy to keyframe + edit", NamedTextColor.YELLOW));
            lore.add(Component.text("SHIFT+LEFT: remove from library", NamedTextColor.RED));
            out.add(new SimpleItem(
                new ItemStackBuilder(iconFor(d))
                    .name(Component.text("⬡ " + name, NamedTextColor.AQUA, TextDecoration.BOLD))
                    .lore(lore)
                    .build(),
                click -> {
                    if (click.getClickType() == ClickType.SHIFT_LEFT) {
                        ParticleDisplayLibrary.remove(name,
                            new File(Sword.getInstance().getDataFolder(), "particles.yml"));
                        open();
                        return;
                    }
                    copyToKeyframeAndEdit(session, d.copy());
                }));
        }
    }

    private void buildCurrentAttackEntries(List<Item> out, AttackDevSession session) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs == null) return;
        List<DisplayEntry> found = collectFromKeyframes(kfs, session.getCurrentAttackName());
        if (found.isEmpty()) return;
        out.add(sectionHeader("Current Attack  (" + found.size() + ")", Material.BLAZE_POWDER));
        for (DisplayEntry e : found) {
            out.add(makeEntry(session, e, NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void buildOtherAttackEntries(List<Item> out, AttackDevSession session) {
        String currentId = session.getCurrentAttackName();
        int total = 0;
        List<Item> items = new ArrayList<>();
        for (var def : AttackRegistry.getAll()) {
            if (def.getId().equals(currentId)) continue;
            if (!(def.getTrajectory() instanceof KeyframedTrajectory kt)) continue;
            List<DisplayEntry> found = collectFromKeyframes(kt.getKeyframes(), def.getId());
            for (DisplayEntry e : found) {
                total++;
                items.add(makeEntry(session, e, NamedTextColor.YELLOW));
            }
        }
        if (items.isEmpty()) return;
        out.add(sectionHeader("Other Attacks  (" + total + ")", Material.BOOK));
        out.addAll(items);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private record DisplayEntry(ParticleDisplay display, String sourceName, int kfIndex) {}

    private static List<DisplayEntry> collectFromKeyframes(List<VolumeKeyframe> kfs, String source) {
        List<DisplayEntry> out = new ArrayList<>();
        for (int i = 0; i < kfs.size(); i++) {
            var effect = kfs.get(i).effect();
            if (effect == null || effect.displays() == null) continue;
            for (ParticleDisplay d : effect.displays()) {
                out.add(new DisplayEntry(d, source, i));
            }
        }
        return out;
    }

    private Item makeEntry(AttackDevSession session, DisplayEntry e, NamedTextColor nameColor) {
        List<Component> lore = buildDisplayLore(e.display());
        lore.add(Component.text("Source: " + e.sourceName() + "  KF#" + e.kfIndex(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("LEFT / SWAP: copy to keyframe + edit", NamedTextColor.YELLOW));
        lore.add(Component.text("SHIFT+LEFT: save copy to library", NamedTextColor.AQUA));
        return new SimpleItem(
            new ItemStackBuilder(iconFor(e.display()))
                .name(Component.text("⬡ " + e.display().shapeTypeLabel(), nameColor, TextDecoration.BOLD))
                .lore(lore)
                .build(),
            click -> {
                if (click.getClickType() == ClickType.SHIFT_LEFT) {
                    promptSaveToLibrary(e.display().copy());
                    return;
                }
                copyToKeyframeAndEdit(session, e.display().copy());
            });
    }

    private void copyToKeyframeAndEdit(AttackDevSession session, ParticleDisplay copy) {
        int kfIdx = session.getCurrentKeyframeIndex();
        session.addKeyframeDisplay(kfIdx, copy);
        int newIndex = session.getEditKeyframes().get(kfIdx).effect().displays().size() - 1;
        new ParticleDisplayEditorMenu(swordPlayer, kfIdx, newIndex).open();
    }

    private void promptSaveToLibrary(ParticleDisplay display) {
        ChatInputCapture.prompt(
            swordPlayer.player(),
            Component.text("Enter a name for this preset:", NamedTextColor.AQUA),
            name -> {
                if (name.equalsIgnoreCase("cancel")) return;
                String key = name.trim().toLowerCase().replace(' ', '_');
                if (key.isEmpty()) return;
                ParticleDisplayLibrary.register(key, display,
                    new File(Sword.getInstance().getDataFolder(), "particles.yml"));
                swordPlayer.message(Component.text(
                    "[Dev] Saved preset '" + key + "' to particles.yml", NamedTextColor.GREEN));
            });
    }

    private static Item sectionHeader(String label, Material mat) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text("── " + label + " ──", NamedTextColor.WHITE, TextDecoration.BOLD))
                .build());
    }

    private static List<Component> buildDisplayLore(ParticleDisplay d) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Shape: " + d.shapeTypeLabel(), NamedTextColor.GRAY));
        lore.add(Component.text("Anchor: " + anchorLabel(d.getAnchor()), NamedTextColor.GRAY));
        lore.add(Component.text(String.format("Origin offset: %.2f / %.2f / %.2f",
            d.getOriginOffset().x, d.getOriginOffset().y, d.getOriginOffset().z), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(String.format("Rand range:   %.2f / %.2f / %.2f",
            d.getRandomOffsetRange().x, d.getRandomOffsetRange().y, d.getRandomOffsetRange().z), NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Repeat: " + d.getRepeatCount()
            + " × " + d.getRepeatPeriodTicks() + "t", NamedTextColor.GRAY));
        lore.add(Component.text("Particles: " + d.getParticles().size(), NamedTextColor.GRAY));
        for (ParticleEffect pe : d.getParticles()) {
            String dust = pe.dustOptions() != null
                ? String.format(" dust:#%06X sz%.1f", pe.dustOptions().getColor().asRGB(), pe.dustOptions().getSize())
                : "";
            lore.add(Component.text(String.format("  %s ×%d spr(%.2f,%.2f,%.2f) spd%.2f%s",
                pe.type().name(), pe.count(),
                pe.spreadOffset().x, pe.spreadOffset().y, pe.spreadOffset().z,
                pe.speed(), dust), NamedTextColor.DARK_GRAY));
        }
        return lore;
    }

    private static String anchorLabel(OriginAnchor a) {
        return switch (a) {
            case OriginAnchor.OwningKeyframe ignored -> "Owning";
            case OriginAnchor.KeyframeIndex ki -> "KF#" + ki.index();
            case OriginAnchor.EntityBodyPoint ebp -> "Body." + ebp.point().name();
            case OriginAnchor.FireLockedOrigin ignored -> "Locked";
            case OriginAnchor.RaycastOrigin ignored -> "RayOrigin";
        };
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
