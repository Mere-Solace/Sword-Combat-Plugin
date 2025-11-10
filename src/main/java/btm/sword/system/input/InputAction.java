package btm.sword.system.input;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;

/**
 * Represents an executable action triggered by player input within the Sword plugin system.
 * Encapsulates the logic for execution, cooldown handling, ability casting checks,
 * and optional display of cooldown or disabled states to the player.
 * <p>
 * Uses {@link Combatant} as the executor and may display messages via {@link SwordPlayer} methods.
 * </p>
 */
public class InputAction {
    /** The action to perform when executed, defined as a consumer of a Combatant. */
    private final Consumer<Combatant> action;

    /** Function to calculate the cooldown duration in milliseconds for this action, based on the executor. */
    private final Function<Combatant, Long> cooldownCalculation;

    /** Predicate that tests whether the executor is allowed to cast this ability at a given time. */
    private final Predicate<Combatant> canCastAbility;

    /** Whether to display the remaining cooldown time to the player if action is on cooldown. */
    private final boolean displayCooldown;

    /** Whether to display a "disabled" effect/message if the action cannot be cast. */
    private final boolean displayDisabled;

    /** Timestamp in milliseconds when this action was last successfully executed. */
    @lombok.Getter
    private long timeLastExecuted = 0;

    /**
     * Constructs an InputAction.
     *
     * @param action the action to execute on {@link Combatant}
     * @param cooldownCalculation function that computes cooldown based on executor; returns milliseconds
     * @param canCastAbility predicate to test if executor can cast the action
     * @param displayCooldown whether to visually show cooldown progress on failure
     * @param displayDisabled whether to visually indicate the ability is disabled on failure
     */
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

    /**
     * Attempts to execute this action by the specified {@link Combatant} executor.
     * Will check cooldown and ability readiness before performing the action.
     * Displays cooldown or disabled visual indicators to the player as configured.
     *
     * @param executor the Combatant attempting to execute the action
     * @return true if action was executed, false otherwise
     */
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

    /**
     * Calculates the cooldown duration in milliseconds for this action,
     * defaulting to zero if no cooldown function is set.
     *
     * @param executor the Combatant executing the action
     * @return the cooldown duration in milliseconds
     */
    public long calcCooldown(Combatant executor) {
        return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
    }

    /**
     * Tests whether the executor meets the conditions to cast this ability.
     * Defaults to always true if no predicate is provided.
     *
     * @param executor the Combatant attempting to cast
     * @return true if casting is allowed, false otherwise
     */
    public boolean canCast(Combatant executor) {
        return canCastAbility == null || canCastAbility.test(executor);
    }

    /**
     * Sets the timestamp when this action was last executed to the current time.
     */
    public void setTimeLastExecuted() {
        timeLastExecuted = System.currentTimeMillis();
    }
}
