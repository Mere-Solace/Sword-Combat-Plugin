package btm.sword.system.attack.dev;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.entity.impl.DevSwordPlayer;
import btm.sword.system.inventory.menu.dev.AttackBrowserMenu;

/**
 * Manages the lifecycle of AnimationMode — the in-world attack keyframe editing mode
 * described in issue #344.
 *
 * <p>AnimationMode is an internal state on a {@link DevSwordPlayer}, not a separate
 * player-wrapper swap. A {@link DevSwordPlayer} is registered for the whole session
 * (constructed at login if the player appears in {@code devnames.txt}); this class only
 * toggles the {@link DevSwordPlayer#isInAnimationMode()} flag and handles inventory
 * save/restore and UmbralBlade activation, mirroring the existing
 * {@link btm.sword.system.entity.impl.SwordPlayer#enterCreativeDevMode()} pattern.</p>
 *
 * <h2>State transition</h2>
 * <pre>
 * CREATIVE_DEV (DevSwordPlayer, inAnimationMode=false)
 *   → {@link #enter} → ANIMATION_MODE (DevSwordPlayer, inAnimationMode=true)
 *   → {@link #exit}  → CREATIVE_DEV  (DevSwordPlayer, inAnimationMode=false)
 * </pre>
 *
 * <h2>Inventory chain</h2>
 * <ol>
 *   <li>{@link #enter} saves the creative-dev inventory to
 *       {@link AttackDevSession#getSavedAnimationInventory()} and replaces it with
 *       animation editing tools (slots 0–8) and a BARRIER exit button (slot 35).</li>
 *   <li>{@link #exit} restores the saved inventory and opens {@link AttackBrowserMenu}.</li>
 * </ol>
 */
public final class AnimationMode {

    private AnimationMode() {}

    /**
     * Enters AnimationMode on the given {@link DevSwordPlayer}.
     *
     * <ol>
     *   <li>Deactivates the UmbralBlade to prevent an orphan display entity.</li>
     *   <li>Saves the current inventory to {@link AttackDevSession#getSavedAnimationInventory()}.</li>
     *   <li>Suppresses all anchored-item upkeep so managed items cannot refill the hotbar.</li>
     *   <li>Clears the inventory and populates the animation hotbar (slots 0–8).</li>
     *   <li>Places a BARRIER exit button at slot 35.</li>
     *   <li>Sets the {@link DevSwordPlayer#isInAnimationMode()} flag to {@code true}.</li>
     *   <li>Starts {@link VolumeEditorMode} if the session is in {@link DevMode#EDITING}.</li>
     * </ol>
     *
     * @param devPlayer the {@link DevSwordPlayer} entering AnimationMode; must already be
     *                  in creative dev mode
     * @param session   the active attack editing session
     */
    public static void enter(DevSwordPlayer devPlayer, AttackDevSession session) {
        // 1. Deactivate blade — prevents orphan display entity
        devPlayer.deactivateUmbralBlade();

        // 2. Snapshot the current creative-dev inventory
        session.setSavedAnimationInventory(devPlayer.player().getInventory().getContents().clone());

        // 3. Suppress all anchored-item upkeep (menu button, ability slots, pouches, etc.)
        devPlayer.setAllAnchoredItemUpkeep(false);

        // 4. Clear inventory and populate animation hotbar
        clearInventory(devPlayer);
        populateAnimationHotbar(devPlayer);

        // 5. Place BARRIER exit button at slot 35
        devPlayer.player().getInventory().setItem(35, buildExitItem());

        // 6. Activate AnimationMode on the wrapper
        devPlayer.setInAnimationMode(true);

        // 7. Ensure VolumeEditorMode render loop is running
        if (session.getMode() == DevMode.EDITING) {
            VolumeEditorMode.startForSession(session);
        }
    }

    /**
     * Exits AnimationMode on the given {@link DevSwordPlayer}.
     *
     * <ol>
     *   <li>Clears the AnimationMode flag.</li>
     *   <li>Restores the inventory from {@link AttackDevSession#getSavedAnimationInventory()}.</li>
     *   <li>Keeps anchored-item upkeep suppressed (player is still in creative dev mode).</li>
     *   <li>Reactivates the UmbralBlade.</li>
     *   <li>Opens {@link AttackBrowserMenu}.</li>
     * </ol>
     *
     * @param devPlayer the currently active {@link DevSwordPlayer} in AnimationMode
     */
    public static void exit(DevSwordPlayer devPlayer) {
        devPlayer.setInAnimationMode(false);

        AttackDevSession session = AttackDevSession.get(devPlayer.player().getUniqueId());

        // Restore inventory to the creative-dev state
        if (session != null && session.getSavedAnimationInventory() != null) {
            devPlayer.player().getInventory().setContents(session.getSavedAnimationInventory());
            session.setSavedAnimationInventory(null);
        }

        // Remain suppressed — still in creative dev mode
        devPlayer.setAllAnchoredItemUpkeep(false);

        // Reactivate blade (creative dev mode deactivates it again if needed)
        devPlayer.activateUmbralBlade();

        new AttackBrowserMenu(devPlayer).open();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void clearInventory(DevSwordPlayer dev) {
        for (int i = 0; i < 36; i++) {
            dev.player().getInventory().setItem(i, null);
        }
        dev.player().getInventory().setItem(38, null);
        dev.player().getInventory().setItem(40, null);
    }

    private static void populateAnimationHotbar(DevSwordPlayer dev) {
        // TODO #349: build and place tool items for slots 0–8
        // Slot 0: ARROW              — select previous keyframe
        // Slot 1: ARROW              — select next keyframe
        // Slot 2: LIGHT_BLUE_DYE    — nudge position X (L: −0.1, R: +0.1, Shift: ±0.5)
        // Slot 3: GREEN_DYE          — nudge position Y
        // Slot 4: CYAN_DYE           — nudge position Z
        // Slot 5: ORANGE_DYE         — nudge half-extents / sphere radius
        // Slot 6: ENDER_EYE          — cycle shape (OBB → SPHERE → …)
        // Slot 7: CLOCK              — play / pause animated preview
        // Slot 8: WRITABLE_BOOK      — open AttackEditorMenu (stays in AnimationMode)
    }

    private static ItemStack buildExitItem() {
        // TODO: use ItemStackBuilder to add "Exit Animation Mode" name + lore
        return ItemStack.of(Material.BARRIER);
    }
}
