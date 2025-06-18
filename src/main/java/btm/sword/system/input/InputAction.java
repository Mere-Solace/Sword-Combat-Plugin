package btm.sword.system.input;

import btm.sword.system.entity.SwordPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final String name;
	private final Runnable runnable;
	private final Function<SwordPlayer, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<SwordPlayer> cannotPerform;
	
	private long timeLastExecuted;
	
	public InputAction(String name,
	                   Runnable runnable,
	                   Function<SwordPlayer, Long> cooldownCalculation,
	                   Predicate<SwordPlayer> cannotPerform) {
		this.name = name;
		this.runnable = runnable;
		this.cooldownCalculation = cooldownCalculation;
		this.cannotPerform = cannotPerform;
	}
	
	public void execute(SwordPlayer executor, BukkitScheduler s, Plugin plugin) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		if (cannotPerform(executor)) {
			executor.entity().sendMessage("      you're disabled bro. \n\t\tActiveAbility: " + executor.getAbilityTask() + ", " + executor.getAbilityTaskName() + "  \n\t\tisGrabbing: " + executor.isGrabbing() + ",  \n\t\tisGrabbed: " + executor.isGrabbed() );
			executor.displayDisablingEffect();
		}
		else if (deltaTime <= cooldown) {
			executor.entity().sendMessage("      on cooldown rn");
			executor.displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
		}
		else {
			setTimeLastExecuted();
			executor.entity().sendMessage("      setting ability task");
			executor.setAbilityTask(s.runTask(plugin, runnable), name);
		}
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
