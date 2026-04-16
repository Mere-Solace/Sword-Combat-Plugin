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
 * </ul>
 *
 * <h2>Layout (2 rows × 9)</h2>
 * <pre>
 * Row 0: [T-] [T]  [T+] [R-] [R]  [R+] [S-] [S]  [S+]
 * Row 1: [D-] [D]  [D+] [#]  [#]  [#]  [#]  [#]  [#]
 * </pre>
 * <p>T=tipDistance, R=raycastDist, S=placementSize, D=duration.</p>
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
        float size = session.getCurrentPlacementSize().x;
        int dur = session.getEditDurationMs();

        // ── Tip distance ─────────────────────────────────────────────────────
        SimpleItem tipDec = dec(click -> {
            session.setTipDistance(clampTip(tip - 0.1f));
            open();
        });
        SimpleItem tipDisplay = paramDisplay("Tip Dist", fmt1(tip),
            "Eye-to-tip distance for non-raycast modes (blocks).");
        SimpleItem tipInc = dec(click -> {
            session.setTipDistance(clampTip(tip + 0.1f));
            open();
        });

        // ── Raycast distance ─────────────────────────────────────────────────
        SimpleItem rayDec = dec(click -> {
            session.setRaycastMaxDistance(clampRay(ray - 0.5f));
            open();
        });
        SimpleItem rayDisplay = paramDisplay("Raycast Dist", fmt1(ray),
            "Maximum block raycast range in RAYCAST mode (blocks).");
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
            "Uniform half-extent applied to each recorded keyframe.");
        SimpleItem sizeInc = inc(click -> {
            float s = Math.min(3.0f, round2(size + 0.05f));
            session.getCurrentPlacementSize().set(s, s, s);
            open();
        });

        // ── Duration ─────────────────────────────────────────────────────────
        SimpleItem durDec = dec(click -> {
            session.setEditDurationMs(Math.max(100, dur - 50));
            open();
        });
        SimpleItem durDisplay = paramDisplay("Duration", dur + " ms",
            "Attack duration written when the recording is saved.");
        SimpleItem durInc = inc(click -> {
            session.setEditDurationMs(Math.min(3000, dur + 50));
            open();
        });

        Gui gui = Gui.normal()
            .setStructure(
                "1 T 2 3 R 4 5 S 6",
                "7 D 8 # # # # # #"
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

    private static SimpleItem paramDisplay(String label, String value, String description) {
        return new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text(description, NamedTextColor.DARK_GRAY)))
                .build()
        );
    }

    // ── Clamp / format helpers ─────────────────────────────────────────────

    private static float clampTip(float v) { return Math.max(0.1f, Math.min(10.0f, round1(v))); }

    private static float clampRay(float v) { return Math.max(1.0f, Math.min(32.0f, round1(v))); }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    private static float round2(float v) { return Math.round(v * 100f) / 100f; }

    private static String fmt1(float v) { return String.format("%.1f", v); }

    private static String fmt2(float v) { return String.format("%.2f", v); }
}
