package btm.sword.system.input;

import btm.sword.system.entity.SwordPlayer;

import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final Runnable runnable;
	private final Function<SwordPlayer, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<SwordPlayer> usablePredicate;
	
	public InputAction(Runnable runnable,
	                   Function<SwordPlayer, Long> cooldownCalculation,
	                   Predicate<SwordPlayer> usablePredicate) {
		this.runnable = runnable;
		this.cooldownCalculation = cooldownCalculation;
		this.usablePredicate = usablePredicate;
	}
	
	public boolean isUsable(SwordPlayer executor) {
		return usablePredicate == null || usablePredicate.test(executor);
	}
	
	// this function should return time in milliseconds
	public long calcCooldown(SwordPlayer executor) {
		return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
	}
	
	public Runnable getRunnable() {
		return runnable;
	}
}
