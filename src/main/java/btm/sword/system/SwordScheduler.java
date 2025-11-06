package btm.sword.system;

import btm.sword.Sword;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class SwordScheduler {
    public static void runLater(Runnable runnable, int delay, TimeUnit unit) {
        Sword.getScheduler().schedule(runnable, delay, unit);
    }

    public static void runBukkitTaskLater(Runnable runnable, int delay, TimeUnit unit) {
        Sword.getScheduler().schedule(() -> {
            Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
            }, delay, unit);
    }

    public static <T> void runConsumerNextTick(Consumer<T> consumer, T param) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(param);
            }
        }.runTaskLater(Sword.getInstance(), 1L);
    }
}
