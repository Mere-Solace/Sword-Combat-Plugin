package btm.sword.system.attack.dev;

import btm.sword.system.entity.impl.DevSwordPlayer;

/**
 * Presents a non-escapable save/discard confirmation dialog when the player
 * attempts to exit {@link AnimationMode} via the BARRIER button in slot 35.
 *
 * <h2>Dialog layout (1 row × 9)</h2>
 * <pre>
 * [ ][ ][ ] [Save &amp; Exit] [ ] [Discard &amp; Exit] [ ][ ][ ]
 * </pre>
 *
 * <ul>
 *   <li><b>Save &amp; Exit</b> (slot 3, {@code LIME_TERRACOTTA}) — writes the current
 *       {@link AttackDevSession#buildCurrentAttack()} result to disk, registers it in
 *       {@link btm.sword.system.attack.def.AttackRegistry}, then calls
 *       {@link AnimationMode#exit}.</li>
 *   <li><b>Discard &amp; Exit</b> (slot 5, {@code RED_TERRACOTTA}) — discards unsaved
 *       changes and calls {@link AnimationMode#exit}.</li>
 * </ul>
 *
 * <p>The window title shows how many unsaved edits have been made using
 * {@link AttackDevSession#getEditCount()}. {@link org.bukkit.event.inventory.InventoryCloseEvent}
 * is cancelled while the dialog is open so the player cannot dismiss it without choosing.</p>
 *
 * <p>Implementation pending issue #349.</p>
 */
public final class SaveConfirmDialog {

    private SaveConfirmDialog() {}

    /**
     * Opens the save/discard confirmation dialog for the given player and session.
     * Currently a stub — logs a placeholder message until #349 is implemented.
     *
     * @param player  the {@link DevSwordPlayer} requesting to exit AnimationMode
     * @param session the active {@link AttackDevSession}, or {@code null} if none
     */
    public static void open(DevSwordPlayer player, AttackDevSession session) {
        // TODO #349: build and open a non-escapable InvUI Window with save/discard buttons
        //   - Title: "Save changes? (" + session.getEditCount() + " edits made)"
        //   - Slot 3: LIME_TERRACOTTA  "Save & Exit"    → save + AnimationMode.exit(player)
        //   - Slot 5: RED_TERRACOTTA   "Discard & Exit" → AnimationMode.exit(player)
        //   - Cancel InventoryCloseEvent while open
        AnimationMode.exit(player);
    }
}
