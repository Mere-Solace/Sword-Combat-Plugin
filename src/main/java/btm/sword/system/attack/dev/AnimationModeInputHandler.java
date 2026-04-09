package btm.sword.system.attack.dev;

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
 * Slot 5  ORANGE_DYE      — nudge half-extents / sphere radius (L: −0.05, R: +0.05)
 * Slot 6  ENDER_EYE       — cycle shape (OBB → SPHERE → …)
 * Slot 7  CLOCK           — play / pause animated preview
 * Slot 8  WRITABLE_BOOK   — open AttackEditorMenu (remains in AnimationMode)
 * </pre>
 *
 * <p>All per-slot handlers except slot 8 are stubs pending implementation of #349.</p>
 */
public final class AnimationModeInputHandler {

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
            case 0 -> handlePrevKeyframe(player, session, input);
            case 1 -> handleNextKeyframe(player, session, input);
            case 2 -> handleNudgePosX(player, session, input);
            case 3 -> handleNudgePosY(player, session, input);
            case 4 -> handleNudgePosZ(player, session, input);
            case 5 -> handleNudgeExtents(player, session, input);
            case 6 -> handleCycleShape(player, session, input);
            case 7 -> handlePlayPause(player, session, input);
            case 8 -> handleOpenEditor(player, input);
            default -> { /* slots outside 0–8 are not animation tools */ }
        }
    }

    // ── Slot handlers ─────────────────────────────────────────────────────────

    /**
     * Selects the previous keyframe in the editing session.
     * TODO #349: implement decrement with wrap-around
     */
    private static void handlePrevKeyframe(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Selects the next keyframe in the editing session.
     * TODO #349: implement increment with wrap-around
     */
    private static void handleNextKeyframe(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Nudges the selected keyframe's X position.
     * TODO #349: L = −0.1, R = +0.1, Shift+L = −0.5, Shift+R = +0.5
     */
    private static void handleNudgePosX(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Nudges the selected keyframe's Y position.
     * TODO #349: L = −0.1, R = +0.1, Shift+L = −0.5, Shift+R = +0.5
     */
    private static void handleNudgePosY(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Nudges the selected keyframe's Z position.
     * TODO #349: L = −0.1, R = +0.1, Shift+L = −0.5, Shift+R = +0.5
     */
    private static void handleNudgePosZ(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Nudges the selected keyframe's half-extents (or sphere radius for SPHERE shapes).
     * TODO #349: L = −0.05, R = +0.05, Shift+L = −0.25, Shift+R = +0.25
     */
    private static void handleNudgeExtents(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Cycles the selected keyframe's volume shape (OBB → SPHERE → …).
     * TODO #349: advance through VolumeShape enum values on any click
     */
    private static void handleCycleShape(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Toggles the animated keyframe preview playback.
     * TODO #349: start/stop a timed preview sweep through all keyframes
     */
    private static void handlePlayPause(DevSwordPlayer p, AttackDevSession s, InputType i) {
        // TODO #349
    }

    /**
     * Opens {@link AttackEditorMenu} without exiting AnimationMode.
     * The editing session remains active and all keyframe state is preserved.
     *
     * @param p     the player holding the WRITABLE_BOOK in slot 8
     * @param input the triggering input; only left/right-click are handled
     */
    private static void handleOpenEditor(DevSwordPlayer p, InputType input) {
        if (input == InputType.LEFT || input == InputType.RIGHT) {
            new AttackEditorMenu(p).open();
        }
    }
}
