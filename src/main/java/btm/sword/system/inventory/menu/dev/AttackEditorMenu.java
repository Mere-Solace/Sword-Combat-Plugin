package btm.sword.system.inventory.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.attack.def.AttackDef;
import btm.sword.system.attack.def.AttackDefSerializer;
import btm.sword.system.attack.def.AttackRegistry;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.item.ForwardItem;
import btm.sword.system.inventory.item.PreviousItem;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * In-game keyframe editor for VOLUME attacks.
 *
 * <p>Opens against the current {@link AttackDevSession} for the player, displaying all
 * keyframes as a paged list. Click a keyframe to select it; the bottom rows expose
 * manipulation buttons for the selected keyframe.</p>
 *
 * <h2>Layout (6 rows × 9)</h2>
 * <pre>
 * Row 0: Back | Dur- | Info | Dur+ | — | — | LoadWand | Save | —
 * Row 1: keyframe list (paged content) ×9
 * Row 2: keyframe list (paged content) ×9
 * Row 3: &lt; prev | next &gt; | — | Add | Delete | — | — | — | —
 * Row 4: Pos -X | +X | -Y | +Y | -Z | +Z | — | — | —
 * Row 5: HalfExt -X | +X | -Y | +Y | -Z | +Z | — | — | —
 * </pre>
 *
 * <p>Left-click adjustment buttons move by ±0.1 blocks/degrees;
 * right-click moves by ±0.5.</p>
 *
 * <p>{@link #save(AttackDevSession)} writes the current state to
 * {@code plugins/sword/attacks/<id>.yml} and updates {@link AttackRegistry}.</p>
 */
public class AttackEditorMenu extends Menu {

    private static final float STEP_SMALL = 0.1f;
    private static final float STEP_LARGE = 0.5f;

    /**
     * Creates an editor for the given player's current editing session.
     *
     * @param player the player owning the session
     */
    public AttackEditorMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        List<VolumeKeyframe> keyframes = session.getEditKeyframes();

        // ── Row 0 controls ────────────────────────────────────────────────────

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> {
                session.stopEditing();
                new AttackBrowserMenu(swordPlayer).open();
            }
        );

        SimpleItem durDecrease = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Duration −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −100ms  Right: −500ms", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                int delta = click.getClickType().isRightClick() ? 500 : 100;
                session.setEditDurationMs(Math.max(50, session.getEditDurationMs() - delta));
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        int durMs = session.getEditDurationMs();
        SimpleItem durInfo = new SimpleItem(
            new ItemStackBuilder(Material.CLOCK)
                .name(Component.text(session.getCurrentAttackName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Duration: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(durMs + "ms", NamedTextColor.YELLOW)),
                    Component.text("Frames: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(keyframes.size()), NamedTextColor.YELLOW)),
                    Component.text("Selected: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(
                            keyframes.isEmpty() ? "—" : String.valueOf(session.getCurrentKeyframeIndex()),
                            NamedTextColor.AQUA))
                ))
                .build()
        );

        SimpleItem durIncrease = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Duration +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +100ms  Right: +500ms", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                int delta = click.getClickType().isRightClick() ? 500 : 100;
                session.setEditDurationMs(session.getEditDurationMs() + delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem loadWand = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("Load Into Wand", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left-click with the volume wand", NamedTextColor.DARK_GRAY),
                    Component.text("will fire this attack.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                if (keyframes.isEmpty()) {
                    swordPlayer.message(Component.text("No keyframes to load.", NamedTextColor.RED));
                    return;
                }
                try {
                    session.loadIntoWand();
                    swordPlayer.message(Component.text(
                        "[Dev] Attack loaded into wand.", NamedTextColor.GREEN));
                } catch (IllegalStateException e) {
                    swordPlayer.message(Component.text("[Dev] " + e.getMessage(), NamedTextColor.RED));
                }
            }
        );

        SimpleItem saveButton = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Save", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("Save to attacks/<id>.yml", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (keyframes.isEmpty()) {
                    swordPlayer.message(Component.text("No keyframes to save.", NamedTextColor.RED));
                    return;
                }
                save(session);
            }
        );

        // ── Keyframe items (paged content) ────────────────────────────────────

        List<Item> kfItems = buildKeyframeItems(session);

        // ── Row 3 controls ────────────────────────────────────────────────────

        SimpleItem addFrame = new SimpleItem(
            new ItemStackBuilder(Material.LIME_TERRACOTTA)
                .name(Component.text("Add Keyframe", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Inserts a frame midway between", NamedTextColor.DARK_GRAY),
                    Component.text("the selected and next frame.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                insertFrameAfterSelected(session);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem deleteFrame = new SimpleItem(
            new ItemStackBuilder(Material.RED_TERRACOTTA)
                .name(Component.text("Delete Frame", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Removes the selected keyframe.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.size() <= 1) {
                    swordPlayer.message(Component.text("Cannot delete the last keyframe.", NamedTextColor.RED));
                    return;
                }
                kfs.remove(session.getCurrentKeyframeIndex());
                int newIdx = Math.min(session.getCurrentKeyframeIndex(), kfs.size() - 1);
                session.setCurrentKeyframeIndex(newIdx);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        VolumeShape currentShape = keyframes.isEmpty()
            ? VolumeShape.SPHERE
            : keyframes.get(session.getCurrentKeyframeIndex()).shape();
        SimpleItem shiftAllYDec = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Shift All Y −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −0.1  Right: −0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions down.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? -0.5f : -0.1f;
                shiftAllY(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllYInc = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Shift All Y +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +0.1  Right: +0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions up.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? 0.5f : 0.1f;
                shiftAllY(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem setShape = new SimpleItem(
            new ItemStackBuilder(Material.ENDER_EYE)
                .name(Component.text("Set Shape", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Current: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(currentShape.name(), NamedTextColor.AQUA)),
                    Component.text("Click to cycle shape.", NamedTextColor.YELLOW)
                ))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.isEmpty()) return;
                int idx = session.getCurrentKeyframeIndex();
                VolumeKeyframe kf = kfs.get(idx);
                VolumeShape[] shapes = VolumeShape.values();
                VolumeShape next = shapes[(kf.shape().ordinal() + 1) % shapes.length];
                kfs.set(idx, new VolumeKeyframe(kf.t(), kf.localPosition(), kf.halfExtents(), kf.rotation(), next));
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        // ── Row 4 — position nudge ─────────────────────────────────────────────

        SimpleItem posXDec = posButton(session, "Pos X −", NamedTextColor.RED, false, Axis.POS_X, true);
        SimpleItem posXInc = posButton(session, "Pos X +", NamedTextColor.GREEN, false, Axis.POS_X, false);
        SimpleItem posYDec = posButton(session, "Pos Y −", NamedTextColor.RED, false, Axis.POS_Y, true);
        SimpleItem posYInc = posButton(session, "Pos Y +", NamedTextColor.GREEN, false, Axis.POS_Y, false);
        SimpleItem posZDec = posButton(session, "Pos Z −", NamedTextColor.RED, false, Axis.POS_Z, true);
        SimpleItem posZInc = posButton(session, "Pos Z +", NamedTextColor.GREEN, false, Axis.POS_Z, false);

        // ── Row 5 — half-extents nudge ────────────────────────────────────────

        SimpleItem heXDec = posButton(session, "Half-X −", NamedTextColor.RED, true, Axis.POS_X, true);
        SimpleItem heXInc = posButton(session, "Half-X +", NamedTextColor.GREEN, true, Axis.POS_X, false);
        SimpleItem heYDec = posButton(session, "Half-Y −", NamedTextColor.RED, true, Axis.POS_Y, true);
        SimpleItem heYInc = posButton(session, "Half-Y +", NamedTextColor.GREEN, true, Axis.POS_Y, false);
        SimpleItem heZDec = posButton(session, "Half-Z −", NamedTextColor.RED, true, Axis.POS_Z, true);
        SimpleItem heZInc = posButton(session, "Half-Z +", NamedTextColor.GREEN, true, Axis.POS_Z, false);

        // ── Build GUI ─────────────────────────────────────────────────────────

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B # D I U # L S #",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< > # A F V J K #",
                "q w e r t y # # #",
                "u i o p g h # # #")
            .addIngredient('#', BORDER)
            .addIngredient('>', new ForwardItem())
            .addIngredient('<', new PreviousItem())
            .addIngredient('B', back)
            .addIngredient('D', durDecrease)
            .addIngredient('I', durInfo)
            .addIngredient('U', durIncrease)
            .addIngredient('L', loadWand)
            .addIngredient('S', saveButton)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('A', addFrame)
            .addIngredient('F', deleteFrame)
            .addIngredient('V', setShape)
            .addIngredient('J', shiftAllYDec)
            .addIngredient('K', shiftAllYInc)
            // pos row
            .addIngredient('q', posXDec)
            .addIngredient('w', posXInc)
            .addIngredient('e', posYDec)
            .addIngredient('r', posYInc)
            .addIngredient('t', posZDec)
            .addIngredient('y', posZInc)
            // half-extents row
            .addIngredient('u', heXDec)
            .addIngredient('i', heXInc)
            .addIngredient('o', heYDec)
            .addIngredient('p', heYInc)
            .addIngredient('g', heZDec)
            .addIngredient('h', heZInc)
            .setContent(kfItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Attack Editor — " + session.getCurrentAttackName())
            .setGui(gui)
            .build()
            .open();
    }

    // ── Keyframe list items ───────────────────────────────────────────────────

    private List<Item> buildKeyframeItems(AttackDevSession session) {
        List<VolumeKeyframe> keyframes = session.getEditKeyframes();
        int selectedIdx = session.getCurrentKeyframeIndex();
        List<Item> items = new ArrayList<>(keyframes.size());

        for (int i = 0; i < keyframes.size(); i++) {
            final int idx = i;
            VolumeKeyframe kf = keyframes.get(i);
            boolean selected = (idx == selectedIdx);

            Vector3f pos = kf.localPosition();
            Vector3f he = kf.halfExtents();

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(String.format("t = %.3f", kf.t()), NamedTextColor.AQUA));
            lore.add(Component.text("shape: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(kf.shape().name(), NamedTextColor.LIGHT_PURPLE)));
            lore.add(Component.text(
                String.format("pos:  x=%.2f  y=%.2f  z=%.2f", pos.x, pos.y, pos.z),
                NamedTextColor.GRAY));
            if (kf.shape() == VolumeShape.SPHERE) {
                lore.add(Component.text(String.format("radius: %.2f", he.x), NamedTextColor.GRAY));
            } else {
                lore.add(Component.text(
                    String.format("half: x=%.2f  y=%.2f  z=%.2f", he.x, he.y, he.z),
                    NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(selected
                ? Component.text("▶ Selected", NamedTextColor.GOLD, TextDecoration.BOLD)
                : Component.text("Click to select", NamedTextColor.YELLOW));

            Material mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
            items.add(new SimpleItem(
                new ItemStackBuilder(mat)
                    .name(Component.text("Frame " + idx, selected ? NamedTextColor.GOLD : NamedTextColor.WHITE,
                        TextDecoration.BOLD))
                    .lore(lore)
                    .build(),
                click -> {
                    session.setCurrentKeyframeIndex(idx);
                    new AttackEditorMenu(swordPlayer).open();
                }
            ));
        }
        return items;
    }

    // ── Adjustment button factory ─────────────────────────────────────────────

    /**
     * Creates a nudge button for position or half-extents on a given axis.
     *
     * @param session    the active editing session
     * @param label      display label
     * @param color      name colour
     * @param halfExtent {@code true} to adjust half-extents, {@code false} for position
     * @param axis       the axis to adjust
     * @param decrement  {@code true} to subtract, {@code false} to add
     */
    private SimpleItem posButton(AttackDevSession session, String label, NamedTextColor color,
            boolean halfExtent, Axis axis, boolean decrement) {
        return new SimpleItem(
            new ItemStackBuilder(halfExtent ? Material.ORANGE_DYE : Material.LIGHT_BLUE_DYE)
                .name(Component.text(label, color, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: ±0.1  R: ±0.5", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.isEmpty()) return;
                int idx = session.getCurrentKeyframeIndex();
                VolumeKeyframe kf = kfs.get(idx);
                float step = click.getClickType().isRightClick() ? STEP_LARGE : STEP_SMALL;
                float delta = decrement ? -step : step;
                kfs.set(idx, applyDelta(kf, halfExtent, axis, delta));
                new AttackEditorMenu(swordPlayer).open();
            }
        );
    }

    private enum Axis { POS_X, POS_Y, POS_Z }

    /**
     * Returns a new {@link VolumeKeyframe} with the given delta applied to the specified field.
     *
     * @param kf         the source keyframe
     * @param halfExtent {@code true} to modify half-extents, {@code false} for position
     * @param axis       the axis to modify
     * @param delta      amount to add
     * @return a new keyframe record with the updated value
     */
    private static VolumeKeyframe applyDelta(VolumeKeyframe kf, boolean halfExtent, Axis axis, float delta) {
        if (halfExtent) {
            Vector3f he = new Vector3f(kf.halfExtents());
            switch (axis) {
                case POS_X -> he.x = Math.max(0.05f, he.x + delta);
                case POS_Y -> he.y = Math.max(0.05f, he.y + delta);
                case POS_Z -> he.z = Math.max(0.05f, he.z + delta);
            }
            return new VolumeKeyframe(kf.t(), kf.localPosition(), he, kf.rotation(), kf.shape());
        } else {
            Vector3f pos = new Vector3f(kf.localPosition());
            switch (axis) {
                case POS_X -> pos.x += delta;
                case POS_Y -> pos.y += delta;
                case POS_Z -> pos.z += delta;
            }
            return new VolumeKeyframe(kf.t(), pos, kf.halfExtents(), kf.rotation(), kf.shape());
        }
    }

    // ── Add frame ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new keyframe after the currently selected one.
     * If the selected frame is the last, duplicates it with {@code t} advanced by 0.1
     * (capped at {@code 1.0}). Otherwise interpolates position, half-extents, and rotation
     * from the selected frame and its successor.
     *
     * @param session the active editing session
     */
    private static void insertFrameAfterSelected(AttackDevSession session) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs.isEmpty()) {
            kfs.add(new VolumeKeyframe(0f, new Vector3f(0f, 1f, 1f), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf(), VolumeShape.SPHERE));
            session.setCurrentKeyframeIndex(0);
            return;
        }

        int idx = session.getCurrentKeyframeIndex();
        VolumeKeyframe cur = kfs.get(idx);

        VolumeKeyframe newFrame;
        if (idx < kfs.size() - 1) {
            VolumeKeyframe next = kfs.get(idx + 1);
            float t = (cur.t() + next.t()) / 2f;
            Vector3f pos = new Vector3f(cur.localPosition()).lerp(next.localPosition(), 0.5f);
            Vector3f he = new Vector3f(cur.halfExtents()).lerp(next.halfExtents(), 0.5f);
            Quaternionf rot = new Quaternionf(cur.rotation()).slerp(next.rotation(), 0.5f);
            newFrame = new VolumeKeyframe(t, pos, he, rot, cur.shape());
        } else {
            float t = Math.min(1.0f, cur.t() + 0.1f);
            newFrame = new VolumeKeyframe(t,
                new Vector3f(cur.localPosition()),
                new Vector3f(cur.halfExtents()),
                new Quaternionf(cur.rotation()),
                cur.shape());
        }

        kfs.add(idx + 1, newFrame);
        session.setCurrentKeyframeIndex(idx + 1);
    }

    // ── Shift All Y ───────────────────────────────────────────────────────────

    /**
     * Applies a Y-axis delta to every keyframe's local position.
     *
     * @param session the active editing session
     * @param delta   amount to add to each keyframe's {@code localPosition.y}
     */
    private static void shiftAllY(AttackDevSession session, float delta) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        for (int i = 0; i < kfs.size(); i++) {
            VolumeKeyframe kf = kfs.get(i);
            Vector3f pos = new Vector3f(kf.localPosition());
            pos.y += delta;
            kfs.set(i, new VolumeKeyframe(kf.t(), pos, kf.halfExtents(), kf.rotation(), kf.shape()));
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Builds the current attack, registers it in {@link AttackRegistry}, and writes it to
     * {@code plugins/sword/attacks/<id>.yml}.
     *
     * @param session the active editing session
     */
    private void save(AttackDevSession session) {
        try {
            AttackDef def = session.buildCurrentAttack();
            AttackRegistry.register(def);

            File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
            attacksDir.mkdirs();
            File file = new File(attacksDir, def.getId() + ".yml");
            AttackDefSerializer.save(file, def);

            swordPlayer.message(Component.text(
                "[Dev] Saved '" + def.getId() + "' → attacks/" + def.getId() + ".yml",
                NamedTextColor.AQUA));
        } catch (Exception e) {
            swordPlayer.message(Component.text("[Dev] Save failed: " + e.getMessage(), NamedTextColor.RED));
        }
    }
}
