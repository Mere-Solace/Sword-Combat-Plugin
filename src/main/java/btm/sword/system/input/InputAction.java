package btm.sword.system.input;

import btm.sword.system.entity.SwordPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final Runnable runnable;
	private final Function<SwordPlayer, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<SwordPlayer> usable;
	
	private final boolean sameItemRequired;
	private long timeLastExecuted;
	
	public InputAction(Runnable runnable,
	                   Function<SwordPlayer, Long> cooldownCalculation,
	                   Predicate<SwordPlayer> usable,
	                   boolean sameItemRequired) {
		this.runnable = runnable;
		this.cooldownCalculation = cooldownCalculation;
		this.usable = usable;
		this.sameItemRequired = sameItemRequired;
	}
	
	public void execute(SwordPlayer executor, BukkitScheduler s, Plugin plugin) {
		long cur = System.currentTimeMillis();
		long time = cur - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		if (!isUsable(executor)) {
			executor.displayDisablingEffect();
		}
		else if (time <= cooldown) {
			executor.displayCooldown(Math.max(0, cooldown - (cur - getTimeLastExecuted())));
		}
		else {
			executor.setAbilityTask(s.runTask(plugin, runnable));
			setTimeLastExecuted();
			executor.displayInputSequence();
		}
	}
	
	public long calcCooldown(SwordPlayer executor) {
		return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
	}
	
	public boolean isUsable(SwordPlayer executor) {
		return usable == null || usable.test(executor);
	}
	
	public boolean isSameItemRequired() {
		return sameItemRequired;
	}
	
	public long getTimeLastExecuted() {
		return timeLastExecuted;
	}
	
	public void setTimeLastExecuted() {
		timeLastExecuted = System.currentTimeMillis();
	}
}
