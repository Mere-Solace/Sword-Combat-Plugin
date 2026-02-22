package btm.sword.system.input;

import btm.sword.system.action.ActionCaster;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Helper that centralizes execution / readiness checks for InputAction. Makes the
 * behavior easier to test and allows future extension (displaying, telemetry, etc.).
 */
public final class InputActionExecutor {
    private InputActionExecutor() {}

    public static boolean canCast(InputAction action, Combatant executor) {
        if (action == null) return false;
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

        // schedule or perform the action using the centralized ActionCaster
        ActionCaster.cast(executor, action.getCastDurationMillis(executor), () -> action.perform(executor));
    }
}
