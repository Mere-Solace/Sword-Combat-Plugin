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
	
	private long timeLastExecuted = 0;
	
	public InputAction(
			Consumer<Combatant> action,
			Function<Combatant, Long> cooldownCalculation,
			Predicate<Combatant> canCastAbility) {
		
		this.action = action;
		this.cooldownCalculation = cooldownCalculation;
		this.canCastAbility = canCastAbility;
	}
	
	public boolean execute(Combatant executor) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		if (deltaTime <= cooldown) {
			((SwordPlayer) executor).displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
			return false;
		}
		if (canCast(executor)) {
			executor.entity().sendMessage("      Running ability");
			action.accept(executor);
			return true;
		}
		else {
			executor.entity().sendMessage("      you're disabled bro. \n\t\tActiveAbility: " + executor.getAbilityCastTask() + "  \n\t\tisGrabbing: " + executor.isGrabbing() + ",  \n\t\tisGrabbed: " + executor.isGrabbed() );
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
