package btm.sword.system.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import lombok.Getter;

// these methods provide a central location that every movement/speed call must go through
public class TimeArbiter {
    @Getter
    private static volatile double GLOBAL_TIME_SCALE = 1.0;
    public static boolean updatingTimeScale = false;

    public static boolean setGlobalTimeScale(double timeScale) {
        if (updatingTimeScale) return false;

        updatingTimeScale = true;
        try {
            GLOBAL_TIME_SCALE = Math.max(0.0, Math.min(2.0, timeScale));

            // Collect all tasks to restart
            List<TaskHandle> tasksToRestart = new ArrayList<>(tasks.values());

            // Cancel all existing tasks
            tasksToRestart.forEach(TaskHandle::cancel);

            // Recreate all tasks with new period
            for (TaskHandle oldTask : tasksToRestart) {
                int newPeriodMs = (int) (oldTask.getOriginalPeriodMs() / GLOBAL_TIME_SCALE);
                TaskHandle newTask = oldTask.recreateWithPeriod(newPeriodMs);
                tasks.put(newTask.getTaskID(), newTask);  // Assuming TaskHandle has getId()
            }

            SwordEntityArbiter.applyToAllRegisteredEntities(applicationOfTimeEffects(timeScale));

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


    private static final Map<Integer, TaskHandle> tasks = new ConcurrentHashMap<>();
    private static final AtomicInteger taskCounter = new AtomicInteger();

    private static final Supplier<Boolean> pauseAll = () -> false; // TODO: determine from where this should come

    public static class TaskHandle {
        @Getter
        private final int taskID;
        private ScheduledFuture<?> future;
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

        private TaskHandle(int taskID, ScheduledFuture<?> future,
                           Runnable precheck, Runnable postcheck, Runnable paused,
                           PredicateRunnablePair[] callbacks, int delay, int period) {
            this.taskID = taskID;
            this.future = future;
            this.precheckRunnable = precheck;
            this.postcheckRunnable = postcheck;
            this.pausedRunnable = paused;
            this.conditionalCallbacks = callbacks;
            this.originalDelayMs = delay;
            this.originalPeriodMs = period;
        }

        public void pause() {
            paused = true;
        }
        public void resume() {
            paused = false;
        }
        public boolean cancel() {
            try {
                return cancelled.compareAndSet(false, true) && future.cancel(true);
            } catch (RuntimeException e) {
                Sword.getInstance().getLogger().warning("Error when canceling a TaskHandler: " + e);
            }
            return false;
        }
        public boolean isCancelled() {
            return cancelled.get();
        }

        public TaskHandle recreateWithPeriod(int newPeriodMs) {
            if (!cancelled.get()) {
                cancel();  // Stop old task
            }
            return TimeArbiter.createTask(precheckRunnable, postcheckRunnable, pausedRunnable,
                conditionalCallbacks, originalDelayMs, newPeriodMs);
        }
    }

    /**
     * Internal method to create tasks (used by constructor and recreation)
     */
    private static TaskHandle createTask(Runnable precheck, Runnable postcheck,
                                         Runnable paused, PredicateRunnablePair[] callbacks,
                                         int delayMs, int periodMs) {
        int taskID = taskCounter.incrementAndGet();
        TaskHandle handle = new TaskHandle(taskID,
            null, precheck, postcheck, paused, callbacks, delayMs, periodMs);

        handle.future = Sword.getScheduler().scheduleAtFixedRate(() ->
            Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                if (handle.cancelled.get()) return;
                if (pauseAll.get() || handle.paused) {
                    if (handle.pausedRunnable != null) handle.pausedRunnable.run();
                    return;
                }

                if (handle.precheckRunnable != null) handle.precheckRunnable.run();

                for (PredicateRunnablePair callback : handle.conditionalCallbacks) {
                    if (callback.testAndAccept()) {
                        handle.cancel();
                        cleanupTask(taskID);
                        return;
                    }
                }

                if (handle.postcheckRunnable != null) handle.postcheckRunnable.run();
            }),
            (int) (delayMs / GLOBAL_TIME_SCALE),
            (int) (Math.max(1, periodMs / GLOBAL_TIME_SCALE)),
            TimeUnit.MILLISECONDS);

        tasks.put(taskID, handle);
        return handle;
    }

    /**
     * Public factory method
     */
    public static TaskHandle runTimeAffectedTaskOnTimer(Runnable precheckRunnable,
                                                        Runnable postcheckRunnable,
                                                        Runnable pausedRunnable,
                                                        int periodMs,
                                                        PredicateRunnablePair... conditionalCallbacks) {
        return createTask(precheckRunnable, postcheckRunnable, pausedRunnable,
            conditionalCallbacks, 0, periodMs);
    }

    private static void cleanupTask(int taskId) {
        tasks.remove(taskId);
    }

    /**
     * Cancel all tasks (shutdown hook)
     */
    public static void shutdown() {
        tasks.values().forEach(TaskHandle::cancel);
        tasks.clear();
    }

    public static void setVelocity(Entity entity, Vector velocity) {
        entity.setVelocity(velocity.clone());
        // TODO: find a way to lessen velocity while still getting the entity to the same spot if time is slowed down/sped up
    }
}
