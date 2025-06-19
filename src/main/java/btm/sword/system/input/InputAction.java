package btm.sword.system.input;

import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class InputAction {
	private final Consumer<Combatant> action;
	private final Function<Combatant, Long> cooldownCalculation; // this function should return time in milliseconds
	private final Predicate<Combatant> cannotPerform;
	
	private long timeLastExecuted = 0;
	
	public InputAction(
			Consumer<Combatant> action,
			Function<Combatant, Long> cooldownCalculation,
			Predicate<Combatant> cannotPerform) {
		
		this.action = action;
		this.cooldownCalculation = cooldownCalculation;
		this.cannotPerform = cannotPerform;
	}
	
	public void execute(Combatant executor) {
		long currentTime = System.currentTimeMillis();
		long deltaTime = currentTime - getTimeLastExecuted();
		long cooldown = calcCooldown(executor);
		
		if (cannotPerform(executor)) {
			executor.entity().sendMessage("      you're disabled bro. \n\t\tActiveAbility: " + executor.getAbilityCastTask() + "  \n\t\tisGrabbing: " + executor.isGrabbing() + ",  \n\t\tisGrabbed: " + executor.isGrabbed() );
			((SwordPlayer) executor).displayDisablingEffect();
		}
		else if (deltaTime <= cooldown) {
			executor.entity().sendMessage("      on cooldown rn");
			((SwordPlayer) executor).displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
		}
		else {
			setTimeLastExecuted();
			executor.entity().sendMessage("      Runnin ability");
			action.accept(executor);
		}
	}
	
	public long calcCooldown(Combatant executor) {
		return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
	}
	
	public boolean cannotPerform(Combatant executor) {
		return cannotPerform == null || cannotPerform.test(executor);
	}
	
	public long getTimeLastExecuted() {
		return timeLastExecuted;
	}
	
	public void setTimeLastExecuted() {
		timeLastExecuted = System.currentTimeMillis();
	}
}
