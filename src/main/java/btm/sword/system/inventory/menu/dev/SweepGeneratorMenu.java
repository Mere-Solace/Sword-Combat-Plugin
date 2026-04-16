package btm.sword.system.inventory.menu.dev;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.Sword;
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
import xyz.xenondevs.invui.window.AnvilWindow;
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
 *   <li><b>Ray Offset</b> — minimum distance from origin for ORIGIN_RAY mode (negative = behind eye for RAYCAST).</li>
 *   <li><b>Height Offset</b> — vertical shift of the eye start position (legacy, kept for config).</li>
 * </ul>
 *
 * <h2>Layout (3 rows × 9)</h2>
 * <pre>
 * Row 0: [T-] [T]  [T+] [R-] [R]  [R+] [S-] [S]  [S+]
 * Row 1: [D-] [D]  [D+] [O-] [O]  [O+] [H-] [H]  [H+]
 * Row 2: [#]  [#]  [#]  [#]  [#]  [#]  [#]  [#]  [#]
 * </pre>
 * <p>T=tipDistance, R=raycastDist, S=placementSize, D=duration, O=rayOriginOffset, H=heightOffset.</p>
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li>Dec/Inc buttons: Left-click = ±small, Right-click = ±medium, Shift+Left = ±large.</li>
 *   <li>Display item click: opens Anvil dialog for direct numeric input.</li>
 * </ul>
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
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setTipDistance(clampTip(tip - delta));
            open();
        });
        SimpleItem tipDisplay = paramDisplay("Tip Dist", fmt1(tip),
            "Eye-to-tip distance for non-raycast modes (blocks).", Material.FEATHER,
            v -> { session.setTipDistance(clampTip(v)); open(); });
        SimpleItem tipInc = inc(click -> {
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setTipDistance(clampTip(tip + delta));
            open();
        });

        // ── Raycast distance ─────────────────────────────────────────────────
        SimpleItem rayDec = dec(click -> {
            float delta = stepFloat(click, 0.5f, 2.0f, 5.0f);
            session.setRaycastMaxDistance(clampRay(ray - delta));
            open();
        });
        SimpleItem rayDisplay = paramDisplay("Raycast Dist", fmt1(ray),
            "Maximum block raycast range in RAYCAST mode (blocks).", Material.SPYGLASS,
            v -> { session.setRaycastMaxDistance(clampRay(v)); open(); });
        SimpleItem rayInc = inc(click -> {
            float delta = stepFloat(click, 0.5f, 2.0f, 5.0f);
            session.setRaycastMaxDistance(clampRay(ray + delta));
            open();
        });

        // ── Placement size ───────────────────────────────────────────────────
        SimpleItem sizeDec = dec(click -> {
            float delta = stepFloat(click, 0.05f, 0.25f, 1.0f);
            float s = Math.max(0.05f, round2(size - delta));
            session.getCurrentPlacementSize().set(s, s, s);
            open();
        });
        SimpleItem sizeDisplay = paramDisplay("Placement Size", fmt2(size),
            "Uniform half-extent applied to each recorded keyframe.", Material.PISTON,
            v -> {
                float s = Math.max(0.05f, Math.min(3.0f, round2(v)));
                session.getCurrentPlacementSize().set(s, s, s);
                open();
            });
        SimpleItem sizeInc = inc(click -> {
            float delta = stepFloat(click, 0.05f, 0.25f, 1.0f);
            float s = Math.min(3.0f, round2(size + delta));
            session.getCurrentPlacementSize().set(s, s, s);
            open();
        });

        // ── Ray origin offset ─────────────────────────────────────────────────
        SimpleItem rayOffDec = dec(click -> {
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setRaycastOriginOffset(clampRayOff(rayOff - delta));
            open();
        });
        SimpleItem rayOffDisplay = paramDisplay("Ray Offset", fmt1(rayOff),
            "Min dist from origin (ORIGIN_RAY) / ray start offset along look dir (RAYCAST).", Material.BLAZE_ROD,
            v -> { session.setRaycastOriginOffset(clampRayOff(v)); open(); });
        SimpleItem rayOffInc = inc(click -> {
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setRaycastOriginOffset(clampRayOff(rayOff + delta));
            open();
        });

        // ── Duration ─────────────────────────────────────────────────────────
        SimpleItem durDec = dec(click -> {
            int delta = stepInt(click, 50, 100, 500);
            session.setEditDurationMs(Math.max(100, dur - delta));
            open();
        });
        SimpleItem durDisplay = paramDisplayInt("Duration", dur + " ms",
            "Attack duration written when the recording is saved.", Material.CLOCK,
            v -> { session.setEditDurationMs(Math.max(100, Math.min(3000, v))); open(); });
        SimpleItem durInc = inc(click -> {
            int delta = stepInt(click, 50, 100, 500);
            session.setEditDurationMs(Math.min(3000, dur + delta));
            open();
        });

        // ── Height offset (ORIGIN_RAY) ────────────────────────────────────────
        SimpleItem heightOffDec = dec(click -> {
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setOriginRayHeightOffset(clampHeightOff(heightOff - delta));
            open();
        });
        SimpleItem heightOffDisplay = paramDisplay("Height Offset", fmt1(heightOff),
            "Vertical eye offset for ORIGIN_RAY mode (legacy, kept for config).", Material.LADDER,
            v -> { session.setOriginRayHeightOffset(clampHeightOff(v)); open(); });
        SimpleItem heightOffInc = inc(click -> {
            float delta = stepFloat(click, 0.1f, 0.5f, 2.0f);
            session.setOriginRayHeightOffset(clampHeightOff(heightOff + delta));
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

    // ── Anvil dialog ───────────────────────────────────────────────────────────

    /**
     * Opens an InvUI AnvilWindow that lets the player type a float value directly.
     * On confirm the value is passed to {@code onInput}; the main menu reopens afterward.
     */
    private void openAnvilDialog(String label, String currentVal, Consumer<Float> onInput) {
        ItemStack inputItem = new ItemStackBuilder(Material.PAPER)
            .name(Component.text(currentVal, NamedTextColor.WHITE))
            .build();
        Gui gui = Gui.normal()
            .setStructure("X # #")
            .addIngredient('X', new SimpleItem(inputItem))
            .addIngredient('#', BORDER)
            .build();
        AnvilWindow.single()
            .setViewer(swordPlayer.player())
            .setTitle(label)
            .setGui(gui)
            .addRenameHandler(text -> {
                try {
                    float val = Float.parseFloat(text.trim());
                    onInput.accept(val);
                } catch (NumberFormatException ignored) {
                    // invalid input — just reopen
                }
                Bukkit.getScheduler().runTask(Sword.getInstance(), this::open);
            })
            .build()
            .open();
    }

    /** Opens an Anvil dialog for integer input (e.g. duration). */
    private void openAnvilDialogInt(String label, String currentVal, Consumer<Integer> onInput) {
        openAnvilDialog(label, currentVal, v -> onInput.accept(Math.round(v)));
    }

    // ── Item helpers ───────────────────────────────────────────────────────────

    private static SimpleItem dec(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("−", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −small  Right: −medium  Shift+Left: −large", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            handler
        );
    }

    private static SimpleItem inc(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +small  Right: +medium  Shift+Left: +large", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            handler
        );
    }

    private SimpleItem paramDisplay(String label, String value, String description,
                                    Material material, Consumer<Float> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text(description, NamedTextColor.DARK_GRAY),
                    Component.text("Click to type a value directly.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> openAnvilDialog(label, value, onInput)
        );
    }

    private SimpleItem paramDisplayInt(String label, String value, String description,
                                       Material material, Consumer<Integer> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(material)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value, NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text(description, NamedTextColor.DARK_GRAY),
                    Component.text("Click to type a value directly.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> openAnvilDialogInt(label, value, onInput)
        );
    }

    // ── Step-size helpers ──────────────────────────────────────────────────────

    /** Returns the delta for a float parameter based on click type. */
    private static float stepFloat(Click click, float small, float medium, float large) {
        return switch (click.getClickType()) {
            case RIGHT -> medium;
            case SHIFT_LEFT -> large;
            default -> small;
        };
    }

    /** Returns the delta for an int parameter based on click type. */
    private static int stepInt(Click click, int small, int medium, int large) {
        return switch (click.getClickType()) {
            case RIGHT -> medium;
            case SHIFT_LEFT -> large;
            default -> small;
        };
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
