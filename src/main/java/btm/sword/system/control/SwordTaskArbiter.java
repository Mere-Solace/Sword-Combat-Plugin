package btm.sword.system.control;

import btm.sword.Sword;
import btm.sword.system.entity.types.Combatant;
import lombok.Getter;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SwordTaskArbiter {
    private static final Map<Integer, TaskHandle> tasks = new ConcurrentHashMap<>();
    private static final AtomicInteger taskCounter = new AtomicInteger();

    private static final Supplier<Boolean> pauseAll = () -> false; // TODO: determine where this should be gotten from

    // Per-task pause/resume capability (bullet-time ready!)
    public static class TaskHandle {
        private ScheduledFuture<?> future;
        @Getter
        private volatile boolean paused = false;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private TaskHandle(ScheduledFuture<?> future) {
            this.future = future;
        }

        public void pause() { paused = true; }
        public void resume() { paused = false; }
        public boolean cancel() { return cancelled.compareAndSet(false, true) && future.cancel(true); }
        public boolean isCancelled() { return cancelled.get(); }
    }

    /**
     * Runs a task on Bukkit main thread with proper lifecycle management
     */
    public static TaskHandle runTaskTimer(Runnable runnable, int delayMs, int periodMs,
                                          Predicate<TaskHandle> shouldEnd) {

        int taskId = taskCounter.incrementAndGet();
        TaskHandle handle = new TaskHandle(null);

        handle.future = Sword.getScheduler().scheduleAtFixedRate(() -> {
            // Fast-fail checks
            if (handle.cancelled.get()) return;
            if (pauseAll.get() || handle.paused || shouldEnd.test(handle)) {
                handle.cancel();
                cleanupTask(taskId);
                return;
            }

            try {
                // Run on Bukkit thread safely
                Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                    if (!handle.cancelled.get() && !handle.paused) {
                        runnable.run();
                    }
                });
            } catch (Exception e) {
                Sword.getInstance().getLogger().warning("Task " + taskId + " failed: " + e.getMessage());
            }

        }, delayMs, periodMs, TimeUnit.MILLISECONDS);
        tasks.put(taskId, handle);

        return handle;
    }

    /**
     * Pause/resume all tasks (global bullet-time)
     */
    public static void setGlobalTimeScale(double scale) {
        boolean paused = scale <= 0.0;
        tasks.values().forEach(task -> {
            if (paused) task.pause();
            else task.resume();
        });
    }

    /**
     * Pause/resume specific entity tasks (per-entity bullet-time)
     */
    public static void setEntityTimeScale(Combatant entity, double scale) {

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
}
