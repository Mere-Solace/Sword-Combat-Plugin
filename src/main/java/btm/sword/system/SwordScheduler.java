package btm.sword.system;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;

/**
 * A Scheduler utility that provides scheduling more granular than Minecraft's tick system.
 * <p>
 * This class provides methods for running tasks later either:
 * </p>
 * <ul>
 *     <li>Asynchronously, through the internal Sword scheduler.</li>
 *     <li>Synchronously (on the main server thread), by wrapping a Bukkit task.</li>
 * </ul>
 * <p>
 * <b>Important:</b> Directly interacting with the Bukkit API from
 * {@link #runLater(Runnable, int, TimeUnit)} is <em>not thread-safe</em>.
 * Use {@link #runBukkitTaskLater(Runnable, int, TimeUnit)} or
 * {@link #runConsumerNextTick(Consumer, Object)} when you need to interact with entities,
 * worlds, or any other Bukkit-managed data.
 */
public class SwordScheduler {
    /**
     * Runs the given {@link Runnable} asynchronously after the specified delay.
     * <p>
     * This delegates to {@link Sword#getScheduler()} and is typically backed by
     * a {@link java.util.concurrent.ScheduledExecutorService}.
     * <p>
     * Do <b>not</b> call Bukkit API methods inside this runnable, as it will execute
     * off the main thread.
     *
     * @param runnable the code to execute
     * @param delay    how long to wait before execution
     * @param unit     the time unit of the delay
     */
    public static void runLater(Runnable runnable, int delay, TimeUnit unit) {
        Sword.getScheduler().schedule(runnable, delay, unit);
    }

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
    public static void runBukkitTaskLater(Runnable runnable, int delay, TimeUnit unit) {
        Sword.getScheduler().schedule(() -> {
            Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
            }, delay, unit);
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
