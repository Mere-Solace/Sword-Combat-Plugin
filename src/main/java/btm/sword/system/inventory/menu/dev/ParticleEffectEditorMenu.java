package btm.sword.system.inventory.menu.dev;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
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
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Edits a single {@link ParticleEffect} within a {@link ParticleDisplay}'s particle list.
 *
 * <p>Because {@link ParticleEffect} is a record, every mutation replaces the entry at
 * {@code particleIndex} with a new record instance. The display's list is mutated
 * in-place; no separate draft is kept at this level.</p>
 *
 * <p>Controls: particle type (click → picker), count, spawn-offset X/Y/Z, and speed.
 * Speed {@code &lt; 0} means "omit speed from the Bukkit call" (shown as DEFAULT).</p>
 */
public class ParticleEffectEditorMenu extends Menu {

    private final int kfIndex;
    private final int displayIndex;
    private final int particleIndex;

    /**
     * @param player        the player viewing the editor
     * @param kfIndex       keyframe index within the edit session
     * @param displayIndex  display index within the keyframe's display list
     * @param particleIndex index of the {@link ParticleEffect} within the display's list
     */
    public ParticleEffectEditorMenu(SwordPlayer player, int kfIndex, int displayIndex, int particleIndex) {
        super(player);
        this.kfIndex = kfIndex;
        this.displayIndex = displayIndex;
        this.particleIndex = particleIndex;
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        ParticleDisplay display = fetchDisplay(session);
        if (display == null || particleIndex < 0 || particleIndex >= display.getParticles().size()) {
            new ParticleListMenu(swordPlayer, kfIndex, displayIndex).open();
            return;
        }

        Player player = swordPlayer.player();
        ParticleEffect pe = display.getParticles().get(particleIndex);
        Vector3f so = pe.spreadOffset();

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> new ParticleListMenu(swordPlayer, kfIndex, displayIndex).open()
        );

        SimpleItem save = new SimpleItem(
            new ItemStackBuilder(Material.EMERALD)
                .name(Component.text("Save", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Save to attacks/<id>.yml", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> AttackEditorMenu.saveAttack(
                AttackDevSession.getOrCreate(swordPlayer.player()), swordPlayer)
        );

        SimpleItem typeButton = new SimpleItem(
            new ItemStackBuilder(Material.FIREWORK_STAR)
                .name(Component.text("Type: ", NamedTextColor.GRAY)
                    .append(Component.text(pe.type().name(), NamedTextColor.AQUA, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to pick a particle type.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> EnumSelectionMenu.forEnum(
                swordPlayer,
                Particle.class,
                "Particle Type",
                pe::type,
                chosen -> commit(display, pe, chosen, pe.count(), pe.spreadOffset(), pe.speed()),
                this::open,
                EnumPickerOptions.<Particle>builder()
                    .filter(p -> !p.name().startsWith("LEGACY_"))
                    .build()
            ).open()
        );

        String speedLabel = pe.speed() < 0 ? "DEFAULT" : String.format("%.2f", pe.speed());

        // ── Count ─────────────────────────────────────────────────────────────
        SimpleItem cntDec = dec(click -> commit(display, pe, pe.type(),
            Math.max(1, pe.count() - stepInt(click)), pe.spreadOffset(), pe.speed()));
        SimpleItem cntDisp = new SimpleItem(
            new ItemStackBuilder(Material.GUNPOWDER)
                .name(Component.text("Count: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(pe.count()), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openIntAnvil("Count", pe.count(),
                v -> commit(display, pe, pe.type(), Math.max(1, v), pe.spreadOffset(), pe.speed()),
                this::open)
        );
        SimpleItem cntInc = inc(click -> commit(display, pe, pe.type(),
            pe.count() + stepInt(click), pe.spreadOffset(), pe.speed()));

        // ── Speed ─────────────────────────────────────────────────────────────
        SimpleItem spdDec = dec(click -> {
            double cur = pe.speed() < 0 ? 0.0 : pe.speed();
            commit(display, pe, pe.type(), pe.count(), pe.spreadOffset(), Math.max(-1.0, cur - stepFloat(click)));
        });
        SimpleItem spdDisp = new SimpleItem(
            new ItemStackBuilder(Material.FEATHER)
                .name(Component.text("Speed: ", NamedTextColor.GRAY)
                    .append(Component.text(speedLabel, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text("Click to type. −1 = DEFAULT.", NamedTextColor.DARK_GRAY),
                    Component.text("Shift-L: reset to DEFAULT", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (click.getClickType().isShiftClick()) {
                    commit(display, pe, pe.type(), pe.count(), pe.spreadOffset(), -1.0);
                    return;
                }
                openDoubleAnvil("Speed (−1 = default)", pe.speed(),
                    v -> commit(display, pe, pe.type(), pe.count(), pe.spreadOffset(), v),
                    this::open);
            }
        );
        SimpleItem spdInc = inc(click -> {
            double cur = pe.speed() < 0 ? 0.0 : pe.speed();
            commit(display, pe, pe.type(), pe.count(), pe.spreadOffset(), cur + stepFloat(click));
        });

        // ── Spread X ──────────────────────────────────────────────────────────
        SimpleItem sxDec = dec(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x - stepFloat(click), so.y, so.z), pe.speed()));
        SimpleItem sxDisp = spreadField("Spread X", so.x, Material.RED_STAINED_GLASS, pe, display,
            v -> new Vector3f(v, so.y, so.z));
        SimpleItem sxInc = inc(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x + stepFloat(click), so.y, so.z), pe.speed()));

        // ── Spread Y ──────────────────────────────────────────────────────────
        SimpleItem syDec = dec(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x, so.y - stepFloat(click), so.z), pe.speed()));
        SimpleItem syDisp = spreadField("Spread Y", so.y, Material.LIME_STAINED_GLASS, pe, display,
            v -> new Vector3f(so.x, v, so.z));
        SimpleItem syInc = inc(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x, so.y + stepFloat(click), so.z), pe.speed()));

        // ── Spread Z ──────────────────────────────────────────────────────────
        SimpleItem szDec = dec(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x, so.y, so.z - stepFloat(click)), pe.speed()));
        SimpleItem szDisp = spreadField("Spread Z", so.z, Material.LIGHT_BLUE_STAINED_GLASS, pe, display,
            v -> new Vector3f(so.x, so.y, v));
        SimpleItem szInc = inc(click -> commit(display, pe, pe.type(), pe.count(),
            new Vector3f(so.x, so.y, so.z + stepFloat(click)), pe.speed()));

        Gui gui = Gui.normal()
            .setStructure(
                "B . . . . . . V T",
                ". 1 2 3 . 4 5 6 .",
                ". q w e . r s t .",
                ". a b c . . . . .",
                ". . . . . . . . .",
                ". . . . . . . . .")
            .addIngredient('.', BORDER)
            .addIngredient('B', back)
            .addIngredient('V', save)
            .addIngredient('T', typeButton)
            .addIngredient('1', cntDec).addIngredient('2', cntDisp).addIngredient('3', cntInc)
            .addIngredient('4', spdDec).addIngredient('5', spdDisp).addIngredient('6', spdInc)
            .addIngredient('q', sxDec).addIngredient('w', sxDisp).addIngredient('e', sxInc)
            .addIngredient('r', syDec).addIngredient('s', syDisp).addIngredient('t', syInc)
            .addIngredient('a', szDec).addIngredient('b', szDisp).addIngredient('c', szInc)
            .build();

        Window.single()
            .setViewer(player)
            .setTitle("Particle #" + particleIndex + " — " + pe.type().name())
            .setGui(gui)
            .build()
            .open();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void commit(ParticleDisplay display, ParticleEffect old,
                        Particle type, int count, Vector3f spreadOffset, double speed) {
        ParticleEffect updated = new ParticleEffect(type, count,
            new Vector3f(spreadOffset), speed, old.dustOptions());
        display.getParticles().set(particleIndex, updated);
        open();
    }

    private SimpleItem spreadField(String label, float current, Material mat,
                                   ParticleEffect pe, ParticleDisplay display,
                                   java.util.function.Function<Float, Vector3f> spreadBuilder) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.2f", current), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openFloatAnvil(label, current,
                v -> commit(display, pe, pe.type(), pe.count(), spreadBuilder.apply(v), pe.speed()),
                this::open)
        );
    }

    private ParticleDisplay fetchDisplay(AttackDevSession session) {
        if (session.getEditKeyframes() == null || kfIndex < 0 || kfIndex >= session.getEditKeyframes().size()) return null;
        var effect = session.getEditKeyframes().get(kfIndex).effect();
        if (effect == null || effect.displays() == null) return null;
        if (displayIndex < 0 || displayIndex >= effect.displays().size()) return null;
        return effect.displays().get(displayIndex);
    }

    private static SimpleItem dec(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("−", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("L small  R medium  Shift+L large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler);
    }

    private static SimpleItem inc(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("L small  R medium  Shift+L large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler);
    }

    private static int stepInt(Click click) {
        return switch (click.getClickType()) {
            case RIGHT -> 5;
            case SHIFT_LEFT -> 10;
            default -> 1;
        };
    }

    private static float stepFloat(Click click) {
        return switch (click.getClickType()) {
            case RIGHT -> 0.25f;
            case SHIFT_LEFT -> 1.0f;
            default -> 0.05f;
        };
    }
}
