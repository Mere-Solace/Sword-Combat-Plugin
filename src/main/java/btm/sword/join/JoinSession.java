package btm.sword.join;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.config.section.JoinSequenceConfig;
import btm.sword.config.section.MenuGridConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.join.menu.JoinRouterMenu;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.runtime.scheduler.TimeArbiter.TaskHandle;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.invui.window.WindowManager;

/**
 * Per-player owner of the join-sequence lifecycle.
 *
 * <h2>Single ownership</h2>
 * <p>For every online player going through join routing there is exactly one
 * {@code JoinSession}. The session owns:</p>
 * <ul>
 *   <li>The current {@link JoinPhase}.</li>
 *   <li>The acquired dark-room slot {@link Location}.</li>
 *   <li>Every running scheduler {@link TaskHandle}.</li>
 *   <li>The currently-open InvUI {@link Window} (indirectly via {@link WindowManager}).</li>
 *   <li>The {@link Destination} chosen during {@link JoinPhase#ROUTING}.</li>
 * </ul>
 * <p>External code interacts only through the intent methods on this class; no getter
 * exposes the phase or task handles. The arbiter that creates sessions never reads or
 * mutates session state directly.</p>
 *
 * <h2>Idempotent transitions</h2>
 * <p>Each transition method early-returns when called from a phase that does not match
 * its precondition, and tears down every task handle owned by the prior phase before
 * scheduling the new phase's handles. Repeated calls produce no duplicates and no leaks.</p>
 *
 * <h2>Termination</h2>
 * <p>The {@code terminated} latch is set by {@link #terminate()}. Once set, every
 * transition method becomes a no-op so a session that has been ended (player quit, plugin
 * disable, etc.) cannot be revived. The latch is private to enforce that termination is
 * the only path out of an in-progress session.</p>
 *
 * <h2>Threading</h2>
 * <p>All transition methods must be called on the Bukkit main thread. The scheduler tasks
 * registered via {@link TimeArbiter} dispatch their bodies on the main thread, so callbacks
 * from those tasks (including the routing countdown's last-iteration callback that drives
 * {@link #enterActive()}) inherit the same threading guarantee.</p>
 */
public final class JoinSession {

    /**
     * Receives a notification that a session has ended (either successfully via
     * {@link JoinPhase#ACTIVE} or via {@link #terminate()}). The arbiter implementing
     * this callback removes the session from its {@code UUID → JoinSession} map.
     */
    @FunctionalInterface
    public interface EvictionCallback {
        /**
         * Removes the session for {@code uuid} from the arbiter map. Idempotent.
         *
         * @param uuid the player whose session should be evicted
         */
        void evict(UUID uuid);
    }

    // ── Identity & collaborators ──────────────────────────────────────────────

    private final UUID uuid;
    private final SwordPlayer swordPlayer;
    private final Location stagingSlot;
    private final PlayerWaitingGate gate;
    private final EvictionCallback evictionCallback;

    // ── Mutable state — only mutated through transition methods ───────────────

    private JoinPhase phase = JoinPhase.STAGING;
    private boolean terminated = false;
    private Destination chosenDestination;

    /**
     * Single ticker that runs from {@link JoinPhase#WAITING} through
     * {@link JoinPhase#ROUTING}. Always zeroes velocity; re-opens the router menu only
     * while in {@link JoinPhase#WAITING}.
     */
    private TaskHandle waitingTicker;

    /** Fixed-iteration countdown owning the {@link JoinPhase#ROUTING} timer. */
    private TaskHandle routingCountdown;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Constructs a new session in {@link JoinPhase#STAGING} bound to the given player.
     *
     * @param swordPlayer        the player this session belongs to; never null
     * @param stagingSlot        the dark-room slot already acquired for this player;
     *                           defensively cloned so later caller mutation cannot
     *                           affect the session. Never null.
     * @param gate               the waiting-phase gate used to engage / disengage / abort
     *                           the player; never null
     * @param evictionCallback   invoked when the session ends (active or terminated) so
     *                           the arbiter can remove this session from its map; never null
     */
    public JoinSession(SwordPlayer swordPlayer,
                       Location stagingSlot,
                       PlayerWaitingGate gate,
                       EvictionCallback evictionCallback) {
        this.swordPlayer = Objects.requireNonNull(swordPlayer, "swordPlayer");
        this.stagingSlot = Objects.requireNonNull(stagingSlot, "stagingSlot").clone();
        this.gate = Objects.requireNonNull(gate, "gate");
        this.evictionCallback = Objects.requireNonNull(evictionCallback, "evictionCallback");
        this.uuid = swordPlayer.player().getUniqueId();
    }

    // ── Public transition API ─────────────────────────────────────────────────

    /**
     * Schedules the deferred transition to {@link JoinPhase#WAITING} after the configured
     * load-wait. Idempotent — repeat calls are ignored.
     *
     * <p>The deferral exists because the spawn-location override sent to the client by
     * {@code PlayerSpawnLocationEvent} needs a moment to propagate and for the staging
     * chunk to load. {@link MenuGridConfig#LOADING_WAIT_MS} is the wait duration.</p>
     */
    public void enterStaging() {
        if (terminated) return;
        if (phase != JoinPhase.STAGING) return;

        SwordScheduler.runBukkitTaskLater(() -> {
            if (terminated) return;
            if (!swordPlayer.player().isOnline()) return;
            enterWaiting();
        }, MenuGridConfig.LOADING_WAIT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Transitions to {@link JoinPhase#WAITING}. Valid from {@link JoinPhase#STAGING} (the
     * happy path) or from {@link JoinPhase#ROUTING} (sneak-cancel). No-op when already in
     * {@link JoinPhase#WAITING} or when the session has been terminated.
     *
     * <p>From STAGING this engages the {@link PlayerWaitingGate}, opens a fresh router
     * menu, and starts the waiting ticker.</p>
     *
     * <p>From ROUTING this cancels the in-flight countdown, clears the chosen destination,
     * and opens a fresh router menu. The waiting ticker is already running and continues
     * uninterrupted; the gate is already engaged.</p>
     */
    public void enterWaiting() {
        if (terminated) return;
        if (phase == JoinPhase.WAITING) return;

        switch (phase) {
            case STAGING -> {
                gate.engage(swordPlayer);
                phase = JoinPhase.WAITING;
                waitingTicker = startWaitingTicker();
                displayMenu();
            }
            case ROUTING -> {
                if (routingCountdown != null) {
                    routingCountdown.cancel();
                    routingCountdown = null;
                }
                chosenDestination = null;
                phase = JoinPhase.WAITING;
                displayMenu();
            }
            default -> throw new IllegalStateException("Cannot enter WAITING from " + phase);
        }
    }

    /**
     * Transitions from {@link JoinPhase#WAITING} to {@link JoinPhase#ROUTING}, starting the
     * configurable countdown that ends with {@link #enterActive()} and the destination
     * teleport. Ignored if the session is not currently in {@link JoinPhase#WAITING} or has
     * been terminated.
     *
     * @param destination the destination the player selected; never null
     */
    public void requestRouting(Destination destination) {
        Objects.requireNonNull(destination, "destination");
        if (terminated) return;
        if (phase != JoinPhase.WAITING) return;

        chosenDestination = destination;
        closeMenuWindow();
        phase = JoinPhase.ROUTING;
        routingCountdown = startRoutingCountdown(destination);
    }

    /**
     * Aborts the routing countdown and returns to {@link JoinPhase#WAITING} with a fresh
     * router menu. Ignored if not currently in {@link JoinPhase#ROUTING} or terminated.
     */
    public void cancelRouting() {
        if (terminated) return;
        if (phase != JoinPhase.ROUTING) return;
        enterWaiting();
    }

    /**
     * Ends this session unconditionally. Cancels every running task, closes the menu
     * window if open, releases the dark-room slot, and evicts this session from the
     * arbiter map.
     *
     * <p>If the session has not yet reached {@link JoinPhase#ACTIVE} the gate is
     * {@code abort}-ed so the stashed inventory is restored to the player. From ACTIVE the
     * gate has already been disengaged and the stash dropped, so nothing further is done
     * to the player.</p>
     *
     * <p>Idempotent — repeat calls are no-ops.</p>
     */
    public void terminate() {
        if (terminated) return;
        terminated = true;

        if (waitingTicker != null) {
            waitingTicker.cancel();
            waitingTicker = null;
        }
        if (routingCountdown != null) {
            routingCountdown.cancel();
            routingCountdown = null;
        }
        closeMenuWindow();

        if (phase != JoinPhase.ACTIVE) {
            gate.abort(swordPlayer);
        }

        MenuSlotGrid.releaseSlot(uuid);
        evictionCallback.evict(uuid);
    }

    // ── Internal transitions ──────────────────────────────────────────────────

    /**
     * Drives the {@link JoinPhase#ROUTING} → {@link JoinPhase#ACTIVE} transition.
     * Invoked from the routing countdown's last-iteration callback. Disengages the gate
     * (which restores survival mode and visibility), teleports the player to the chosen
     * destination, releases the slot, and evicts the session.
     */
    private void enterActive() {
        if (terminated) return;
        if (phase != JoinPhase.ROUTING) return;

        if (waitingTicker != null) {
            waitingTicker.cancel();
            waitingTicker = null;
        }
        // routingCountdown is already in its final fire when this runs; no need to cancel.
        routingCountdown = null;

        closeMenuWindow();

        Destination destination = chosenDestination;
        gate.disengage(swordPlayer, destination);
        swordPlayer.teleport(destination.spawn());

        MenuSlotGrid.releaseSlot(uuid);

        phase = JoinPhase.ACTIVE;
        evictionCallback.evict(uuid);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a new {@link JoinRouterMenu} bound to this session's
     * {@link #requestRouting(Destination)} method and opens it. Each call constructs a
     * fresh menu instance so the {@code selected} latch on a previous instance does not
     * leak across cancel-and-reopen cycles.
     */
    private void displayMenu() {
        JoinRouterMenu menu = new JoinRouterMenu(swordPlayer, this::requestRouting);
        menu.open();
    }

    /**
     * Closes whichever InvUI window the player currently has open, if any.
     */
    private void closeMenuWindow() {
        Window window = WindowManager.getInstance().getOpenWindow(swordPlayer.player());
        if (window != null) window.close();
    }

    /**
     * Starts the single waiting-phase ticker that zeroes velocity every tick and re-opens
     * the router menu while in {@link JoinPhase#WAITING}. Cancellation is owned by
     * {@link #terminate()} and {@link #enterActive()}.
     */
    private TaskHandle startWaitingTicker() {
        return TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> {
                if (terminated) return;
                swordPlayer.player().setVelocity(new Vector(0, 0, 0));
                if (phase == JoinPhase.WAITING) {
                    Window open = WindowManager.getInstance().getOpenWindow(swordPlayer.player());
                    if (open == null) {
                        displayMenu();
                    }
                }
            },
            0, 50,
            JoinSession.class, "waitingTicker"
        );
    }

    /**
     * Starts the routing countdown task. Iteration count is derived from
     * {@link JoinSequenceConfig#ROUTING_DURATION_MS} and
     * {@link JoinSequenceConfig#ROUTING_TICK_PERIOD_MS} so the last-iteration callback
     * fires at exactly {@code ROUTING_DURATION_MS} ms after the click. The callback drives
     * the transition into {@link JoinPhase#ACTIVE}.
     */
    private TaskHandle startRoutingCountdown(Destination destination) {
        AtomicInteger ai = new AtomicInteger(0);
        int periodMs = Math.max(1, JoinSequenceConfig.ROUTING_TICK_PERIOD_MS);
        int iterations = Math.max(0,
            (JoinSequenceConfig.ROUTING_DURATION_MS / periodMs) - 1);

        return TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> swordPlayer.displayTitle(
                Component.text(ai.incrementAndGet()),
                Component.text("Preparing to teleport..."),
                0L, periodMs + 100L, 0L
            ),
            0, periodMs, iterations,
            JoinSession.class, "routingCountdown",
            () -> {
                swordPlayer.displayTitle(
                    Component.text(ai.incrementAndGet()),
                    Component.text("Teleporting"),
                    0L, periodMs + 100L, 0L
                );
                enterActive();
            }
        );
    }
}
