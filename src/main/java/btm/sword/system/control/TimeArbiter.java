package btm.sword.system.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.Debug;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.display.DisplayUtil;
import lombok.Getter;


// these methods provide a central location that every movement/speed call must go through

/**
 * Central scheduling arbiter for all repeating Sword tasks.
 *
 * <p>All {@link TaskHandle} registrations are driven by a single global
 * {@link BukkitRunnable} that fires every server tick. This replaces the
 * previous per-task {@link java.util.concurrent.ScheduledExecutorService}
 * approach, which created ~200+ concurrent futures at runtime.</p>
 *
 * <p>Time-bound tasks automatically apply {@link #GLOBAL_TIME_SCALE} to their
 * effective period on every fire — no rescheduling is needed when the scale
 * changes.</p>
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

    // We now don't even access the maps, so it does not matter if they are maps or have O(1) lookup
    // we will just cancel a task internally, and if it's canceled, it'll clean itself up the next time it tries to run
    // tasks will only schedule themselves one iteration out, and they will be placed into
    // bucketed priority queues based on their next run time in epoch milliseconds.
    /** Lookup maps kept for O(1) cleanup by task ID. */
    private static final Map<Integer, TaskHandle> TIME_BOUND_TASKS = new ConcurrentHashMap<>();
    private static final Supplier<Boolean> PAUSE_ALL = () -> false;
    private static final Map<Integer, TaskHandle> TIME_INDEPENDENT_TASKS = new ConcurrentHashMap<>();



    // private class for task bucket:
    // contains a millisecond range,
    // a Priority queue
    // and a scheduled future object that runs at a specified rate
    //        ^ result of calling Sword.getScheduler().scheduleAtFixedRate()

    // TODO: move to a util class or smth.
    private record Range(int low, int high) {
        public boolean in(int val) {
            return val > low && val <= high;
        }
    }

    private class TaskHandleBucket {
        private final TimeUnit timeUnit;
        private final Range range;
        private final PriorityQueue<TaskHandle> pq = new PriorityQueue<>();
        private ScheduledFuture<?> scheduler;

        public TaskHandleBucket(TimeUnit timeUnit, Range range) {
            this.timeUnit = timeUnit;
            this.range = range;
        }

        public TaskHandleBucket(Range range) {
            this(TimeUnit.MILLISECONDS, range);
        }

        public void tick(long now) {
            while (!pq.isEmpty()
                && pq.peek().nextFireTimeMs < System.currentTimeMillis()) {
                while (!pq.isEmpty() && pq.peek() == null) pq.poll();
                if (pq.isEmpty()) return;
                pq.poll().tick(now);
            }
        }

        public void begin() {
            scheduler = Sword.getScheduler().scheduleAtFixedRate(
                () -> tick(System.currentTimeMillis()),
                0, range.low, timeUnit
            );
        }
    }



    private static final Range R_1MS_5MS = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_1MS_5MS = new PriorityQueue<>();

    private static final Range R_5MS_25MS = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_5MS_25MS = new PriorityQueue<>();

    private static final Range R_25MS_100MS = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_25MS_100MS = new PriorityQueue<>();

    private static final Range R_100MS_1000MS = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_100MS_1000MS = new PriorityQueue<>();

    private static final Range R_1S_5S = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_1S_5S = new PriorityQueue<>();

    private static final Range R_5S_30S = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_5S_30S = new PriorityQueue<>();

    private static final Range R_30S_PLUS = new Range(1, 5);
    private static final PriorityQueue<TaskHandle> TASK_QUEUE_30S_PLUS = new PriorityQueue<>();

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger();

    /**
     * All active (non-cancelled) task handles, driven by the single global tick.
     * Only ever accessed from the main server thread.
     */
    private static final List<TaskHandle> REGISTERED_TASKS = new ArrayList<>();

    // ── Global tick ───────────────────────────────────────────────────────────

    /**
     * Starts the single global tick loop that drives all registered tasks.
     * Must be called once from {@link Sword#onEnable()}.
     */
    public static void startGlobalTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                REGISTERED_TASKS.removeIf(handle -> {
                    if (handle == null || handle.isCancelled()) return true;
                    handle.tick(now);
                    return handle.isCancelled();
                });
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }

    // ── Time scale ────────────────────────────────────────────────────────────

    /**
     * Sets the global time scale (0.0–2.0, where 1.0 is normal speed).
     * Time-bound tasks will apply the new scale automatically on their next fire;
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
     * <p>Instead of owning a {@link java.util.concurrent.ScheduledFuture}, each
     * handle tracks {@code nextFireTimeMs} and is driven by the global tick.
     * Cancellation sets a flag; the global tick removes cancelled handles via
     * {@link List#removeIf}.</p>
     */
    public static final class TaskHandle {

        @Getter
        private final int taskID;
        private final boolean timeBound;

        /** Absolute epoch-ms at which this task should next fire. */
        private long nextFireTimeMs;

        // TODO: need to have a calculation for get time remaining so that when tasks are re-scheduled
        //  we don't immediately schedule all tasks, and keep the time spacing of task scheduling
        //  uniform across time scales.
        @Getter
        private volatile boolean paused = false;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private final Runnable precheckRunnable;
        private final Runnable postcheckRunnable;
        private final Runnable pausedRunnable;
        private final PredicateRunnablePair[] conditionalCallbacks;
        private final int originalDelayMs;
        @Getter
        private final int originalPeriodMs;

        private final Class<?> callingClass;
        private final String callingMethodName;

        private TaskHandle(int taskID, boolean timeBound, long nextFireTimeMs,
                           Runnable precheck, Runnable postcheck, Runnable paused,
                           PredicateRunnablePair[] callbacks, int delay, int period,
                           Class<?> callingClass, String callingMethodName) {
            this.taskID = taskID;
            this.timeBound = timeBound;
            this.nextFireTimeMs = nextFireTimeMs;
            this.precheckRunnable = precheck;
            this.postcheckRunnable = postcheck;
            this.pausedRunnable = paused;
            this.conditionalCallbacks = callbacks;
            this.originalDelayMs = delay;
            this.originalPeriodMs = period;
            this.callingClass = callingClass;
            this.callingMethodName = callingMethodName;
        }

        /**
         * Called every server tick by the global loop.
         * Fires the task if {@code now >= nextFireTimeMs} and reschedules the
         * next fire time using the current time scale for time-bound tasks.
         *
         * @param now {@code System.currentTimeMillis()} captured once per tick
         */
        void tick(long now) {
            if (cancelled.get() || now < nextFireTimeMs) return;

            if (timeBound && (PAUSE_ALL.get() || paused)) {
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
         * Cancels this task. The global tick will remove it from the registered
         * list on the next pass.
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
            precheck, postcheck, paused, callbacks, delayMs, periodMs,
            callingClass, callingMethodName);

        REGISTERED_TASKS.add(handle);

        if (timeBound) {
            TIME_BOUND_TASKS.put(taskID, handle);
        } else {
            TIME_INDEPENDENT_TASKS.put(taskID, handle);
        }
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
            false,
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
     * Cancels all registered tasks and clears state. Called on plugin shutdown.
     */
    public static void shutdown() {
        for (TaskHandle handle : REGISTERED_TASKS) {
            handle.cancelled.set(true);
        }
        REGISTERED_TASKS.clear();
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
