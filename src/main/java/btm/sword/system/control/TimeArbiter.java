package btm.sword.system.control;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import lombok.Setter;


// these methods provide a central location that every movement/speed call must go through
public class TimeArbiter {
    @Getter
    private static volatile double GLOBAL_TIME_SCALE = 1.0;
    private static volatile double GLOBAL_TELEPORT_DURATION_SCALING = 1.0;
    public static boolean updatingTimeScale = false;
    public static boolean restartingAllTasks = false;

    private static final Map<Integer, TaskHandle> timeBoundTasks = new ConcurrentHashMap<>();
    private static final Supplier<Boolean> pauseAll = () -> false; // TODO: determine from where this should come

    private static final Map<Integer, TaskHandle> timeIndependentTasks = new ConcurrentHashMap<>();

    private static final AtomicInteger taskCounter = new AtomicInteger();


    public static boolean setGlobalTimeScale(double timeScale) {

        // When timescale is set, need to update all displays and places where displays are located
        // So that this method can set their smooth teleport duration.

        // TODO: change speed for newly spawned and registered entities (not this class but needed to get it down)

        if (updatingTimeScale) return false;

        updatingTimeScale = true;
        try {
            GLOBAL_TIME_SCALE = Math.max(0.0, Math.min(2.0, timeScale));
            GLOBAL_TELEPORT_DURATION_SCALING = Math.max(1.0, 1 / GLOBAL_TIME_SCALE);

            // Mark all uncancelled tasks to be restarted
            for (TaskHandle task : timeBoundTasks.values()) {
                if (!task.isCancelled()) {  // Only restart non-cancelled tasks
                    task.setMarkedToRestart(true);
                }
            }
//
            SwordEntityArbiter.applyToAllRegisteredEntities(applicationOfTimeEffects(timeScale));

            return true;
        } finally {
            updatingTimeScale = false;
        }
    }

    private static void processRestartRequest(int taskID, boolean timeBound) {
        TaskHandle handle = timeBound ? timeBoundTasks.get(taskID) : timeIndependentTasks.get(taskID);
        if (handle == null) return;

        handle.future.cancel(true);
        handle.rescheduleFuture();
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
            PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 99999999, potionStrength);
            PotionEffect slowFall = new PotionEffect(PotionEffectType.SLOW_FALLING, 99999999, potionStrength);
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
            PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 99999999, potionStrength);
            application = swordEntity -> {
                swordEntity.self().clearActivePotionEffects();
                swordEntity.self().addPotionEffect(speed);
            };
        }

        return application;
    }

    public static class TaskHandle {
        @Getter
        private final int taskID;
        private final boolean timeBound;

        private ScheduledFuture<?> future;
        @Getter
        private volatile boolean paused = false;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        @Getter
        @Setter
        private boolean markedToRestart = false;

        private final Runnable precheckRunnable;
        private final Runnable postcheckRunnable;
        private final Runnable pausedRunnable;
        private final PredicateRunnablePair[] conditionalCallbacks;
        private final int originalDelayMs;
        @Getter
        private final int originalPeriodMs;

        // Testing Attributes
        private final Class<?> callingClass;
        private final String callingMethodName;

        private TaskHandle(int taskID, boolean timeBound, ScheduledFuture<?> future,
                           Runnable precheck, Runnable postcheck, Runnable paused,
                           PredicateRunnablePair[] callbacks, int delay, int period,
                           Class<?> callingClass, String callingMethodName) {

            this.taskID = taskID;
            this.timeBound = timeBound;
            this.future = future;
            this.precheckRunnable = precheck;
            this.postcheckRunnable = postcheck;
            this.pausedRunnable = paused;
            this.conditionalCallbacks = callbacks;
            this.originalDelayMs = delay;
            this.originalPeriodMs = period;

            // For testing purposes:
            this.callingClass = callingClass;
            this.callingMethodName = callingMethodName;
        }

        public void pause() {
            paused = true;
        }
        public void resume() {
            paused = false;
        }
        public boolean cancel() {
            boolean successfullyCanceled = false;
            try {
                successfullyCanceled = cancelled.compareAndSet(false, true) && future.cancel(true);
            } catch (RuntimeException e) {
                Sword.getInstance().getLogger().warning("Error when canceling a TaskHandler: " + e);
            }

            Debug.debug(TimeArbiter.class, 183, "Cancelling task [ " + taskID + " ] " +
                " : Successful ? " + successfullyCanceled + " From: " + callingClass + " " + callingMethodName);

            cleanupTask(taskID);

            return successfullyCanceled;
        }
        public boolean isCancelled() {
            return cancelled.get();
        }

        public void rescheduleFuture() {
            if (!cancelled.get()) {
                future.cancel(false);  // Stop old future
            }
            scheduleTaskFuture(this);
            setMarkedToRestart(false);
        }
    }

    /**
     * Internal method to create tasks (used by constructor and recreation)
     */
    private static TaskHandle createTask(boolean timeBound,
                                         Runnable precheck, Runnable postcheck,
                                         Runnable paused, PredicateRunnablePair[] callbacks,
                                         int delayMs, int periodMs,
                                         Class<?> callingClass, String callingMethodName) {
        int taskID = taskCounter.incrementAndGet();

        Debug.debug(TimeArbiter.class, 211, "Creating new TaskHandle: [ " + taskID +
            " ] From: " + callingClass + " " + callingMethodName);

        TaskHandle handle = new TaskHandle(taskID, timeBound,
            null, precheck, postcheck, paused, callbacks, delayMs, periodMs,
            callingClass, callingMethodName);

        scheduleTaskFuture(handle);

        if (timeBound) {
            timeBoundTasks.put(taskID, handle);
        } else {
            timeIndependentTasks.put(taskID, handle);
        }
        return handle;
    }

    private static void scheduleTaskFuture(TaskHandle handle) {

        int effectivePeriod = handle.timeBound ?
            (int) (Math.max(1, handle.originalPeriodMs / GLOBAL_TIME_SCALE)) :
            handle.originalPeriodMs;

        handle.future = Sword.getScheduler().scheduleAtFixedRate(() ->
                Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                    if (handle.isMarkedToRestart()) {
                        int newPeriod = (int) (handle.originalPeriodMs / GLOBAL_TIME_SCALE);
                        if (effectivePeriod != newPeriod) {
                            processRestartRequest(handle.taskID, handle.timeBound);
                            return;
                        }
                        else {
                            handle.setMarkedToRestart(false);
                        }
                    }
                    if (handle.cancelled.get()) return;
                    if (handle.timeBound && (pauseAll.get() || handle.paused)) {
                        if (handle.pausedRunnable != null) handle.pausedRunnable.run();
                        return;
                    }

                    if (handle.precheckRunnable != null) handle.precheckRunnable.run();

                    for (PredicateRunnablePair callback : handle.conditionalCallbacks) {
                        if (callback.testAndAccept()) {
                            handle.cancel();
                            cleanupTask(handle.taskID);
                            return;
                        }
                    }

                    if (handle.postcheckRunnable != null) handle.postcheckRunnable.run();
                }),
            (int) (handle.originalDelayMs / GLOBAL_TIME_SCALE),
            (int) (Math.max(1, handle.originalPeriodMs / GLOBAL_TIME_SCALE)),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Public factory method
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

    private static void cleanupTask(int taskId) {
        if (timeBoundTasks.remove(taskId) == null)
            timeIndependentTasks.remove(taskId);
    }

    /**
     * Cancel all tasks (shutdown hook)
     */
    public static void shutdown() {
        timeBoundTasks.values().forEach(TaskHandle::cancel);
        timeBoundTasks.clear();
    }

    public static void setVelocity(Entity entity, Vector velocity) {
        entity.setVelocity(velocity.clone());
        // TODO: find a way to lessen velocity while still getting the entity to the same spot if time is slowed down/sped up
    }

    public static void teleportDisplay(Display display, Location destination, @Nullable Vector direction, int teleportDuration) {
        DisplayUtil.setSmoothTeleportDuration(display,
            teleportDuration == 0 ? 0 : Math.max(1, (int) (teleportDuration * GLOBAL_TELEPORT_DURATION_SCALING))
        );
        if (direction == null) {
            display.teleport(destination);
        } else {
            destination.setDirection(direction); // doing in two lines just in case, I don't believe it affects it though.
            display.teleport(destination);
        }
    }

    private static final double TELEPORT_NORMALIZING_FACTOR = 3;

    /**
     *
     * @param display the display affected
     * @param transformation the new transformation to be applied
     * @param transformDuration millisecond duration of teleport (will be converted to ticks in this method)
     */
    public static void setDisplayTransformation(Display display, Transformation transformation, int transformDuration) {
        int effectiveDuration = transformDuration == 0 ? 0 :
            (int) (SwordTimeUnit.millisToTicks(transformDuration) * TELEPORT_NORMALIZING_FACTOR * GLOBAL_TELEPORT_DURATION_SCALING);

        DisplayUtil.setInterpolationValues(display, 0, effectiveDuration);

        display.setTransformation(transformation);
    }
}
