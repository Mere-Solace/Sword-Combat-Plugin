package btm.sword.system.inventory.menu.dev;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.system.attack.visuals.ParticleDisplay;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.EnumPickerOptions;
import btm.sword.system.inventory.menu.EnumSelectionMenu;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Paged list of all {@link ParticleEffect}s attached to a single {@link ParticleDisplay}.
 *
 * <p>Left-click an entry to open {@link ParticleEffectEditorMenu}. Shift-left-click
 * to delete the entry. The Add button opens an {@link EnumSelectionMenu} particle-type
 * picker; on pick a default effect is appended and the list reopens.</p>
 */
public class ParticleListMenu extends Menu {

    private final int kfIndex;
    private final int displayIndex;

    /**
     * @param player       the player viewing the menu
     * @param kfIndex      keyframe index within the edit session
     * @param displayIndex display index within the keyframe's display list
     */
    public ParticleListMenu(SwordPlayer player, int kfIndex, int displayIndex) {
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

        Player player = swordPlayer.player();
        List<ParticleEffect> particles = display.getParticles();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new ParticleDisplayEditorMenu(swordPlayer, kfIndex, displayIndex).open()
        );

        SimpleItem save = new SimpleItem(
            new ItemStackBuilder(Material.EMERALD)
                .name(Component.text("Save", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Save to attacks/<id>.yml", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> AttackEditorMenu.saveAttack(session, swordPlayer)
        );

        SimpleItem info = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(display.shapeTypeLabel() + " Particles", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Slot " + displayIndex + "  •  " + particles.size() + " entries", NamedTextColor.DARK_GRAY),
                    Component.text("L-click: edit  |  Shift-L: delete", NamedTextColor.DARK_GRAY)))
                .build()
        );

        SimpleItem addButton = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+ Add Particle", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Pick a particle type to append.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> EnumSelectionMenu.forEnum(
                swordPlayer,
                Particle.class,
                "Particle Type",
                () -> Particle.CRIT,
                chosen -> {
                    List<ParticleEffect> updated = new ArrayList<>(display.getParticles());
                    updated.add(new ParticleEffect(chosen, 1, new Vector3f(0.2f, 0.2f, 0.2f), -1.0, null, null));
                    display.setParticles(updated);
                },
                this::open,
                EnumPickerOptions.<Particle>builder()
                    .filter(p -> !p.name().startsWith("LEGACY_"))
                    .build()
            ).open()
        );

        SimpleItem fromPreset = new SimpleItem(
            new ItemStackBuilder(Material.NETHER_STAR)
                .name(Component.text("Add from Preset", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("Pick a pre-configured particle effect.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new ParticlePresetPickerMenu(swordPlayer, kfIndex, displayIndex).open()
        );

        SimpleItem clearButton = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Clear All", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Remove all particle entries.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                display.setParticles(new ArrayList<>());
                open();
            }
        );

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < particles.size(); i++) {
            ParticleEffect pe = particles.get(i);
            items.add(particleEntry(pe, i, display));
        }

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B V I # A P X # #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < . > # # #")
            .addIngredient('#', BORDER)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('B', back)
            .addIngredient('V', save)
            .addIngredient('I', info)
            .addIngredient('X', clearButton)
            .addIngredient('A', addButton)
            .addIngredient('P', fromPreset)
            .addIngredient('<', new btm.sword.system.inventory.item.PreviousItem())
            .addIngredient('>', new btm.sword.system.inventory.item.ForwardItem())
            .setContent(items)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle(display.shapeTypeLabel() + " Particles — kf" + kfIndex + " slot" + displayIndex)
            .setGui(gui)
            .build()
            .open();
    }

    private Item particleEntry(ParticleEffect pe, int idx, ParticleDisplay display) {
        Material mat = iconFor(pe.type());
        Vector3f so = pe.spreadOffset();
        String speedLabel = pe.speed() < 0 ? "default" : String.format("%.2f", pe.speed());

        return new AbstractItem() {
            @Override
            public xyz.xenondevs.invui.item.ItemProvider getItemProvider() {
                return new xyz.xenondevs.invui.item.ItemWrapper(
                    new ItemStackBuilder(mat)
                        .name(Component.text("#" + idx + "  ", NamedTextColor.DARK_GRAY)
                            .append(Component.text(pe.type().name(), NamedTextColor.AQUA, TextDecoration.BOLD)))
                        .lore(List.of(
                            Component.text("Count: " + pe.count(), NamedTextColor.GRAY),
                            Component.text(String.format("Spread: (%.2f, %.2f, %.2f)", so.x, so.y, so.z), NamedTextColor.GRAY),
                            Component.text("Speed: " + speedLabel, NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("L-click: edit  |  Shift-L: delete", NamedTextColor.DARK_GRAY)))
                        .build()
                );
            }

            @Override
            public void handleClick(@NotNull ClickType clickType,
                                    @NotNull Player p,
                                    @NotNull InventoryClickEvent event) {
                if (clickType == ClickType.SHIFT_LEFT) {
                    List<ParticleEffect> updated = new ArrayList<>(display.getParticles());
                    updated.remove(idx);
                    display.setParticles(updated);
                    open();
                } else {
                    new ParticleEffectEditorMenu(swordPlayer, pe.getType(), kfIndex, displayIndex, idx).open();
                }
            }
        };
    }

    private ParticleDisplay fetch(AttackDevSession session) {
        if (session.getEditKeyframes() == null || kfIndex < 0 || kfIndex >= session.getEditKeyframes().size()) return null;
        var effect = session.getEditKeyframes().get(kfIndex).effect();
        if (effect == null || effect.displays() == null) return null;
        if (displayIndex < 0 || displayIndex >= effect.displays().size()) return null;
        return effect.displays().get(displayIndex);
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

    /** Default DustOptions used when appending a DUST particle — placeholder colour. */
    @SuppressWarnings("unused")
    private static Particle.DustOptions defaultDust() {
        return new Particle.DustOptions(Color.fromRGB(255, 120, 0), 1.0f);
    }
}
