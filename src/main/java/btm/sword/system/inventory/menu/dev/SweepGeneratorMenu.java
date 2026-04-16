package btm.sword.system.inventory.menu.dev;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;

import btm.sword.system.attack.dev.AttackDevSession;
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
 * Dev-only menu for adjusting recording session constants.
 *
 * <p>Opened when the player clicks the volume-attack wand item in their inventory.
 * Exposes four per-session parameters:</p>
 * <ul>
 *   <li><b>Tip Distance</b> — how far from the eye non-raycast points are placed.</li>
 *   <li><b>Raycast Dist</b> — maximum block-raycast range for RAYCAST placement mode.</li>
 *   <li><b>Placement Size</b> — uniform half-extent applied to each recorded keyframe.</li>
 *   <li><b>Duration</b> — attack duration written when the recording is saved.</li>
 *   <li><b>Ray Offset</b> — distance along look direction where the ray starts (negative = behind eye).</li>
 *   <li><b>Height Offset</b> — vertical shift of the eye start position for ORIGIN_RAY mode.</li>
 * </ul>
 *
 * <h2>Layout (3 rows × 9)</h2>
 * <pre>
 * Row 0: [T-] [T]  [T+] [R-] [R]  [R+] [S-] [S]  [S+]
 * Row 1: [D-] [D]  [D+] [O-] [O]  [O+] [H-] [H]  [H+]
 * Row 2: [#]  [#]  [#]  [#]  [#]  [#]  [#]  [#]  [#]
 * </pre>
 * <p>T=tipDistance, R=raycastDist, S=placementSize, D=duration, O=rayOriginOffset, H=heightOffset.</p>
 */
public class SweepGeneratorMenu extends Menu {

    /**
     * Creates the Recording Settings menu for the given player.
     *
     * @param player the player opening the menu
     */
    public SweepGeneratorMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());

        float tip = session.getTipDistance();
        float ray = session.getRaycastMaxDistance();
        float rayOff = session.getRaycastOriginOffset();
        float heightOff = session.getOriginRayHeightOffset();
        float size = session.getCurrentPlacementSize().x;
        int dur = session.getEditDurationMs();

        // ── Tip distance ─────────────────────────────────────────────────────
        SimpleItem tipDec = dec(click -> {
            session.setTipDistance(clampTip(tip - 0.1f));
            open();
        });
        SimpleItem tipDisplay = paramDisplay("Tip Dist", fmt1(tip),
            "Eye-to-tip distance for non-raycast modes (blocks).", Material.FEATHER);
        SimpleItem tipInc = inc(click -> {
            session.setTipDistance(clampTip(tip + 0.1f));
            open();
        });

        // ── Raycast distance ─────────────────────────────────────────────────
        SimpleItem rayDec = dec(click -> {
            session.setRaycastMaxDistance(clampRay(ray - 0.5f));
            open();
        });
        SimpleItem rayDisplay = paramDisplay("Raycast Dist", fmt1(ray),
            "Maximum block raycast range in RAYCAST mode (blocks).", Material.SPYGLASS);
        SimpleItem rayInc = inc(click -> {
            session.setRaycastMaxDistance(clampRay(ray + 0.5f));
            open();
        });

        // ── Placement size ───────────────────────────────────────────────────
        SimpleItem sizeDec = dec(click -> {
            float s = Math.max(0.05f, round2(size - 0.05f));
            session.getCurrentPlacementSize().set(s, s, s);
            open();
        });
        SimpleItem sizeDisplay = paramDisplay("Placement Size", fmt2(size),
            "Uniform half-extent applied to each recorded keyframe.", Material.PISTON);
        SimpleItem sizeInc = inc(click -> {
            float s = Math.min(3.0f, round2(size + 0.05f));
            session.getCurrentPlacementSize().set(s, s, s);
            open();
        });

        // ── Ray origin offset ─────────────────────────────────────────────────
        SimpleItem rayOffDec = dec(click -> {
            session.setRaycastOriginOffset(clampRayOff(rayOff - 0.1f));
            open();
        });
        SimpleItem rayOffDisplay = paramDisplay("Ray Offset", fmt1(rayOff),
            "Ray start offset along look direction (negative = behind eye).", Material.BLAZE_ROD);
        SimpleItem rayOffInc = inc(click -> {
            session.setRaycastOriginOffset(clampRayOff(rayOff + 0.1f));
            open();
        });

        // ── Duration ─────────────────────────────────────────────────────────
        SimpleItem durDec = dec(click -> {
            session.setEditDurationMs(Math.max(100, dur - 50));
            open();
        });
        SimpleItem durDisplay = paramDisplay("Duration", dur + " ms",
            "Attack duration written when the recording is saved.", Material.CLOCK);
        SimpleItem durInc = inc(click -> {
            session.setEditDurationMs(Math.min(3000, dur + 50));
            open();
        });

        // ── Height offset (ORIGIN_RAY) ────────────────────────────────────────
        SimpleItem heightOffDec = dec(click -> {
            session.setOriginRayHeightOffset(clampHeightOff(heightOff - 0.1f));
            open();
        });
        SimpleItem heightOffDisplay = paramDisplay("Height Offset", fmt1(heightOff),
            "Vertical eye offset for ORIGIN_RAY mode (negative = lower start).", Material.LADDER);
        SimpleItem heightOffInc = inc(click -> {
            session.setOriginRayHeightOffset(clampHeightOff(heightOff + 0.1f));
            open();
        });

        Gui gui = Gui.normal()
            .setStructure(
                "1 T 2 3 R 4 5 S 6",
                "7 D 8 9 O 0 A H B",
                "# # # # # # # # #"
            )
            .addIngredient('1', tipDec)
            .addIngredient('T', tipDisplay)
            .addIngredient('2', tipInc)
            .addIngredient('3', rayDec)
            .addIngredient('R', rayDisplay)
            .addIngredient('4', rayInc)
            .addIngredient('5', sizeDec)
            .addIngredient('S', sizeDisplay)
            .addIngredient('6', sizeInc)
            .addIngredient('7', durDec)
            .addIngredient('D', durDisplay)
            .addIngredient('8', durInc)
            .addIngredient('9', rayOffDec)
            .addIngredient('O', rayOffDisplay)
            .addIngredient('0', rayOffInc)
            .addIngredient('A', heightOffDec)
            .addIngredient('H', heightOffDisplay)
            .addIngredient('B', heightOffInc)
            .addIngredient('#', BORDER)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Recording Settings")
            .setGui(gui)
            .build()
            .open();
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

    private static SimpleItem paramDisplay(String label, String value, String description, Material material) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text(description, NamedTextColor.DARK_GRAY)))
                .build()
        );
    }

    // ── Clamp / format helpers ─────────────────────────────────────────────

    private static float clampTip(float v) { return Math.max(0.1f, Math.min(10.0f, round1(v))); }

    private static float clampRay(float v) { return Math.max(1.0f, Math.min(32.0f, round1(v))); }

    private static float clampRayOff(float v) { return Math.max(-5.0f, Math.min(5.0f, round1(v))); }

    private static float clampHeightOff(float v) { return Math.max(-3.0f, Math.min(3.0f, round1(v))); }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    private static float round2(float v) { return Math.round(v * 100f) / 100f; }

    private static String fmt1(float v) { return String.format("%.1f", v); }

    private static String fmt2(float v) { return String.format("%.2f", v); }
}
