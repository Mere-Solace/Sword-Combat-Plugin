package btm.sword.umbral.motion;

/**
 * Pluggable per-tick motion strategy installed on a {@link BladeMotion}.
 * <p>
 * Drivers carry whatever internal state their algorithm needs (parametric {@code t},
 * accumulated tick counts, last-position cache, etc.) and may freely mutate that state during
 * {@link #tick}. They MUST NOT touch the underlying {@link org.bukkit.entity.ItemDisplay}
 * directly, schedule their own scheduler tasks, or read FSM state — every interaction with
 * the world goes through the supplied {@link BladeMotionContext}.
 * <p>
 * <b>Lifecycle contract:</b>
 * <ol>
 *   <li>{@link #onInstall} is invoked exactly once before the first {@link #tick} call.</li>
 *   <li>{@link #tick} is invoked once per server tick while this driver is the installed driver
 *       on its hosting {@link BladeMotion}. Returning {@link Status#DONE} signals that the
 *       driver has completed its work and should be uninstalled.</li>
 *   <li>{@link #onUninstall} is invoked exactly once when the driver leaves the
 *       {@link BladeMotion.State#DRIVING DRIVING} state — whether because {@code tick} returned
 *       {@code DONE}, the host called {@link BladeMotion#stop()}, or another driver was
 *       installed in its place.</li>
 * </ol>
 * A driver instance MUST NOT be reused after {@code onUninstall}. Returning {@code DONE} does
 * not itself trigger any FSM transition; the owning state must observe {@link BladeMotion#isEnded()}
 * during its own tick to react.
 */
public interface BladeMotionDriver {

    /**
     * Called exactly once before the first {@link #tick}.
     *
     * @param context the per-driver view of the motion subsystem
     */
    void onInstall(BladeMotionContext context);

    /**
     * Called once per server tick while this driver is installed.
     *
     * @param context the per-driver view of the motion subsystem
     * @return {@link Status#RUNNING} to remain installed for another tick, or {@link Status#DONE}
     *         to be uninstalled at the end of this tick
     */
    Status tick(BladeMotionContext context);

    /**
     * Called exactly once when this driver leaves {@code DRIVING}. Implementations should
     * release any per-instance resources captured in {@link #onInstall} or during {@link #tick}.
     *
     * @param context the per-driver view of the motion subsystem
     */
    void onUninstall(BladeMotionContext context);

    /** Per-tick result reported by {@link #tick}. */
    enum Status {
        /** Driver wants to be ticked again next server tick. */
        RUNNING,
        /** Driver has completed and should be uninstalled at the end of this tick. */
        DONE
    }
}
