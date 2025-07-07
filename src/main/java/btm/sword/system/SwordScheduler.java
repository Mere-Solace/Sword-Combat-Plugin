package btm.sword.system;

import btm.sword.Sword;
import org.bukkit.Bukkit;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SwordScheduler {
	public static void runLater(Runnable runnable, int delay, TimeUnit unit) {
		ScheduledExecutorService scheduler = Sword.getScheduler();
		scheduler.schedule(() -> {
			Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
		}, delay, unit);
	}
}
