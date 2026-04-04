package btm.sword.system.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * <p>Tasks are routed into one of seven {@link TaskHandleBucket}s based on their
 * base period. Each bucket owns a {@link PriorityBlockingQueue} ordered by
 * {@code nextFireTimeMs} and is driven by a {@link ScheduledFuture} on the shared
 * async executor. When a bucket fires, it drains all due tasks into a single
 * {@code Bukkit.runTask()} batch so task bodies always execute on the main thread.</p>
 *
 * <p>Bucket schedulers start lazily on the first {@link TaskHandleBucket#offer} and
 * stop automatically when the queue empties, so idle buckets consume no executor
 * resources.</p>
 *
 * <p>Time-bound tasks automatically apply {@link #GLOBAL_TIME_SCALE} to their
 * effective period on every fire — no rescheduling needed when the scale changes.</p>
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

    /** Lookup maps kept for O(1) cleanup by task ID. */
    private static final Map<Integer, TaskHandle> TIME_BOUND_TASKS = new ConcurrentHashMap<>();
    private static final Map<Integer, TaskHandle> TIME_INDEPENDENT_TASKS = new ConcurrentHashMap<>();

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger();

    // ── Range ─────────────────────────────────────────────────────────────────

    /**
     * A half-open millisecond interval [{@code low}, {@code high}) used to route
     * tasks to the appropriate {@link TaskHandleBucket}.
     */
    private record Range(int low, int high) {
        /** @return {@code true} if {@code val} falls in [{@code low}, {@code high}) */
        public boolean in(int val) {
            return val >= low && val < high;
        }
    }

    // ── TaskHandleBucket ──────────────────────────────────────────────────────

    /**
     * A single scheduling tier. Tasks whose base period falls within this bucket's
     * {@link Range} are placed here. A {@link ScheduledFuture} fires at {@code pollRate}
     * in {@code pollUnit}, drains all due tasks from the {@link PriorityBlockingQueue},
     * and dispatches them as a single batch to the Bukkit main thread.
     *
     * <p>The scheduler starts lazily on the first {@link #offer} call and cancels itself
     * automatically when the queue empties.</p>
     */
    private static final class TaskHandleBucket {

        private final int pollRate;
        private final TimeUnit pollUnit;
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
                    for (TaskHandle t : batch) {
                        if (t.isCancelled()) continue;
                        try {
                            t.tick(now);
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

    private static final TaskHandleBucket BUCKET_1MS = new TaskHandleBucket(
        1, TimeUnit.MILLISECONDS, new Range(1, 5));
    private static final TaskHandleBucket BUCKET_5MS = new TaskHandleBucket(
        5, TimeUnit.MILLISECONDS, new Range(5, 25));
    private static final TaskHandleBucket BUCKET_25MS = new TaskHandleBucket(
        25, TimeUnit.MILLISECONDS, new Range(25, 100));
    private static final TaskHandleBucket BUCKET_100MS = new TaskHandleBucket(
        100, TimeUnit.MILLISECONDS, new Range(100, 1000));
    private static final TaskHandleBucket BUCKET_1S = new TaskHandleBucket(
        1, TimeUnit.SECONDS, new Range(1000, 5000));
    private static final TaskHandleBucket BUCKET_5S = new TaskHandleBucket(
        5, TimeUnit.SECONDS, new Range(5000, 30000));
    private static final TaskHandleBucket BUCKET_30S = new TaskHandleBucket(
        30, TimeUnit.SECONDS, new Range(30000, Integer.MAX_VALUE));

    private static TaskHandleBucket bucketFor(int periodMs) {
        if (BUCKET_1MS.range.in(periodMs))   return BUCKET_1MS;
        if (BUCKET_5MS.range.in(periodMs))   return BUCKET_5MS;
        if (BUCKET_25MS.range.in(periodMs))  return BUCKET_25MS;
        if (BUCKET_100MS.range.in(periodMs)) return BUCKET_100MS;
        if (BUCKET_1S.range.in(periodMs))    return BUCKET_1S;
        if (BUCKET_5S.range.in(periodMs))    return BUCKET_5S;
        return BUCKET_30S;
    }

    // ── Time scale ────────────────────────────────────────────────────────────

    /**
     * Sets the global time scale (0.0–2.0, where 1.0 is normal speed).
     * Time-bound tasks apply the new scale automatically on their next fire;
     * no rescheduling is required.
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
     * <p>Each handle tracks {@code nextFireTimeMs} and is placed in the appropriate
     * {@link TaskHandleBucket} based on its {@code originalPeriodMs}. After executing
     * on the Bukkit main thread, non-cancelled handles are re-inserted into their
     * bucket's queue via {@link TaskHandleBucket#offer}.</p>
     *
     * <p>Implements {@link Comparable} so {@link PriorityBlockingQueue} can order
     * handles by next fire time.</p>
     */
    public static final class TaskHandle implements Comparable<TaskHandle> {

        @Getter
        private final int taskID;
        private final boolean timeBound;

        /** Absolute epoch-ms at which this task should next fire. */
        private long nextFireTimeMs;

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

        /** The bucket this task belongs to — used for re-insertion after main-thread execution. */
        private TaskHandleBucket ownerBucket;

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
         * Called by the bucket's main-thread batch runner.
         * Fires the task body if due and reschedules the next fire time.
         *
         * @param now {@code System.currentTimeMillis()} captured at batch-drain time
         */
        void tick(long now) {
            if (cancelled.get() || now < nextFireTimeMs) return;

            if (timeBound && paused) {
                if (pausedRunnable != null) pausedRunnable.run();
                scheduleNextFire(now);
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
            scheduleNextFire(now);
        }

        private void scheduleNextFire(long now) {
            long effectivePeriod = timeBound
                ? Math.max(1L, (long) (originalPeriodMs / GLOBAL_TIME_SCALE))
                : originalPeriodMs;
            nextFireTimeMs = now + effectivePeriod;
        }

        /** Pauses this task (time-bound tasks only — runs {@code pausedRunnable} instead). */
        public void pause() {
            paused = true;
        }

        /** Resumes a paused task. */
        public void resume() {
            paused = false;
        }

        /**
         * Cancels this task. The bucket lazily removes it on the next poll.
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

        if (timeBound) {
            TIME_BOUND_TASKS.put(taskID, handle);
        } else {
            TIME_INDEPENDENT_TASKS.put(taskID, handle);
        }

        TaskHandleBucket bucket = bucketFor(periodMs);
        handle.ownerBucket = bucket;
        bucket.offer(handle);

        return handle;
    }

    private static void cleanupTask(int taskId) {
        if (TIME_BOUND_TASKS.remove(taskId) == null)
            TIME_INDEPENDENT_TASKS.remove(taskId);
    }

    // ── Public factory methods ─────────────────────────────────────────────────

    /**
     * Registers a repeating task that is affected by the global time scale.
     * Supports pause/resume via {@code pausedRunnable}.
     *
     * @param precheckRunnable    runs before condition checks each fire; may be null
     * @param postcheckRunnable   runs after condition checks each fire; may be null
     * @param pausedRunnable      runs instead of the normal body when paused; may be null
     * @param delayMs             initial delay in milliseconds before the first fire
     * @param periodMs            repeat interval in milliseconds
     * @param callingClass        class registering this task (for debug logging)
     * @param callingMethodName   method registering this task (for debug logging)
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
     * @param precheckRunnable    runs before condition checks each fire; may be null
     * @param postcheckRunnable   runs after condition checks each fire; may be null
     * @param delayMs             initial delay in milliseconds before the first fire
     * @param periodMs            repeat interval in milliseconds
     * @param callingClass        class registering this task (for debug logging)
     * @param callingMethod       method registering this task (for debug logging)
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
     * @param precheckRunnable     runs before condition checks each fire; may be null
     * @param postcheckRunnable    runs after condition checks each fire; may be null
     * @param delayMs              initial delay in milliseconds
     * @param periodMs             repeat interval in milliseconds
     * @param maxIterations        maximum number of times to fire before auto-cancel
     * @param callingClass         class registering this task
     * @param callingMethod        method registering this task
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
     * No-op hook called from {@link Sword#onEnable()}. Bucket schedulers start
     * lazily on the first task registration; no explicit initialization is needed.
     */
    public static void beginAll() {
        // Buckets are lazy — they start automatically when the first task is offered.
    }

    /**
     * Cancels all active bucket schedulers and marks all queued tasks cancelled.
     * Must be called from {@link Sword#onDisable()}.
     */
    public static void shutdown() {
        BUCKET_1MS.stop();
        BUCKET_5MS.stop();
        BUCKET_25MS.stop();
        BUCKET_100MS.stop();
        BUCKET_1S.stop();
        BUCKET_5S.stop();
        BUCKET_30S.stop();
        TIME_BOUND_TASKS.clear();
        TIME_INDEPENDENT_TASKS.clear();
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
     * @param display           the display to teleport
     * @param destination       target location
     * @param direction         optional facing direction; if null the display keeps its current facing
     * @param teleportDuration  base teleport duration (ms)
     * @param clazz             calling class (for tracing)
     * @param lineNum           calling line number (for tracing)
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
