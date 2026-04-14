package btm.sword.system.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.Debug;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.display.DisplayUtil;
import lombok.Getter;


/**
 * Central scheduling arbiter for all repeating Sword tasks.
 *
 * <p>Tasks are routed into one of two scheduling tiers based on their effective period:</p>
 * <ul>
 *   <li><b>HyperBucket</b> (period &lt; {@value #HYPER_THRESHOLD_MS} ms): a single Bukkit
 *   1-tick {@link BukkitRunnable} running every server tick on the main thread. Each tick it
 *   computes {@code catchupCount = max(1, floor(elapsed / effectivePeriodMs))} and calls
 *   {@link TaskHandle#executeBody()} that many times — deterministic sub-tick iteration rates
 *   with no async scheduling jitter.</li>
 *   <li><b>TaskHandleBucket</b> (period &ge; {@value #HYPER_THRESHOLD_MS} ms): a
 *   {@link PriorityBlockingQueue} ordered by {@code nextFireTimeMs}, driven by a lazy
 *   {@link ScheduledFuture}. Due tasks are drained into a single {@code Bukkit.runTask()} batch
 *   so bodies always execute on the main thread.</li>
 * </ul>
 *
 * <p>Time-bound tasks apply {@link #GLOBAL_TIME_SCALE} to their effective period on every fire.
 * If the scale shifts a task's effective period across the {@value #HYPER_THRESHOLD_MS} ms boundary,
 * it migrates between tiers automatically on its next fire.</p>
 */
public final class TimeArbiter {

    private TimeArbiter() {}

    @Getter
    private static volatile double GLOBAL_TIME_SCALE = 1.0;
    private static volatile double GLOBAL_TELEPORT_DURATION_SCALING = 1.0;

    /** Pluggable movement-speed application, updated when the global time scale changes. */
    public static volatile Consumer<SwordEntity> movementSpeedApplication = swordEntity -> {};
    /** Guards against re-entrant {@link #setGlobalTimeScale} calls. */
    public static boolean updatingTimeScale = false;

    /** Lookup map for O(1) cleanup by task ID. */
    private static final Map<Integer, TaskHandle> ALL_TASKS = new ConcurrentHashMap<>();

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger();

    /**
     * Tasks whose {@link TaskHandle#effectivePeriodMs()} is below this threshold are handled
     * by {@link HyperBucket}; tasks at or above it go into a {@link TaskHandleBucket}.
     */
    static final int HYPER_THRESHOLD_MS = 50;

    // ── Range ─────────────────────────────────────────────────────────────────

    /**
     * A half-open millisecond interval [{@code low}, {@code high}) documenting
     * the period range each {@link TaskHandleBucket} covers.
     */
    private record Range(int low, int high) {}

    // ── HyperBucket ───────────────────────────────────────────────────────────

    /**
     * Scheduling tier for tasks whose effective period is below {@value TimeArbiter#HYPER_THRESHOLD_MS} ms.
     *
     * <p>A single {@link BukkitRunnable} runs every Bukkit tick on the main thread. For each live handle
     * it computes {@code catchupCount = max(1, floor(elapsed / effectivePeriodMs))} and calls
     * {@link TaskHandle#executeBody()} that many times, delivering the correct iteration rate even when
     * the main thread was delayed. The runnable starts lazily on the first {@link #offer} call and
     * cancels itself when the handle list empties.</p>
     */
    private static final class HyperBucket {

        private final List<TaskHandle> handles = new CopyOnWriteArrayList<>();
        private volatile BukkitTask tickTask;
        private final AtomicBoolean active = new AtomicBoolean(false);

        /**
         * Adds a handle and starts the tick loop if it was idle.
         *
         * @param handle the task to add
         */
        void offer(TaskHandle handle) {
            handles.add(handle);
            if (active.compareAndSet(false, true)) {
                tickTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        tick();
                    }
                }.runTaskTimer(Sword.getInstance(), 0L, 1L);
            }
        }

        private void tick() {
            long now = System.currentTimeMillis();
            boolean anyAlive = false;

            for (TaskHandle handle : handles) {
                if (handle.isCancelled()) {
                    handles.remove(handle);
                    continue;
                }

                // Initial delay not yet elapsed — nextFireTimeMs holds the first-fire epoch
                if (now < handle.nextFireTimeMs) {
                    anyAlive = true;
                    continue;
                }

                long effPeriod = handle.effectivePeriodMs();

                // Migrate to TaskHandleBucket if time scale slowed this task past the hyper threshold
                if (effPeriod >= HYPER_THRESHOLD_MS) {
                    handles.remove(handle);
                    handle.nextFireTimeMs = now + effPeriod;
                    TaskHandleBucket bucket = bucketFor(effPeriod);
                    handle.ownerBucket = bucket;
                    bucket.offer(handle);
                    anyAlive = true;
                    continue;
                }

                long elapsed = now - handle.lastHyperFireMs;
                int catchupCount = (int) Math.max(1, elapsed / effPeriod);

                for (int i = 0; i < catchupCount; i++) {
                    if (handle.isCancelled()) break;
                    handle.executeBody();
                }

                handle.lastHyperFireMs = now;

                if (!handle.isCancelled()) {
                    anyAlive = true;
                } else {
                    handles.remove(handle);
                }
            }

            // Self-cancel when no live handles remain
            if (!anyAlive && active.compareAndSet(true, false)) {
                tickTask.cancel();
            }
        }

        /**
         * Cancels the tick loop and marks all handles cancelled. Called on plugin shutdown.
         */
        void stop() {
            active.set(false);
            if (tickTask != null) tickTask.cancel();
            handles.forEach(h -> h.cancelled.set(true));
            handles.clear();
        }
    }

    private static final HyperBucket HYPER_BUCKET = new HyperBucket();

    // ── TaskHandleBucket ──────────────────────────────────────────────────────

    /**
     * Scheduling tier for tasks with an effective period at or above {@value TimeArbiter#HYPER_THRESHOLD_MS} ms.
     *
     * <p>A {@link ScheduledFuture} fires at {@code pollRate} in {@code pollUnit}, drains all due tasks
     * from the {@link PriorityBlockingQueue}, and dispatches them as a single batch to the Bukkit main
     * thread. The scheduler starts lazily on the first {@link #offer} call and cancels itself
     * automatically when the queue empties.</p>
     */
    private static final class TaskHandleBucket {

        private final int pollRate;
        private final TimeUnit pollUnit;
        @SuppressWarnings("unused")
        private final Range range;
        private final PriorityBlockingQueue<TaskHandle> pq = new PriorityBlockingQueue<>();
        private volatile ScheduledFuture<?> scheduler;
        private final AtomicBoolean active = new AtomicBoolean(false);

        TaskHandleBucket(int pollRate, TimeUnit pollUnit, Range range) {
            this.pollRate = pollRate;
            this.pollUnit = pollUnit;
            this.range = range;
        }

        /**
         * Adds a task to this bucket and starts the scheduler if it was idle.
         *
         * @param handle the task to enqueue
         */
        void offer(TaskHandle handle) {
            pq.offer(handle);
            if (active.compareAndSet(false, true)) {
                begin();
            }
        }

        private void begin() {
            scheduler = Sword.getScheduler().scheduleAtFixedRate(this::tick, 0, pollRate, pollUnit);
        }

        private void tick() {
            try {
                long now = System.currentTimeMillis();
                List<TaskHandle> batch = new ArrayList<>();
                TaskHandle head;
                while ((head = pq.peek()) != null) {
                    if (head.isCancelled()) {
                        pq.poll(); // lazy cancelled-task cleanup
                        continue;
                    }
                    if (head.nextFireTimeMs > now) break;
                    pq.poll();
                    batch.add(head);
                }
                if (batch.isEmpty()) {
                    checkShutdown();
                    return;
                }
                Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                    long mainThreadNow = System.currentTimeMillis();
                    long drainToExecMs = mainThreadNow - now;
                    if (drainToExecMs > 10) {
                        Debug.system("BUCKET_LAG drainMs=" + drainToExecMs
                            + " bucket=" + pollRate + pollUnit.name().charAt(0)
                            + " batchSize=" + batch.size());
                    }
                    for (TaskHandle t : batch) {
                        if (t.isCancelled()) continue;

                        // Migrate to HyperBucket if time scale sped this task into sub-tick territory
                        long effPeriod = t.effectivePeriodMs();
                        if (effPeriod < HYPER_THRESHOLD_MS) {
                            t.ownerBucket = null;
                            t.lastHyperFireMs = mainThreadNow;
                            HYPER_BUCKET.offer(t);
                            continue;
                        }

                        try {
                            t.tick(mainThreadNow);
                        } catch (Exception e) {
                            Debug.system("Task [" + t.getTaskID() + "] threw during execution: " + e.getMessage());
                            t.cancel();
                        }
                        if (!t.isCancelled()) t.ownerBucket.offer(t);
                    }
                });
                checkShutdown();
            } catch (Exception e) {
                Debug.system("Bucket tick threw unexpectedly: " + e.getMessage());
            }
        }

        /**
         * Cancels the scheduler when the queue is empty. A race guard restarts it
         * if a task was offered between the empty check and the cancel.
         */
        private void checkShutdown() {
            if (pq.isEmpty() && active.compareAndSet(true, false)) {
                scheduler.cancel(false);
                // Race guard: offer() may have won the race between isEmpty() and cancel().
                if (!pq.isEmpty() && active.compareAndSet(false, true)) {
                    begin();
                }
            }
        }

        /**
         * Cancels the scheduler and marks all queued tasks cancelled. Called on plugin shutdown.
         */
        void stop() {
            active.set(false);
            if (scheduler != null) scheduler.cancel(false);
            pq.forEach(t -> t.cancelled.set(true));
            pq.clear();
        }
    }

    // ── Buckets ───────────────────────────────────────────────────────────────

    private static final TaskHandleBucket BUCKET_50MS = new TaskHandleBucket(
        50, TimeUnit.MILLISECONDS, new Range(50, 100));
    private static final TaskHandleBucket BUCKET_100MS = new TaskHandleBucket(
        100, TimeUnit.MILLISECONDS, new Range(100, 1000));
    private static final TaskHandleBucket BUCKET_1S = new TaskHandleBucket(
        1, TimeUnit.SECONDS, new Range(1000, 5000));
    private static final TaskHandleBucket BUCKET_5S = new TaskHandleBucket(
        5, TimeUnit.SECONDS, new Range(5000, 30000));
    private static final TaskHandleBucket BUCKET_30S = new TaskHandleBucket(
        30, TimeUnit.SECONDS, new Range(30000, Integer.MAX_VALUE));

    private static TaskHandleBucket bucketFor(long periodMs) {
        if (periodMs < 100)   return BUCKET_50MS;
        if (periodMs < 1000)  return BUCKET_100MS;
        if (periodMs < 5000)  return BUCKET_1S;
        if (periodMs < 30000) return BUCKET_5S;
        return BUCKET_30S;
    }

    // ── Time scale ────────────────────────────────────────────────────────────

    /**
     * Sets the global time scale (0.0–2.0, where 1.0 is normal speed).
     * Time-bound tasks apply the new scale automatically on their next fire;
     * tasks near the {@value #HYPER_THRESHOLD_MS} ms boundary migrate between tiers lazily.
     *
     * @param timeScale the new time scale
     * @return {@code true} if applied successfully
     */
    public static boolean setGlobalTimeScale(double timeScale) {
        if (updatingTimeScale) return false;

        updatingTimeScale = true;
        try {
            GLOBAL_TIME_SCALE = Math.max(0.0, Math.min(2.0, timeScale));
            GLOBAL_TELEPORT_DURATION_SCALING = Math.max(1.0, 1 / GLOBAL_TIME_SCALE);
            movementSpeedApplication = applicationOfTimeEffects(timeScale);
            SwordEntityArbiter.applyToAllRegisteredEntities(movementSpeedApplication);
            return true;
        } finally {
            updatingTimeScale = false;
        }
    }

    private static Consumer<SwordEntity> applicationOfTimeEffects(double timeScale) {
        Consumer<SwordEntity> application;
        int potionStrength;

        if (timeScale == 1) {
            application = swordEntity -> swordEntity.self().clearActivePotionEffects();
        }
        else if (timeScale < 1) {
            if (timeScale < 0.2) potionStrength = 5;
            else if (timeScale < 0.4) potionStrength = 4;
            else if (timeScale < 0.6) potionStrength = 3;
            else if (timeScale < 0.8) potionStrength = 2;
            else potionStrength = 1;
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, potionStrength);
            PotionEffect slowFall = new PotionEffect(PotionEffectType.SLOW_FALLING, PotionEffect.INFINITE_DURATION, potionStrength);
            application = swordEntity -> {
                swordEntity.self().clearActivePotionEffects();
                swordEntity.self().addPotionEffect(slowness);
                swordEntity.self().addPotionEffect(slowFall);
            };
        }
        else {
            if (timeScale < 1.2) potionStrength = 1;
            else if (timeScale < 1.4) potionStrength = 2;
            else if (timeScale < 1.6) potionStrength = 3;
            else if (timeScale < 1.8) potionStrength = 4;
            else potionStrength = 5;
            PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, potionStrength);
            application = swordEntity -> {
                swordEntity.self().clearActivePotionEffects();
                swordEntity.self().addPotionEffect(speed);
            };
        }

        return application;
    }

    // ── TaskHandle ────────────────────────────────────────────────────────────

    /**
     * A handle to a registered repeating task.
     *
     * <p>Handles in {@link TaskHandleBucket} are ordered by {@code nextFireTimeMs} in a
     * {@link PriorityBlockingQueue}. Handles in {@link HyperBucket} use {@code nextFireTimeMs}
     * only to enforce the initial delay; catch-up timing is tracked via {@code lastHyperFireMs}.</p>
     *
     * <p>Implements {@link Comparable} so {@link PriorityBlockingQueue} can order handles by
     * next fire time.</p>
     */
    public static final class TaskHandle implements Comparable<TaskHandle> {

        @Getter
        private final int taskID;
        private final boolean timeBound;

        /**
         * Absolute epoch-ms at which this task should next fire.
         * For {@link HyperBucket} handles this is set once at creation (the first-fire epoch) and
         * is not updated afterwards. For {@link TaskHandleBucket} handles it is advanced on every
         * fire via {@code scheduleNextFire}.
         */
        long nextFireTimeMs;

        /**
         * Last epoch-ms at which {@link HyperBucket} executed this handle.
         * Initialized to the first-fire epoch; updated to {@code now} after each catch-up batch.
         * Unused by {@link TaskHandleBucket} handles.
         */
        long lastHyperFireMs;

        @Getter
        private volatile boolean paused = false;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private final Runnable precheckRunnable;
        private final Runnable postcheckRunnable;
        private final Runnable pausedRunnable;
        private final PredicateRunnablePair[] conditionalCallbacks;
        @Getter
        private final int originalPeriodMs;

        private final Class<?> callingClass;
        private final String callingMethodName;

        /** The {@link TaskHandleBucket} this handle belongs to; {@code null} when in {@link HyperBucket}. */
        TaskHandleBucket ownerBucket;

        TaskHandle(int taskID, boolean timeBound, long nextFireTimeMs,
                   Runnable precheck, Runnable postcheck, Runnable paused,
                   PredicateRunnablePair[] callbacks, int period,
                   Class<?> callingClass, String callingMethodName) {
            this.taskID = taskID;
            this.timeBound = timeBound;
            this.nextFireTimeMs = nextFireTimeMs;
            this.precheckRunnable = precheck;
            this.postcheckRunnable = postcheck;
            this.pausedRunnable = paused;
            this.conditionalCallbacks = callbacks;
            this.originalPeriodMs = period;
            this.callingClass = callingClass;
            this.callingMethodName = callingMethodName;
        }

        @Override
        public int compareTo(TaskHandle other) {
            return Long.compare(this.nextFireTimeMs, other.nextFireTimeMs);
        }

        /**
         * Returns the effective period in milliseconds. Applies {@link TimeArbiter#GLOBAL_TIME_SCALE}
         * for time-bound tasks; returns {@code originalPeriodMs} unchanged for time-independent tasks.
         *
         * @return effective period in milliseconds, always &ge; 1
         */
        long effectivePeriodMs() {
            return timeBound
                ? Math.max(1L, (long) (originalPeriodMs / GLOBAL_TIME_SCALE))
                : originalPeriodMs;
        }

        /**
         * Executes the task body once. Runs the precheck, evaluates conditional callbacks
         * (cancelling this handle if any trigger), then runs the postcheck. For paused time-bound
         * tasks, runs the paused runnable instead.
         *
         * <p>Called directly by {@link HyperBucket} in its catch-up loop, and by {@link #tick}
         * when dispatched via {@link TaskHandleBucket}.</p>
         */
        void executeBody() {
            if (timeBound && paused) {
                if (pausedRunnable != null) pausedRunnable.run();
                return;
            }
            if (precheckRunnable != null) precheckRunnable.run();
            for (PredicateRunnablePair callback : conditionalCallbacks) {
                if (callback.testAndAccept()) {
                    cancel();
                    return;
                }
            }
            if (postcheckRunnable != null) postcheckRunnable.run();
        }

        /**
         * Called by the {@link TaskHandleBucket} main-thread batch runner.
         * Fires the task body if due and advances {@code nextFireTimeMs}.
         *
         * @param now {@code System.currentTimeMillis()} captured at main-thread dispatch time
         */
        void tick(long now) {
            if (cancelled.get() || now < nextFireTimeMs) return;
            executeBody();
            if (!cancelled.get()) scheduleNextFire(now);
        }

        private void scheduleNextFire(long now) {
            nextFireTimeMs = now + effectivePeriodMs();
        }

        /**
         * Pauses this task. Time-bound tasks run the {@code pausedRunnable} each fire instead of
         * the normal body.
         */
        public void pause() {
            paused = true;
        }

        /** Resumes a paused task. */
        public void resume() {
            paused = false;
        }

        /**
         * Cancels this task. Both {@link HyperBucket} and {@link TaskHandleBucket} lazily remove
         * it on their next iteration.
         *
         * @return {@code true} if this call performed the cancellation
         */
        public boolean cancel() {
            boolean ok = cancelled.compareAndSet(false, true);
            if (ok) {
                Debug.system("cancel task [" + taskID + "] from "
                    + callingClass.getSimpleName() + "." + callingMethodName);
                cleanupTask(taskID);
            }
            return ok;
        }

        /**
         * Returns whether this task has been cancelled.
         *
         * @return {@code true} if cancelled
         */
        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    // ── Internal task creation ─────────────────────────────────────────────────

    private static TaskHandle createTask(boolean timeBound,
                                         Runnable precheck, Runnable postcheck,
                                         Runnable paused, PredicateRunnablePair[] callbacks,
                                         int delayMs, int periodMs,
                                         Class<?> callingClass, String callingMethodName) {
        int taskID = TASK_COUNTER.incrementAndGet();

        Debug.system("create task [" + taskID + "] from "
            + callingClass.getSimpleName() + "." + callingMethodName);

        double scale = timeBound ? GLOBAL_TIME_SCALE : 1.0;
        long firstFireMs = System.currentTimeMillis() + (long) (delayMs / scale);

        TaskHandle handle = new TaskHandle(taskID, timeBound, firstFireMs,
            precheck, postcheck, paused, callbacks, periodMs,
            callingClass, callingMethodName);

        handle.lastHyperFireMs = firstFireMs;

        ALL_TASKS.put(taskID, handle);

        long effPeriod = handle.effectivePeriodMs();
        if (effPeriod < HYPER_THRESHOLD_MS) {
            handle.ownerBucket = null;
            HYPER_BUCKET.offer(handle);
        } else {
            TaskHandleBucket bucket = bucketFor(effPeriod);
            handle.ownerBucket = bucket;
            bucket.offer(handle);
        }

        return handle;
    }

    private static void cleanupTask(int taskId) {
        ALL_TASKS.remove(taskId);
    }

    // ── Public factory methods ─────────────────────────────────────────────────

    /**
     * Registers a repeating task that is affected by the global time scale.
     * Supports pause/resume via {@code pausedRunnable}.
     *
     * @param precheckRunnable     runs before condition checks each fire; may be null
     * @param postcheckRunnable    runs after condition checks each fire; may be null
     * @param pausedRunnable       runs instead of the normal body when paused; may be null
     * @param delayMs              initial delay in milliseconds before the first fire
     * @param periodMs             repeat interval in milliseconds
     * @param callingClass         class registering this task (for debug logging)
     * @param callingMethodName    method registering this task (for debug logging)
     * @param conditionalCallbacks auto-cancel conditions checked each fire
     * @return a {@link TaskHandle} that can be paused, resumed, or cancelled
     */
    public static TaskHandle runTimeBoundBukkitTaskOnTimer(@Nullable Runnable precheckRunnable,
                                                           @Nullable Runnable postcheckRunnable,
                                                           @Nullable Runnable pausedRunnable,
                                                           int delayMs,
                                                           int periodMs,
                                                           Class<?> callingClass,
                                                           String callingMethodName,
                                                           PredicateRunnablePair... conditionalCallbacks) {
        return createTask(true, precheckRunnable, postcheckRunnable, pausedRunnable,
            conditionalCallbacks, delayMs, periodMs, callingClass, callingMethodName);
    }

    /**
     * Registers a repeating task that is <em>not</em> affected by the global time scale.
     *
     * @param precheckRunnable     runs before condition checks each fire; may be null
     * @param postcheckRunnable    runs after condition checks each fire; may be null
     * @param delayMs              initial delay in milliseconds before the first fire
     * @param periodMs             repeat interval in milliseconds
     * @param callingClass         class registering this task (for debug logging)
     * @param callingMethod        method registering this task (for debug logging)
     * @param conditionalCallbacks auto-cancel conditions checked each fire
     * @return a {@link TaskHandle} that can be cancelled
     */
    public static TaskHandle runTimeIndependentBukkitTaskOnTimer(@Nullable Runnable precheckRunnable,
                                                                 @Nullable Runnable postcheckRunnable,
                                                                 int delayMs,
                                                                 int periodMs,
                                                                 Class<?> callingClass,
                                                                 String callingMethod,
                                                                 PredicateRunnablePair... conditionalCallbacks) {
        return createTask(false, precheckRunnable, postcheckRunnable, null,
            conditionalCallbacks, delayMs, periodMs, callingClass, callingMethod);
    }

    /**
     * Registers a repeating task that automatically cancels after {@code maxIterations} fires.
     * Scales with {@link #GLOBAL_TIME_SCALE}.
     *
     * @param precheckRunnable      runs before condition checks each fire; may be null
     * @param postcheckRunnable     runs after condition checks each fire; may be null
     * @param delayMs               initial delay in milliseconds
     * @param periodMs              repeat interval in milliseconds
     * @param maxIterations         maximum number of times to fire before auto-cancel
     * @param callingClass          class registering this task
     * @param callingMethod         method registering this task
     * @param lastIterationCallback runs on the final iteration; may be null
     * @param conditionalCallbacks  additional auto-cancel conditions
     * @return a {@link TaskHandle} that can be cancelled early
     */
    @SuppressWarnings("all")
    public static TaskHandle runFixedIterationTaskTimer(@Nullable Runnable precheckRunnable,
                                                        @Nullable Runnable postcheckRunnable,
                                                        int delayMs,
                                                        int periodMs,
                                                        int maxIterations,
                                                        Class<?> callingClass,
                                                        String callingMethod,
                                                        @Nullable Runnable lastIterationCallback,
                                                        PredicateRunnablePair... conditionalCallbacks) {
        int[] iteration = {0};
        PredicateRunnablePair[] endPredicates = Arrays.copyOf(
            conditionalCallbacks,
            conditionalCallbacks.length + 1
        );
        endPredicates[conditionalCallbacks.length] = new PredicateRunnablePair(
            () -> iteration[0] > maxIterations, lastIterationCallback
        );

        return createTask(
            true,
            () -> {
                if (precheckRunnable != null) precheckRunnable.run();
                iteration[0]++;
            },
            postcheckRunnable, null,
            endPredicates,
            delayMs, periodMs,
            callingClass, callingMethod
        );
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * No-op hook called from {@link Sword#onEnable()}. Both scheduling tiers start lazily on the
     * first task registration; no explicit initialization is needed.
     */
    public static void beginAll() {
        // Both HyperBucket and TaskHandleBuckets start lazily on first offer.
    }

    /**
     * Cancels all active schedulers and marks all queued tasks cancelled.
     * Must be called from {@link Sword#onDisable()}.
     */
    public static void shutdown() {
        HYPER_BUCKET.stop();
        BUCKET_50MS.stop();
        BUCKET_100MS.stop();
        BUCKET_1S.stop();
        BUCKET_5S.stop();
        BUCKET_30S.stop();
        ALL_TASKS.clear();
    }

    // ── Display / physics utilities ───────────────────────────────────────────

    /**
     * Sets the velocity of an entity. Exists as a central hook for future
     * time-scale-aware velocity adjustment.
     *
     * @param entity   the entity to move
     * @param velocity the velocity vector to apply
     */
    public static void setVelocity(Entity entity, Vector velocity) {
        entity.setVelocity(velocity.clone());
        // TODO: find a way to lessen velocity while still getting the entity to the same spot if time is slowed down/sped up
    }

    /**
     * Teleports a display entity with a smooth teleport duration scaled by the global time scale.
     *
     * @param display          the display to teleport
     * @param destination      target location
     * @param direction        optional facing direction; if null the display keeps its current facing
     * @param teleportDuration base teleport duration in milliseconds
     * @param clazz            calling class (for tracing)
     * @param lineNum          calling line number (for tracing)
     */
    public static void teleportDisplay(Display display, Location destination, @Nullable Vector direction,
                                       int teleportDuration, Class<?> clazz, int lineNum) {
        DisplayUtil.setSmoothTeleportDuration(display,
            teleportDuration == 0 ? 0 : Math.max(1, (int) (teleportDuration * GLOBAL_TELEPORT_DURATION_SCALING))
        );
        if (direction == null) {
            display.teleport(destination);
        } else {
            destination.setDirection(direction);
            display.teleport(destination);
        }
    }

    private static final double TELEPORT_NORMALIZING_FACTOR = 3;

    /**
     * Applies a transformation to a display entity with a duration scaled by the global time scale.
     *
     * @param display           the display to transform
     * @param transformation    the new transformation
     * @param transformDuration base duration in milliseconds
     */
    public static void setDisplayTransformation(Display display, Transformation transformation, int transformDuration) {
        int effectiveDuration = transformDuration == 0 ? 0
            : (int) (SwordTimeUnit.millisToTicks(transformDuration) * TELEPORT_NORMALIZING_FACTOR * GLOBAL_TELEPORT_DURATION_SCALING);

        DisplayUtil.setInterpolationValues(display, 0, effectiveDuration);
        display.setTransformation(transformation);
    }
}
