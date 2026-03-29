package btm.sword.system.scene.animation;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.input.ActivationContext;
import btm.sword.system.input.InputType;
import btm.sword.system.scene.CameraSystem;

/**
 * Handles player inputs while {@link ActivationContext#CUTSCENE} is active.
 * <p>
 * Called from {@link SwordPlayer#act(InputType)} before the normal trie traversal
 * so that cutscene-specific bindings completely bypass combat logic.
 * </p>
 *
 * <ul>
 *   <li>{@link InputType#SHIFT} / {@link InputType#SHIFT_TAP} — exit the cutscene.</li>
 *   <li>All other inputs — silently ignored (reserved for future dialogue / chapter-skip).</li>
 * </ul>
 */
public final class CutsceneInputHandler {

    private CutsceneInputHandler() {}

    /**
     * Processes an input for a player currently in a cutscene.
     * Shift exits the scene; all other inputs are suppressed.
     *
     * @param player the player in the cutscene
     * @param input  the input type received
     */
    public static void handle(SwordPlayer player, InputType input) {
        if (input == InputType.SHIFT || input == InputType.SHIFT_TAP) {
            CameraSystem.stopController(player);
            player.setActivationContext(ActivationContext.NORMAL);
        }
        // All other inputs are intentionally suppressed during cutscenes.
    }
}
