package btm.sword.system.attack.dev;

import java.util.List;

import org.joml.Vector3f;

import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.DevSwordPlayer;
import btm.sword.system.input.InputType;
import btm.sword.system.inventory.menu.dev.AttackEditorMenu;

/**
 * Routes player inputs to the correct hotbar-tool action while
 * {@link AnimationMode} is active on a {@link DevSwordPlayer}.
 *
 * <p>Called by {@link DevSwordPlayer#act(InputType)} when
 * {@link DevSwordPlayer#isInAnimationMode()} is {@code true}. Dispatch is based
 * on the slot currently held by the player; each slot corresponds to a specific
 * editing action defined in issue #349.</p>
 *
 * <h2>Hotbar layout</h2>
 * <pre>
 * Slot 0  ARROW           — select previous keyframe
 * Slot 1  ARROW           — select next keyframe
 * Slot 2  LIGHT_BLUE_DYE  — nudge position X   (L: −0.1, R: +0.1, Shift: ±0.5)
 * Slot 3  GREEN_DYE       — nudge position Y
 * Slot 4  CYAN_DYE        — nudge position Z
 * Slot 5  ORANGE_DYE      — nudge half-extents / sphere radius (L: −0.05, R: +0.05, Drop: −0.25, Swap: +0.25)
 * Slot 6  ENDER_EYE       — cycle shape (OBB → SPHERE → …)
 * Slot 7  CLOCK           — play / pause animated preview
 * Slot 8  WRITABLE_BOOK   — open AttackEditorMenu (remains in AnimationMode)
 * </pre>
 */
public final class AnimationModeInputHandler {

    /** Preview auto-advance interval in milliseconds. */
    private static final int PREVIEW_ADVANCE_MS = 200;

    private AnimationModeInputHandler() {}

    /**
     * Dispatches an input event to the appropriate hotbar-tool handler.
     * Does nothing if there is no active {@link AttackDevSession} or the session
     * is not in {@link DevMode#EDITING} mode.
     *
     * @param player the {@link DevSwordPlayer} receiving the input
     * @param input  the input type fired by the player
     */
    public static void handle(DevSwordPlayer player, InputType input) {
        int slot = player.player().getInventory().getHeldItemSlot();
        AttackDevSession session = AttackDevSession.get(player.player().getUniqueId());
        if (session == null || session.getMode() != DevMode.EDITING) return;

        switch (slot) {
            case 0 -> handlePrevKeyframe(session, input);
            case 1 -> handleNextKeyframe(session, input);
            case 2 -> handleNudgePos(session, input, Axis.X);
            case 3 -> handleNudgePos(session, input, Axis.Y);
            case 4 -> handleNudgePos(session, input, Axis.Z);
            case 5 -> handleNudgeExtents(session, input);
            case 6 -> handleCycleShape(session, input);
            case 7 -> handlePlayPause(player, session, input);
            case 8 -> handleOpenEditor(player, input);
            default -> { /* slots outside 0–8 are not animation tools */ }
        }
    }

    // ── Slot handlers ─────────────────────────────────────────────────────────

    /**
     * Selects the previous keyframe, wrapping around to the last when at index 0.
     */
    private static void handlePrevKeyframe(AttackDevSession session, InputType input) {
        if (input != InputType.LEFT && input != InputType.RIGHT) return;
        int size = session.getEditKeyframes().size();
        if (size == 0) return;
        int idx = (session.getCurrentKeyframeIndex() - 1 + size) % size;
        session.setCurrentKeyframeIndex(idx);
    }

    /**
     * Selects the next keyframe, wrapping around to index 0 from the last.
     */
    private static void handleNextKeyframe(AttackDevSession session, InputType input) {
        if (input != InputType.LEFT && input != InputType.RIGHT) return;
        int size = session.getEditKeyframes().size();
        if (size == 0) return;
        int idx = (session.getCurrentKeyframeIndex() + 1) % size;
        session.setCurrentKeyframeIndex(idx);
    }

    /**
     * Nudges the selected keyframe's position on the given axis.
     * L = −small, R = +small, Shift+L = −large, Shift+R = +large.
     */
    private static void handleNudgePos(AttackDevSession session, InputType input, Axis axis) {
        float delta = nudgeDelta(input, 0.1f, 0.5f);
        if (delta == 0f) return;
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs.isEmpty()) return;
        int idx = session.getCurrentKeyframeIndex();
        VolumeKeyframe kf = kfs.get(idx);
        Vector3f pos = new Vector3f(kf.localPosition());
        switch (axis) {
            case X -> pos.x += delta;
            case Y -> pos.y += delta;
            case Z -> pos.z += delta;
        }
        kfs.set(idx, new VolumeKeyframe(kf.t(), pos, kf.halfExtents(), kf.rotation(), kf.shape(), kf.effect(), kf.jump(), kf.linearToNext(), kf.keyframeType()));
        session.setEditCount(session.getEditCount() + 1);
    }

    /**
     * Nudges the selected keyframe's half-extents.
     * For {@link VolumeShape#SPHERE} only the radius (x) is changed.
     * For {@link VolumeShape#OBB} all three axes are adjusted equally.
     */
    private static void handleNudgeExtents(AttackDevSession session, InputType input) {
        float delta = nudgeDelta(input, 0.05f, 0.25f);
        if (delta == 0f) return;
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs.isEmpty()) return;
        int idx = session.getCurrentKeyframeIndex();
        VolumeKeyframe kf = kfs.get(idx);
        Vector3f he = new Vector3f(kf.halfExtents());
        if (kf.shape() == VolumeShape.SPHERE) {
            he.x = Math.max(0.05f, he.x + delta);
        } else {
            he.x = Math.max(0.05f, he.x + delta);
            he.y = Math.max(0.05f, he.y + delta);
            he.z = Math.max(0.05f, he.z + delta);
        }
        kfs.set(idx, new VolumeKeyframe(kf.t(), kf.localPosition(), he, kf.rotation(), kf.shape(), kf.effect(), kf.jump(), kf.linearToNext(), kf.keyframeType()));
        session.setEditCount(session.getEditCount() + 1);
    }

    /**
     * Advances the selected keyframe's shape to the next {@link VolumeShape} enum value.
     */
    private static void handleCycleShape(AttackDevSession session, InputType input) {
        if (input != InputType.LEFT && input != InputType.RIGHT) return;
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs.isEmpty()) return;
        int idx = session.getCurrentKeyframeIndex();
        VolumeKeyframe kf = kfs.get(idx);
        VolumeShape[] shapes = VolumeShape.values();
        VolumeShape next = shapes[(kf.shape().ordinal() + 1) % shapes.length];
        kfs.set(idx, new VolumeKeyframe(kf.t(), kf.localPosition(), kf.halfExtents(), kf.rotation(), next, kf.effect(), kf.jump(), kf.linearToNext(), kf.keyframeType()));
        session.setEditCount(session.getEditCount() + 1);
    }

    /**
     * Toggles animated preview playback. When playing, a repeating task advances
     * {@link AttackDevSession#getCurrentKeyframeIndex()} by 1 every
     * {@value #PREVIEW_ADVANCE_MS} ms, looping. The task cancels itself when the
     * preview flag is cleared or the session leaves EDITING mode.
     */
    private static void handlePlayPause(DevSwordPlayer player, AttackDevSession session, InputType input) {
        if (input != InputType.LEFT && input != InputType.RIGHT) return;
        boolean nowPlaying = !session.isPlayingPreview();
        session.setPlayingPreview(nowPlaying);
        if (!nowPlaying) return;

        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                int size = session.getEditKeyframes().size();
                if (size == 0) return;
                session.setCurrentKeyframeIndex((session.getCurrentKeyframeIndex() + 1) % size);
            },
            null,
            0, PREVIEW_ADVANCE_MS,
            AnimationModeInputHandler.class, "previewLoop",
            new btm.sword.system.control.PredicateRunnablePair(
                () -> !session.isPlayingPreview() || session.getMode() != DevMode.EDITING,
                () -> session.setPlayingPreview(false))
        );
    }

    /**
     * Opens {@link AttackEditorMenu} without exiting AnimationMode.
     *
     * @param player the player holding the WRITABLE_BOOK in slot 8
     * @param input  the triggering input; only left/right-click are handled
     */
    private static void handleOpenEditor(DevSwordPlayer player, InputType input) {
        if (input == InputType.LEFT || input == InputType.RIGHT) {
            new AttackEditorMenu(player).open();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private enum Axis { X, Y, Z }

    /**
     * Maps an input type to a signed nudge delta.
     *
     * <ul>
     *   <li>LEFT  → −small</li>
     *   <li>RIGHT → +small</li>
     *   <li>SWAP  → +large</li>
     *   <li>DROP  → −large</li>
     * </ul>
     *
     * @param input the input type to map
     * @param small magnitude for LEFT / RIGHT
     * @param large magnitude for SWAP / DROP
     * @return positive or negative delta, or {@code 0f} for unrecognised inputs
     */
    private static float nudgeDelta(InputType input, float small, float large) {
        return switch (input) {
            case LEFT -> -small;
            case RIGHT -> small;
            case SWAP -> large;
            case DROP -> -large;
            default -> 0f;
        };
    }
}
