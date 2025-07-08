package btm.sword.system.input;

import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final Consumer<Combatant> action;
	private final Function<Combatant, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<Combatant> canCastAbility;
	private final boolean displayCooldown;
	private final boolean displayDisabled;
	
	private long timeLastExecuted = 0;
	
	public InputAction(
			Consumer<Combatant> action,
			Function<Combatant, Long> cooldownCalculation,
			Predicate<Combatant> canCastAbility,
			boolean displayCooldown,
			boolean displayDisabled) {
		this.action = action;
		this.cooldownCalculation = cooldownCalculation;
		this.canCastAbility = canCastAbility;
		this.displayCooldown = displayCooldown;
		this.displayDisabled = displayDisabled;
	}
	
	public boolean execute(Combatant executor) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		if (deltaTime <= cooldown) {
			if (displayCooldown)
				((SwordPlayer) executor).displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
			return false;
		}
		if (canCast(executor)) {
			action.accept(executor);
			setTimeLastExecuted();
			return true;
		}
		else {
			if (displayDisabled)
				((SwordPlayer) executor).displayDisablingEffect();
			return false;
		}
	}
	
	public long calcCooldown(Combatant executor) {
		return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
	}
	
	public boolean canCast(Combatant executor) {
		return canCastAbility == null || canCastAbility.test(executor);
	}
	
	public long getTimeLastExecuted() {
		return timeLastExecuted;
	}
	
	public void setTimeLastExecuted() {
		timeLastExecuted = System.currentTimeMillis();
	}
}
