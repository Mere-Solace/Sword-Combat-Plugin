package btm.sword.combat.dev;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.entity.player.DevSwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.dev.AttackBrowserMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Manages the lifecycle of AnimationMode — the in-world attack keyframe editing mode
 * described in issue #344.
 *
 * <p>AnimationMode is an internal state on a {@link DevSwordPlayer}, not a separate
 * player-wrapper swap. A {@link DevSwordPlayer} is registered for the whole session
 * (constructed at login if the player appears in {@code devnames.txt}); this class only
 * toggles the {@link DevSwordPlayer#isInAnimationMode()} flag and handles inventory
 * save/restore and UmbralBlade activation, mirroring the existing
 * {@link btm.sword.entity.player.SwordPlayer#enterCreativeDevMode()} pattern.</p>
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
     *   <li>Suppresses the blade via {@link DevSwordPlayer#setUmbralBladeActive(boolean)} —
     *       {@code Combatant.handleUmbralBladeTick()} destroys the blade on the next tick.</li>
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
        // 1. Suppress the blade via the lifecycle owner — blade is destroyed on the next tick.
        devPlayer.setUmbralBladeActive(false);

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
     * Exits AnimationMode on the given {@link DevSwordPlayer} and opens {@link AttackBrowserMenu}.
     *
     * @param devPlayer the currently active {@link DevSwordPlayer} in AnimationMode
     */
    public static void exit(DevSwordPlayer devPlayer) {
        exitSilent(devPlayer);
        new AttackBrowserMenu(devPlayer).open();
    }

    /**
     * Exits AnimationMode without opening any menu — use when the player cannot interact
     * with a menu (e.g. death, forced abort).
     *
     * <ol>
     *   <li>Clears the AnimationMode flag.</li>
     *   <li>Restores the inventory from {@link AttackDevSession#getSavedAnimationInventory()}.</li>
     *   <li>Keeps anchored-item upkeep suppressed (player is still in creative dev mode).</li>
     *   <li>Re-enables the blade via {@link DevSwordPlayer#setUmbralBladeActive(boolean)} —
     *       the lifecycle owner respawns it on the next tick.</li>
     * </ol>
     *
     * @param devPlayer the currently active {@link DevSwordPlayer} in AnimationMode
     */
    public static void exitSilent(DevSwordPlayer devPlayer) {
        devPlayer.setInAnimationMode(false);

        AttackDevSession session = AttackDevSession.get(devPlayer.player().getUniqueId());

        if (session != null && session.getSavedAnimationInventory() != null) {
            devPlayer.player().getInventory().setContents(session.getSavedAnimationInventory());
            session.setSavedAnimationInventory(null);
        }

        devPlayer.setAllAnchoredItemUpkeep(false);
        devPlayer.setUmbralBladeActive(true);
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
        dev.player().getInventory().setItem(0,
            new ItemStackBuilder(Material.ARROW)
                .name(Component.text("← Prev Keyframe", NamedTextColor.WHITE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Any click — select previous keyframe", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(1,
            new ItemStackBuilder(Material.ARROW)
                .name(Component.text("Next Keyframe →", NamedTextColor.WHITE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Any click — select next keyframe", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(2,
            new ItemStackBuilder(Material.LIGHT_BLUE_DYE)
                .name(Component.text("Nudge Pos X", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: −0.1  R: +0.1  Drop: −0.5  Swap: +0.5", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(3,
            new ItemStackBuilder(Material.GREEN_DYE)
                .name(Component.text("Nudge Pos Y", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: −0.1  R: +0.1  Drop: −0.5  Swap: +0.5", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(4,
            new ItemStackBuilder(Material.CYAN_DYE)
                .name(Component.text("Nudge Pos Z", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: −0.1  R: +0.1  Drop: −0.5  Swap: +0.5", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(5,
            new ItemStackBuilder(Material.ORANGE_DYE)
                .name(Component.text("Nudge Half-Extents", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("L: −0.05  R: +0.05  Drop: −0.25  Swap: +0.25", NamedTextColor.DARK_GRAY),
                    Component.text("SPHERE: adjusts radius only", NamedTextColor.GRAY)
                ))
                .build());
        dev.player().getInventory().setItem(6,
            new ItemStackBuilder(Material.ENDER_EYE)
                .name(Component.text("Cycle Shape", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(Component.text("Any click — OBB → SPHERE → …", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(7,
            new ItemStackBuilder(Material.CLOCK)
                .name(Component.text("Play / Pause Preview", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(Component.text("Any click — toggle animated preview", NamedTextColor.DARK_GRAY)))
                .build());
        dev.player().getInventory().setItem(8,
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Open Editor", NamedTextColor.WHITE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Any click — open AttackEditorMenu", NamedTextColor.DARK_GRAY),
                    Component.text("Shortcut: SHIFT+SWAP with the wand", NamedTextColor.DARK_GRAY)
                ))
                .build());
    }

    private static ItemStack buildExitItem() {
        return new ItemStackBuilder(Material.BARRIER)
            .name(Component.text("Exit Animation Mode", NamedTextColor.RED, TextDecoration.BOLD))
            .lore(List.of(
                Component.text("Left-click — save & exit", NamedTextColor.DARK_GRAY),
                Component.text("Opens the save confirm dialog.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("This is the ONLY way to exit —", NamedTextColor.GRAY),
                Component.text("closing the inventory does not stop keyframe display.", NamedTextColor.GRAY)
            ))
            .build();
    }
}
