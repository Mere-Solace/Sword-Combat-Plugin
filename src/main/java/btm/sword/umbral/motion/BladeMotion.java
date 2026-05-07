package btm.sword.umbral.motion;

import java.util.Objects;

/**
 * Single owner of the blade's per-tick motion. Hosts at most one {@link BladeMotionDriver} at a
 * time and forwards each {@link #tick()} call to it.
 * <p>
 * <b>Lifecycle states</b> (see {@link State}):
 * <ul>
 *   <li>{@link State#IDLE IDLE} — initial state; no driver installed; {@link #tick()} is a no-op.</li>
 *   <li>{@link State#DRIVING DRIVING} — a driver is installed and being ticked.</li>
 *   <li>{@link State#ENDED ENDED} — terminal-until-reinstall; the previously installed driver
 *       has been uninstalled and no driver is currently active. Reinstalling via
 *       {@link #install(BladeMotionDriver)} returns the motion to {@code DRIVING}.</li>
 * </ul>
 *
 * <b>Invariants:</b>
 * <ol>
 *   <li>At most one driver is installed at any moment.</li>
 *   <li>{@link BladeMotionDriver#onInstall} is called exactly once before the first
 *       {@link BladeMotionDriver#tick}.</li>
 *   <li>{@link BladeMotionDriver#onUninstall} is called exactly once for both the natural
 *       {@code DONE}-from-tick path and the external {@link #stop()} path.</li>
 *   <li>{@link #install(BladeMotionDriver)} and {@link #stop()} are safe to call from inside a
 *       driver's {@link BladeMotionDriver#tick} — their effect is queued and applied at end of
 *       tick. Last call wins.</li>
 *   <li>{@link #stop()} is idempotent.</li>
 * </ol>
 *
 * <b>Ownership:</b> the {@link BladeMotionContext} is created by and exclusively owned by this
 * class; no other component may construct one. The underlying {@link BladeMotionHost} is
 * supplied at construction and is expected to remain valid for the motion's entire lifetime.
 */
public final class BladeMotion {

    /** Lifecycle state of the motion subsystem. */
    public enum State {
        /** Initial state. No driver installed. */
        IDLE,
        /** A driver is installed and being ticked each server tick. */
        DRIVING,
        /** A driver has been uninstalled. Reinstall to return to {@code DRIVING}. */
        ENDED
    }

    private final BladeMotionContext context;
    private State state = State.IDLE;
    private BladeMotionDriver currentDriver;
    private BladeMotionDriver pendingDriver;
    private boolean pendingStop;
    private boolean ticking;

    /**
     * Creates a new motion subsystem bound to the given host.
     *
     * @param host the bridge to the underlying display + thrower; must remain valid for the
     *             lifetime of this motion
     */
    public BladeMotion(BladeMotionHost host) {
        this.context = new BladeMotionContext(Objects.requireNonNull(host, "host"));
    }

    /** Returns the current lifecycle state. */
    public State state() {
        return state;
    }

    /** Returns {@code true} if the motion is in {@link State#ENDED ENDED}. */
    public boolean isEnded() {
        return state == State.ENDED;
    }

    /** Returns {@code true} if a driver is currently installed and being ticked. */
    public boolean isDriving() {
        return state == State.DRIVING;
    }

    /**
     * Installs a driver, replacing the current one (if any). Transitions to
     * {@link State#DRIVING DRIVING}. If called from inside a driver's tick, the swap is queued
     * and applied at end of tick (last call wins).
     *
     * @param driver the driver to install; must not be {@code null}
     */
    public void install(BladeMotionDriver driver) {
        Objects.requireNonNull(driver, "driver");
        if (ticking) {
            pendingDriver = driver;
            pendingStop = false;
            return;
        }
        swapTo(driver);
    }

    /**
     * Uninstalls the current driver (if any) and transitions to {@link State#ENDED ENDED}.
     * Idempotent. If called from inside a driver's tick, takes effect at end of tick (last call
     * wins — a subsequent {@link #install} from the same tick supersedes the stop).
     */
    public void stop() {
        if (ticking) {
            pendingStop = true;
            pendingDriver = null;
            return;
        }
        if (state == State.DRIVING && currentDriver != null) {
            BladeMotionDriver finished = currentDriver;
            currentDriver = null;
            state = State.ENDED;
            finished.onUninstall(context);
        } else {
            state = State.ENDED;
        }
    }

    /**
     * Advances the installed driver by one tick. No-op if not in {@link State#DRIVING DRIVING}.
     * Must be called exactly once per server tick by the blade's coordinator.
     */
    public void tick() {
        if (state != State.DRIVING || currentDriver == null) return;
        ticking = true;
        try {
            context.incrementTicksSinceInstall();
            BladeMotionDriver.Status status = currentDriver.tick(context);
            if (status == BladeMotionDriver.Status.DONE) {
                BladeMotionDriver finished = currentDriver;
                currentDriver = null;
                state = State.ENDED;
                finished.onUninstall(context);
            }
        } finally {
            ticking = false;
            applyPending();
        }
    }

    private void applyPending() {
        if (pendingDriver != null) {
            BladeMotionDriver next = pendingDriver;
            pendingDriver = null;
            pendingStop = false;
            swapTo(next);
        } else if (pendingStop) {
            pendingStop = false;
            stop();
        }
    }

    private void swapTo(BladeMotionDriver next) {
        if (currentDriver != null) {
            BladeMotionDriver previous = currentDriver;
            currentDriver = null;
            previous.onUninstall(context);
        }
        currentDriver = next;
        state = State.DRIVING;
        context.resetTicksSinceInstall();
        next.onInstall(context);
    }
}
