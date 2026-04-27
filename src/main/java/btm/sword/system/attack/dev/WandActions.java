package btm.sword.system.attack.dev;

import btm.sword.system.attack.def.AttackInstance;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.dev.AttackBrowserMenu;
import btm.sword.system.inventory.menu.dev.AttackEditorMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Static action helpers for volume-attack wand input bindings registered in
 * {@link btm.sword.system.input.InputRegistrar}.
 *
 * <h2>DROP+DROP — exit session</h2>
 * {@link #exitSession} stops whatever dev session is currently active (RECORDING,
 * EDITING, or VIEWING), sends an actionbar confirmation, and opens
 * {@link AttackBrowserMenu}.
 *
 * <h2>SHIFT+SWAP — open editor</h2>
 * {@link #openEditor} transitions the player into EDITING mode if possible and opens
 * {@link AttackEditorMenu}. Falls back to opening {@link AttackBrowserMenu} when
 * there is nothing to edit.
 */
public final class WandActions {

    private WandActions() {}

    /**
     * Stops the active dev session and opens {@link AttackBrowserMenu}.
     *
     * <ul>
     *   <li>If a session is active, calls {@link AttackDevSession#stopCurrentSession()}.</li>
     *   <li>Sends an actionbar message confirming the session was ended.</li>
     *   <li>Opens {@link AttackBrowserMenu}.</li>
     * </ul>
     *
     * @param executor the player holding the volume-attack wand
     */
    public static void exitSession(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;

        AttackDevSession session = AttackDevSession.get(player.player().getUniqueId());
        if (session != null && session.isActive()) {
            session.stopCurrentSession();
        }

        player.player().sendActionBar(
            Component.text("[Dev] Session ended", NamedTextColor.YELLOW));
        new AttackBrowserMenu(player).open();
    }

    /**
     * Opens {@link AttackEditorMenu}, entering EDITING mode first if necessary.
     *
     * <ul>
     *   <li>If already in {@link DevMode#EDITING}: opens the editor directly.</li>
     *   <li>If a wand-loaded {@link AttackInstance} exists: calls
     *       {@link AttackDevSession#startEditingFromDef} then opens the editor.</li>
     *   <li>Otherwise: opens {@link AttackBrowserMenu} so the player can pick an attack.</li>
     * </ul>
     *
     * @param executor the player holding the volume-attack wand
     */
    public static void openEditor(Combatant executor) {
        if (!(executor instanceof SwordPlayer player)) return;

        AttackDevSession session = AttackDevSession.getOrCreate(player.player());

        if (session.getMode() == DevMode.EDITING) {
            new AttackEditorMenu(player).open();
            return;
        }

        AttackInstance loaded = session.getLoadedAttackInstance();
        if (loaded != null) {
            try {
                session.startEditingFromDef(loaded);
                new AttackEditorMenu(player).open();
            } catch (IllegalArgumentException e) {
                player.message(Component.text("[Dev] " + e.getMessage(), NamedTextColor.RED));
                new AttackBrowserMenu(player).open();
            }
            return;
        }

        new AttackBrowserMenu(player).open();
    }
}
