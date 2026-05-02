package btm.sword.input;

import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.TimeArbiter;

/**
 * Owns the timing and scheduled-task lifecycle for the two physical-input gestures that
 * Bukkit does not expose as discrete events: right-click hold and sneak hold.
 *
 * <h2>Responsibility</h2>
 * <ul>
 *   <li>Tracks whether each gesture is currently held.</li>
 *   <li>Records the duration of each gesture.</li>
 *   <li>Schedules the per-tick polling task that drives the gesture and decides when
 *       to fire its tap or hold callback.</li>
 * </ul>
 *
 * <p>The tracker is intentionally <strong>oblivious to inventory, blocking, parry, and
 * tree state</strong>. All gameplay-side reactions are routed through the
 * {@link RightHoldHandler} and {@link SneakHandler} interfaces, which the owner (e.g.
 * {@code SwordPlayer}) implements. This keeps the input layer a single-writer for gesture
 * timing and lets the player layer remain the single-writer for everything else.</p>
 *
 * <h2>Lifecycle</h2>
 * For each gesture there are exactly four explicit transitions:
 * <pre>
 *   IDLE  --start()-->  HELD
 *   HELD  --release()-->  RELEASED  (tap/hold fired on next scheduler tick)
 *   HELD  --abort()-->  IDLE        (no callbacks fired; used for shutdown/teardown)
 *   RELEASED  --(scheduler)-->  IDLE
 * </pre>
 *
 * <h3>Idempotency</h3>
 * <ul>
 *   <li>{@code start*} is a no-op while the gesture is already held.</li>
 *   <li>{@code release*} is a no-op while the gesture is not held.</li>
 *   <li>{@code abort*} is a no-op while no task is scheduled.</li>
 *   <li>{@link #shutdown()} aborts both gestures and is safe to call repeatedly.</li>
 * </ul>
 *
 * <h3>Invariants</h3>
 * <ul>
 *   <li>While {@code held == true}, exactly one scheduled task exists for that gesture.</li>
 *   <li>While {@code held == false}, no scheduled task is needed; any task in flight will
 *       cancel itself on its next fire via the registered exit predicate.</li>
 *   <li>{@code durationMs} is only updated at the {@code release()} transition; readers
 *       observe a stable value between releases.</li>
 *   <li>{@code onEnded} runs synchronously inside {@code release()} so that callers can
 *       restore inventory state before the deferred tap/hold callback fires.</li>
 * </ul>
 */
public final class InputGestureTracker {

    /** Maximum duration in milliseconds for a release to count as a tap rather than a hold. */
    public static final long DEFAULT_TAP_THRESHOLD_MS = 162L;

    private final RightHoldHandler rightHandler;
    private final SneakHandler sneakHandler;
    private final long tapThresholdMs;

    private TimeArbiter.TaskHandle rightTask;
    private boolean rightHeld;
    private long rightStartMs;
    private long rightDurationMs;

    private TimeArbiter.TaskHandle sneakTask;
    private boolean sneakHeld;
    private long sneakStartMs;
    private long sneakDurationMs;

    /**
     * Constructs a tracker that uses the default {@value #DEFAULT_TAP_THRESHOLD_MS} ms
     * tap threshold.
     *
     * @param rightHandler callback object for right-click gesture lifecycle events
     * @param sneakHandler callback object for sneak-hold gesture lifecycle events
     */
    public InputGestureTracker(RightHoldHandler rightHandler, SneakHandler sneakHandler) {
        this(rightHandler, sneakHandler, DEFAULT_TAP_THRESHOLD_MS);
    }

    /**
     * Constructs a tracker with an explicit tap threshold.
     *
     * @param rightHandler callback object for right-click gesture lifecycle events
     * @param sneakHandler callback object for sneak-hold gesture lifecycle events
     * @param tapThresholdMs gesture release durations strictly less than this value are
     *                       reported as a tap; durations greater than or equal report as a hold
     */
    public InputGestureTracker(RightHoldHandler rightHandler,
                               SneakHandler sneakHandler,
                               long tapThresholdMs) {
        this.rightHandler = rightHandler;
        this.sneakHandler = sneakHandler;
        this.tapThresholdMs = tapThresholdMs;
    }

    // ── Right-hold lifecycle ──────────────────────────────────────────────────

    /**
     * Begins a right-hold gesture. No-op if a right-hold is already in progress.
     * Fires {@link RightHoldHandler#onBegan()} synchronously.
     */
    public void startRightHold() {
        if (rightHeld) return;
        rightHeld = true;
        rightStartMs = System.currentTimeMillis();
        rightDurationMs = 0L;
        rightHandler.onBegan();
        if (rightTask != null && !rightTask.isCancelled()) rightTask.cancel();
        rightTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            this::pollRight,
            null,
            100, 50,
            InputGestureTracker.class, "rightHold",
            new PredicateRunnablePair(
                () -> !rightHeld,
                () -> {
                    boolean longPress = rightDurationMs >= tapThresholdMs;
                    rightHandler.onReleased(longPress);
                    rightTask = null;
                }
            )
        );
    }

    /**
     * Marks the right-hold released. The deferred tap/hold callback fires on the next
     * scheduler tick via the exit predicate registered in {@link #startRightHold()}.
     * {@link RightHoldHandler#onEnded()} runs synchronously here so callers can restore
     * inventory state before the deferred callback observes it.
     * No-op if no right-hold is in progress.
     */
    public void releaseRightHold() {
        if (!rightHeld) return;
        rightHeld = false;
        rightDurationMs = System.currentTimeMillis() - rightStartMs;
        rightHandler.onEnded();
    }

    /**
     * Cancels the right-hold gesture without firing any further callbacks. Used during
     * teardown (player leave, plugin disable). Idempotent.
     */
    public void abortRightHold() {
        if (rightTask != null && !rightTask.isCancelled()) rightTask.cancel();
        rightTask = null;
        rightHeld = false;
        rightStartMs = 0L;
        rightDurationMs = 0L;
    }

    /** Returns {@code true} while a right-hold gesture is in progress. */
    public boolean isRightHeld() {
        return rightHeld;
    }

    /**
     * Returns the duration of the most recent right-hold gesture in milliseconds.
     * Updated at {@link #releaseRightHold()} and stable between gestures.
     */
    public long rightDurationMs() {
        return rightDurationMs;
    }

    private void pollRight() {
        if (!rightHeld) return;
        rightHandler.onTick();
        if (!rightHandler.shouldContinue()) releaseRightHold();
    }

    // ── Sneak-hold lifecycle ──────────────────────────────────────────────────

    /**
     * Begins a sneak-hold gesture. No-op if a sneak-hold is already in progress.
     * Fires {@link SneakHandler#onBegan()} synchronously.
     */
    public void startSneak() {
        if (sneakHeld) return;
        sneakHeld = true;
        sneakStartMs = System.currentTimeMillis();
        sneakDurationMs = 0L;
        sneakHandler.onBegan();
        if (sneakTask != null && !sneakTask.isCancelled()) sneakTask.cancel();
        sneakTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            this::pollSneak,
            null,
            0, 50,
            InputGestureTracker.class, "sneakHold",
            new PredicateRunnablePair(
                () -> !sneakHeld,
                () -> {
                    boolean longPress = sneakDurationMs >= tapThresholdMs;
                    sneakHandler.onReleased(longPress);
                    sneakTask = null;
                }
            )
        );
    }

    /**
     * Marks the sneak-hold released. The deferred tap/hold callback fires on the next
     * scheduler tick. {@link SneakHandler#onEnded()} runs synchronously here.
     * No-op if no sneak-hold is in progress.
     */
    public void releaseSneak() {
        if (!sneakHeld) return;
        sneakHeld = false;
        sneakDurationMs = System.currentTimeMillis() - sneakStartMs;
        sneakHandler.onEnded();
    }

    /**
     * Cancels the sneak-hold gesture without firing any further callbacks. Used during
     * teardown. Idempotent.
     */
    public void abortSneak() {
        if (sneakTask != null && !sneakTask.isCancelled()) sneakTask.cancel();
        sneakTask = null;
        sneakHeld = false;
        sneakStartMs = 0L;
        sneakDurationMs = 0L;
    }

    /** Returns {@code true} while a sneak-hold gesture is in progress. */
    public boolean isSneakHeld() {
        return sneakHeld;
    }

    /**
     * Returns the duration of the most recent sneak-hold gesture in milliseconds.
     * Updated at {@link #releaseSneak()} and stable between gestures.
     */
    public long sneakDurationMs() {
        return sneakDurationMs;
    }

    private void pollSneak() {
        if (!sneakHeld) return;
        sneakHandler.onTick();
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    /**
     * Aborts both gestures and clears all task handles. Safe to call repeatedly and from
     * any state. Must be invoked when the owning entity is destroyed (player leave or
     * plugin disable) to guarantee that no scheduled task outlives its owner.
     */
    public void shutdown() {
        abortRightHold();
        abortSneak();
    }

    // ── Handler contracts ─────────────────────────────────────────────────────

    /**
     * Common gesture lifecycle hooks shared by right-hold and sneak-hold handlers.
     * All methods default to no-ops so implementations may opt in to the events they care about.
     */
    public interface GestureHandler {
        /** Fired once when the gesture begins, synchronously inside {@code start*()}. */
        default void onBegan() {}

        /** Fired once per scheduler tick while the gesture is held. */
        default void onTick() {}

        /**
         * Fired once on the scheduler tick after the gesture is released. The deferred firing
         * lets implementations restore state in {@link #onEnded()} before this observes it.
         *
         * @param longPress {@code true} if the release duration met or exceeded the configured
         *                  tap threshold; {@code false} otherwise
         */
        default void onReleased(boolean longPress) {}

        /** Fired synchronously inside {@code release*()}, before the deferred {@link #onReleased(boolean)}. */
        default void onEnded() {}
    }

    /** Right-hold handler with an additional per-tick continuation predicate. */
    public interface RightHoldHandler extends GestureHandler {
        /**
         * Returns whether the right-hold should continue. Evaluated each tick from
         * {@link #onTick()}. Returning {@code false} triggers a natural release.
         */
        default boolean shouldContinue() {
            return true;
        }
    }

    /** Sneak-hold handler. */
    public interface SneakHandler extends GestureHandler {}
}
