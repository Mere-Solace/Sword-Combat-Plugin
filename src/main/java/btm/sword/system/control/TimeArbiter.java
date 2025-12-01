package btm.sword.system.control;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import btm.sword.utility.Debug;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import lombok.Getter;

public class TimeArbiter {
    private static volatile double GLOBAL_TIME_SCALE = 1.0;
    // these methods provide a central location that every movement/speed call must go through

    public static void setGlobalTimeScale(double scale) {
        GLOBAL_TIME_SCALE = Math.max(0.0, Math.min(2.0, scale));
        // All entities automatically recalculate on next tick
    }

    public static double getGlobalTimeScale() {
        return GLOBAL_TIME_SCALE;
    }

    private static final Map<Integer, TaskHandle> tasks = new ConcurrentHashMap<>();
    private static final AtomicInteger taskCounter = new AtomicInteger();

    private static final Supplier<Boolean> pauseAll = () -> false; // TODO: determine from where this should come

    public static class TaskHandle {
        private ScheduledFuture<?> future;
        @Getter
        private volatile boolean paused = false;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private TaskHandle(ScheduledFuture<?> future) {
            this.future = future;
        }

        public void pause() {
            paused = true;
        }
        public void resume() {
            paused = false;
        }
        public boolean cancel() {
            return cancelled.compareAndSet(false, true) && future.cancel(true);
        }
        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    /**
     * Runs a task on Bukkit main thread with proper lifecycle management
     */
    public static TaskHandle runTaskTimer(Runnable precheckRunnable, Runnable postcheckRunnable,
                                          int delayMs, int periodMs,
                                          Runnable pausedRunnable,
                                          PredicateRunnablePair... conditionalCallbacks) {

        int taskId = taskCounter.incrementAndGet();
        TaskHandle handle = new TaskHandle(null);

        handle.future = Sword.getScheduler().scheduleAtFixedRate(() ->
            Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                if (handle.cancelled.get()) return;

                if (pauseAll.get() || handle.paused) { // task paused, not ended. upon resume, will continue.
                    return;
                }

                precheckRunnable.run();

                for (PredicateRunnablePair callback : conditionalCallbacks) {
                    if (callback.testAndAccept()) {
                        handle.cancel();
                        cleanupTask(taskId);
                        return;
                    }
                }

                postcheckRunnable.run();
            }), delayMs, periodMs + 1, TimeUnit.MILLISECONDS);
        tasks.put(taskId, handle);

        return handle;
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
