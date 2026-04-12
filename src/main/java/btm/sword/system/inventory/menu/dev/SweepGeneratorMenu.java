package btm.sword.system.inventory.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.dev.DevMode;
import btm.sword.system.attack.dev.SweepRecordingAction;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.entity.impl.SwordPlayer;
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
 * Dev-only menu for procedurally generating a series of OBB keyframes along the player's
 * forward (local Z) axis.
 *
 * <p>Provides seven configurable parameters: keyframe count, start distance, inter-frame
 * spacing, base half-extent, multiplicative scale-per-step, additive scale-per-step, and
 * attack duration. Clicking "Generate" inserts the resulting keyframes into the current
 * {@link AttackDevSession} and opens {@link AttackEditorMenu} for fine-tuning.</p>
 *
 * <h2>Layout (3 rows × 9)</h2>
 * <pre>
 * Row 0: [N-] [N]  [N+] [D-] [D]  [D+] [S-] [S]  [S+]
 * Row 1: [B-] [B]  [B+] [M-] [M]  [M+] [A-] [A]  [A+]
 * Row 2: [u-] [u]  [u+] [#]  [#]  [#]  [#]  [G]  [#]
 * </pre>
 * <p>N=keyframe count, D=startDist, S=spacing, B=baseSize, M=multScale, A=addScale,
 * u=duration, G=Generate.</p>
 *
 * <h2>Temporal behaviour</h2>
 * <p>Generated frames are placed at evenly-spaced {@code t} values. The
 * {@code VolumeSimulation} (200 Hz) interpolates position via Catmull-Rom between adjacent
 * keyframes, so the hitbox travels from the nearest frame to the farthest over the full
 * {@code durationMs}. No immediate between-OBB detection is used — the temporal sweep is
 * already precise enough at 200 Hz.</p>
 *
 * <p>Trigger: open player inventory and click the volume-attack wand item.</p>
 */
public class SweepGeneratorMenu extends Menu {

    /** Y offset from bounding-box centre to approximate eye level. */
    private static final float EYE_Y_OFFSET = 0.6f;

    // ── Parameters ──────────────────────────────────────────────────────────
    private final int n;
    private final float startDist;
    private final float spacing;
    private final float baseSize;
    private final float multScale;
    private final float addScale;
    private final int durationMs;

    /**
     * Creates the menu with default parameters.
     *
     * @param player the player opening the menu
     */
    public SweepGeneratorMenu(SwordPlayer player) {
        this(player, 4, 1.0f, 0.75f, 0.5f, 1.0f, 0.0f, 500);
    }

    /**
     * Creates the menu with explicit parameters (used when refreshing after a +/- click).
     *
     * @param player     the player opening the menu
     * @param n          keyframe count (2–16)
     * @param startDist  distance to first frame (0.25–10.0)
     * @param spacing    distance between frames (0.1–5.0)
     * @param baseSize   initial half-extent X/Y (0.05–3.0)
     * @param multScale  size multiplier per step (0.5–3.0)
     * @param addScale   additive size per step (-0.5–2.0)
     * @param durationMs attack duration in milliseconds (100–3000)
     */
    public SweepGeneratorMenu(SwordPlayer player, int n, float startDist, float spacing,
            float baseSize, float multScale, float addScale, int durationMs) {
        super(player);
        this.n = n;
        this.startDist = startDist;
        this.spacing = spacing;
        this.baseSize = baseSize;
        this.multScale = multScale;
        this.addScale = addScale;
        this.durationMs = durationMs;
    }

    @Override
    public void open() {
        // ── Row 0: keyframe count, startDist, spacing ────────────────────────

        SimpleItem nDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, clampN(n - 1), startDist, spacing,
                baseSize, multScale, addScale, durationMs).open());
        SimpleItem nDisplay = paramDisplay("Keyframes", String.valueOf(n),
            "Number of OBB keyframes to generate.");
        SimpleItem nInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, clampN(n + 1), startDist, spacing,
                baseSize, multScale, addScale, durationMs).open());

        SimpleItem distDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, clampDist(startDist - 0.25f), spacing,
                baseSize, multScale, addScale, durationMs).open());
        SimpleItem distDisplay = paramDisplay("Start Dist", fmt1(startDist),
            "Z offset of the first frame from the player's body centre.");
        SimpleItem distInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, clampDist(startDist + 0.25f), spacing,
                baseSize, multScale, addScale, durationMs).open());

        SimpleItem spacingDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, clampSpacing(spacing - 0.1f),
                baseSize, multScale, addScale, durationMs).open());
        SimpleItem spacingDisplay = paramDisplay("Spacing", fmt1(spacing),
            "Z distance between consecutive keyframes.");
        SimpleItem spacingInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, clampSpacing(spacing + 0.1f),
                baseSize, multScale, addScale, durationMs).open());

        // ── Row 1: baseSize, multScale, addScale ─────────────────────────────

        SimpleItem sizeDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                clampSize(baseSize - 0.05f), multScale, addScale, durationMs).open());
        SimpleItem sizeDisplay = paramDisplay("Base Size", fmt2(baseSize),
            "Initial half-extent (X and Y) of the first frame.");
        SimpleItem sizeInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                clampSize(baseSize + 0.05f), multScale, addScale, durationMs).open());

        SimpleItem multDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, clampMult(multScale - 0.1f), addScale, durationMs).open());
        SimpleItem multDisplay = paramDisplay("Mult Scale", fmt2(multScale),
            "Size multiplier per step — >1 widens frames toward the far end.");
        SimpleItem multInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, clampMult(multScale + 0.1f), addScale, durationMs).open());

        SimpleItem addDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, multScale, clampAdd(addScale - 0.05f), durationMs).open());
        SimpleItem addDisplay = paramDisplay("Add Scale", fmt2(addScale),
            "Flat half-extent added each step.");
        SimpleItem addInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, multScale, clampAdd(addScale + 0.05f), durationMs).open());

        // ── Row 2: duration, generate ─────────────────────────────────────────

        SimpleItem durDec = dec(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, multScale, addScale, clampDur(durationMs - 50)).open());
        SimpleItem durDisplay = paramDisplay("Duration", durationMs + " ms",
            "Total attack duration. Shorter = faster sweep.");
        SimpleItem durInc = inc(click ->
            new SweepGeneratorMenu(swordPlayer, n, startDist, spacing,
                baseSize, multScale, addScale, clampDur(durationMs + 50)).open());

        SimpleItem generate = new SimpleItem(
            new ItemStackBuilder(Material.EMERALD)
                .name(Component.text("Generate", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Creates " + n + " keyframes along the", NamedTextColor.DARK_GRAY),
                    Component.text("forward axis and opens the editor.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> generate()
        );

        Gui gui = Gui.normal()
            .setStructure(
                "1 N 2 3 E 4 5 S 6",
                "7 B 8 9 M 0 a A b",
                "c D d # # # # G #"
            )
            .addIngredient('1', nDec)
            .addIngredient('N', nDisplay)
            .addIngredient('2', nInc)
            .addIngredient('3', distDec)
            .addIngredient('E', distDisplay)
            .addIngredient('4', distInc)
            .addIngredient('5', spacingDec)
            .addIngredient('S', spacingDisplay)
            .addIngredient('6', spacingInc)
            .addIngredient('7', sizeDec)
            .addIngredient('B', sizeDisplay)
            .addIngredient('8', sizeInc)
            .addIngredient('9', multDec)
            .addIngredient('M', multDisplay)
            .addIngredient('0', multInc)
            .addIngredient('a', addDec)
            .addIngredient('A', addDisplay)
            .addIngredient('b', addInc)
            .addIngredient('c', durDec)
            .addIngredient('D', durDisplay)
            .addIngredient('d', durInc)
            .addIngredient('#', BORDER)
            .addIngredient('G', generate)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Sweep Generator")
            .setGui(gui)
            .build()
            .open();
    }

    // ── Generation ─────────────────────────────────────────────────────────

    private void generate() {
        List<VolumeKeyframe> frames = buildFrames();
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());

        if (session.getMode() == DevMode.EDITING) {
            session.setEditKeyframes(frames);
            session.setEditDurationMs(durationMs);
            session.loadIntoWand();
            new AttackEditorMenu(swordPlayer).open();
            return;
        }

        if (session.getMode() == DevMode.VIEWING) {
            session.stopViewing();
        }

        File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
        attacksDir.mkdirs();
        String name = SweepRecordingAction.nextAvailableName("sweep_gen", attacksDir);
        session.startEditing(name, frames, durationMs, null);
        session.loadIntoWand();
        new AttackEditorMenu(swordPlayer).open();
    }

    private List<VolumeKeyframe> buildFrames() {
        List<VolumeKeyframe> frames = new ArrayList<>(n);
        float zHalfExt = spacing / 2f;
        float sizeMult = 1f;
        for (int i = 0; i < n; i++) {
            float t = (n == 1) ? 0f : (float) i / (n - 1);
            float z = startDist + i * spacing;
            float rawSize = baseSize * sizeMult + addScale * i;
            float he = Math.max(0.05f, rawSize);
            frames.add(new VolumeKeyframe(
                t,
                new Vector3f(0f, EYE_Y_OFFSET, z),
                new Vector3f(he, he, zHalfExt),
                new Quaternionf(),
                VolumeShape.OBB,
                null
            ));
            sizeMult *= multScale;
        }
        return frames;
    }

    // ── Item helpers ───────────────────────────────────────────────────────

    private static SimpleItem dec(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("−", NamedTextColor.RED, TextDecoration.BOLD))
                .build(),
            handler
        );
    }

    private static SimpleItem inc(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
                .build(),
            handler
        );
    }

    private static SimpleItem paramDisplay(String label, String value, String description) {
        return new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text(description, NamedTextColor.DARK_GRAY)))
                .build()
        );
    }

    // ── Clamp helpers ──────────────────────────────────────────────────────

    private static int clampN(int v) { return Math.max(2, Math.min(16, v)); }

    private static float clampDist(float v) { return Math.max(0.25f, Math.min(10.0f, round1(v))); }

    private static float clampSpacing(float v) { return Math.max(0.1f, Math.min(5.0f, round1(v))); }

    private static float clampSize(float v) { return Math.max(0.05f, Math.min(3.0f, round2(v))); }

    private static float clampMult(float v) { return Math.max(0.5f, Math.min(3.0f, round2(v))); }

    private static float clampAdd(float v) { return Math.max(-0.5f, Math.min(2.0f, round2(v))); }

    private static int clampDur(int v) { return Math.max(100, Math.min(3000, v)); }

    /** Rounds to 1 decimal place to avoid floating-point accumulation. */
    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    /** Rounds to 2 decimal places. */
    private static float round2(float v) { return Math.round(v * 100f) / 100f; }

    private static String fmt1(float v) { return String.format("%.1f", v); }

    private static String fmt2(float v) { return String.format("%.2f", v); }
}
