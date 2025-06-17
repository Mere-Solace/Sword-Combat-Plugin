package btm.sword.system.input;

import btm.sword.system.entity.SwordPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final Runnable runnable;
	private final Function<SwordPlayer, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<SwordPlayer> cannotPerform;
	private final boolean dominantAbility;
	
	private long timeLastExecuted;
	
	public InputAction(Runnable runnable,
	                   Function<SwordPlayer, Long> cooldownCalculation,
	                   Predicate<SwordPlayer> cannotPerform,
	                   boolean dominantAbility) {
		this.runnable = runnable;
		this.cooldownCalculation = cooldownCalculation;
		this.cannotPerform = cannotPerform;
		this.dominantAbility = dominantAbility;
	}
	
	public boolean execute(SwordPlayer executor, BukkitScheduler s, Plugin plugin) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		executor.cannotPerformAction();
		
		if (cannotPerform(executor)) {
			executor.displayDisablingEffect();
			return false;
		}
		if (deltaTime <= cooldown) {
			executor.displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
			return false;
		}
		setTimeLastExecuted();
		BukkitTask abilityTask = s.runTask(plugin, runnable);
		if (dominantAbility) {
			executor.setAbilityTask(abilityTask);
			executor.entity().sendMessage("Setting ability task as: " + abilityTask);
		}
		return true;
	}
	
	public long calcCooldown(SwordPlayer executor) {
		return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
	}
	
	public boolean cannotPerform(SwordPlayer executor) {
		return cannotPerform == null || cannotPerform.test(executor);
	}
	
	public long getTimeLastExecuted() {
		return timeLastExecuted;
	}
	
	public void setTimeLastExecuted() {
		timeLastExecuted = System.currentTimeMillis();
	}
}
