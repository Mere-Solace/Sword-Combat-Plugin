package btm.sword.system.control;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;

import org.bukkit.scheduler.BukkitTask;

public class SwordScheduler {

    /**
     * Runs the given {@link Runnable} synchronously (on the main server thread)
     * after the specified delay, measured using the internal asynchronous scheduler.
     * <p>
     * Internally, this first schedules an async delay, and then enqueues the runnable
     * into Bukkit's main-thread scheduler via {@link Bukkit#getScheduler()}.
     * <p>
     *
     * @param runnable the code to execute on the Bukkit main thread
     * @param delay    how long to wait before execution
     * @param unit     the time unit of the delay
     */
    public static ScheduledFuture<?> runBukkitTaskLater(Runnable runnable, int delay, TimeUnit unit) {
        return Sword.getScheduler().schedule(() -> {
            Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
            }, delay, unit);
    }

    public static void runBukkitTask(Runnable runnable) {
        Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
    }

    /**
     * Runs a {@link Consumer} on the next server tick (after a 1-tick delay),
     * passing the provided parameter.
     * <p>
     * This is a simple utility for scheduling dependent logic that must occur
     * one tick after an event, while ensuring main-thread safety.
     *
     * @param consumer the consumer to run
     * @param param    the parameter to pass to the consumer
     * @param <T>      the type of the parameter
     */
    public static <T> void runConsumerNextTick(Consumer<T> consumer, T param) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(param);
            }
        }.runTaskLater(Sword.getInstance(), 1L);
    }
}
