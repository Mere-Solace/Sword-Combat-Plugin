package btm.sword.join;

import btm.sword.entity.base.SwordEntity;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.input.trie.ActivationContext;

/**
 * Single-purpose guard helper that reports whether a {@link SwordPlayer} is currently
 * locked out of all gameplay input by the join-waiting phase.
 *
 * <h2>Purpose</h2>
 * <p>The hard input-suppression invariant for {@link ActivationContext#WAITING} is enforced
 * at one structural chokepoint inside {@code InputRouter.route} — a single early-return
 * cancels all combat/skill/blade/ability dispatch. This helper exists for the handful of
 * Bukkit listeners that side-step the router (inventory clicks, item pickup, drag events)
 * and need an equivalent first-line check.</p>
 *
 * <h2>Discipline</h2>
 * <ul>
 *   <li>Only call this from event handlers or pre-dispatch code paths. Do not consult it
 *       inside core gameplay code — by the time something reaches gameplay logic, the
 *       router-level gate has already done its job.</li>
 *   <li>This helper is read-only. It never mutates player state. It is safe from any thread.</li>
 * </ul>
 */
public final class WaitingPhaseGuard {

    private WaitingPhaseGuard() {}

    /**
     * Returns {@code true} if the given player is currently in the join-waiting phase
     * and therefore must be denied any gameplay-side handling of the inbound event.
     *
     * @param player the player whose context to inspect; never null
     * @return {@code true} if the player's {@link ActivationContext} is
     *         {@link ActivationContext#WAITING}
     */
    public static boolean isLocked(SwordPlayer player) {
        return player.getActivationContext() == ActivationContext.WAITING;
    }

    /**
     * Convenience overload that accepts any {@link SwordEntity} — non-player entities
     * are never locked, so this returns {@code false} for them. Use this when the calling
     * site already has a {@code SwordEntity} reference and the explicit instanceof check
     * would be noise.
     *
     * @param entity the entity to inspect; never null
     * @return {@code true} only if {@code entity} is a {@link SwordPlayer} in
     *         {@link ActivationContext#WAITING}
     */
    public static boolean isLocked(SwordEntity entity) {
        return entity instanceof SwordPlayer sp && isLocked(sp);
    }
}
