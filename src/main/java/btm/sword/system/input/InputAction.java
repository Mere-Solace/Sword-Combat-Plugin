package btm.sword.system.input;

import btm.sword.Sword;
import btm.sword.system.entity.SwordPlayer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private static final Plugin plugin = Sword.getInstance();
	private static final BukkitScheduler s = Bukkit.getScheduler();
	
	private final Runnable runnable;
	private final Function<SwordPlayer, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<SwordPlayer> cannotPerform;
	
	private long timeLastExecuted;
	
	public InputAction(Runnable runnable,
	                   Function<SwordPlayer, Long> cooldownCalculation,
	                   Predicate<SwordPlayer> cannotPerform) {
		this.runnable = runnable;
		this.cooldownCalculation = cooldownCalculation;
		this.cannotPerform = cannotPerform;
	}
	
	public void execute(SwordPlayer executor) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		if (cannotPerform(executor)) {
			executor.entity().sendMessage("      you're disabled bro. \n\t\tActiveAbility: " + executor.getAbilityCastTask() + ", " + executor.getAbilityCastTaskName() + "  \n\t\tisGrabbing: " + executor.isGrabbing() + ",  \n\t\tisGrabbed: " + executor.isGrabbed() );
			executor.displayDisablingEffect();
		}
		else if (deltaTime <= cooldown) {
			executor.entity().sendMessage("      on cooldown rn");
			executor.displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
		}
		else {
			setTimeLastExecuted();
			executor.entity().sendMessage("      setting ability task");
			s.runTask(plugin, runnable);
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
