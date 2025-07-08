package btm.sword.system;

import btm.sword.Sword;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class SwordScheduler {
	public static void runLater(Runnable runnable, int delay, TimeUnit unit) {
		Sword.getScheduler().schedule(() -> {
			Bukkit.getScheduler().runTask(Sword.getInstance(), runnable);
			}, delay, unit);
	}
}
