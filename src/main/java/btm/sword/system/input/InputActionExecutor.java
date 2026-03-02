package btm.sword.system.input;

import btm.sword.system.action.ActionCaster;
import btm.sword.system.action.constraint.ActionConstraint;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Helper that centralizes execution / readiness checks for InputAction. Makes the
 * behavior easier to test and allows future extension (displaying, telemetry, etc.).
 */
public final class InputActionExecutor {
    private InputActionExecutor() {}

    /**
     * Returns whether the executor currently meets all conditions to cast the given action.
     * Checks composable {@link ActionConstraint}s, the legacy canCast predicate, and soulfire.
     * Used for UI readiness display in the input tree as well as pre-execution gating.
     *
     * @param action the action to check
     * @param executor the combatant attempting to cast
     * @return true if all conditions pass, false if any fail
     */
    public static boolean canCast(InputAction action, Combatant executor) {
        if (action == null) return false;
        for (ActionConstraint constraint : action.getConstraints()) {
            if (!constraint.test(executor)) return false;
        }
        if (action.unableToCast(executor)) return false;
        if (executor instanceof SwordPlayer sp) {
            return sp.getAspects().soulfireCur() >= action.getRequiredSoulfire(executor);
        }
        return true;
    }

    /**
     * Validates and (if valid) schedules/executes the action using {@link ActionCaster}.
     * Returns true if the action was scheduled/performed, false if it failed validation.
     */
    public static void execute(InputAction action, Combatant executor) {
        if (action == null) return;
        if (!action.handlePerformAttempt(executor)) return;
        
        executor.message("Executing an ability");
        // schedule or perform the action using the centralized ActionCaster
        ActionCaster.cast(executor, action.getCastDurationMillis(executor), () -> action.perform(executor));
    }
}
