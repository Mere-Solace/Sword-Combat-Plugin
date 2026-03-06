package btm.sword.system.input;

import btm.sword.system.action.ActionCaster;
import btm.sword.system.action.constraint.ActionConstraint;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import net.kyori.adventure.text.format.TextColor;

/**
 * Helper that centralizes execution / readiness checks for InputAction. Makes the
 * behavior easier to test and allows future extension (displaying, telemetry, etc.).
 */
public final class InputActionExecutor {
    private InputActionExecutor() {}

    /**
     * Describes why an {@link InputAction} is or is not ready to cast.
     * <p>
     * Priority order: {@code ON_COOLDOWN} supersedes all others (per HUD coloring design);
     * {@code DISABLED} supersedes soulfire; {@code INSUFFICIENT_SOULFIRE} is lowest non-ready state.
     * </p>
     */
    public enum ReadinessState {
        /** All conditions pass; the action can fire immediately. */
        READY,
        /** Cooldown has not yet expired. Supersedes all other non-ready states. */
        ON_COOLDOWN,
        /** The executor lacks sufficient soulfire. */
        INSUFFICIENT_SOULFIRE,
        /** A constraint or canCast predicate is failing. */
        DISABLED
    }

    /**
     * Evaluates the full readiness of the given action for the executor, returning the
     * highest-priority reason it cannot fire (or {@link ReadinessState#READY} if all pass).
     * <p>
     * Evaluation order: cooldown → constraints → canCast predicate → soulfire.
     * Cooldowns supersede all other states per HUD display design.
     * </p>
     *
     * @param action the action to evaluate
     * @param executor the combatant attempting to cast
     * @return the readiness state
     */
    public static ReadinessState readiness(InputAction action, Combatant executor) {
        if (action == null) return ReadinessState.DISABLED;

        long elapsed = System.currentTimeMillis() - action.getTimeLastExecuted();
        if (elapsed <= action.calcCooldown(executor)) return ReadinessState.ON_COOLDOWN;

        for (ActionConstraint constraint : action.getConstraints()) {
            if (!constraint.test(executor)) return ReadinessState.DISABLED;
        }

        if (action.unableToCast(executor)) return ReadinessState.DISABLED;

        if (executor instanceof SwordPlayer sp &&
                sp.getAspects().soulfireCur() < action.getRequiredSoulfire(executor)) {
            return ReadinessState.INSUFFICIENT_SOULFIRE;
        }

        return ReadinessState.READY;
    }

    /**
     * Returns whether the executor currently meets all conditions to cast the given action.
     * Delegates to {@link #readiness(InputAction, Combatant)} — now includes cooldown in the check.
     *
     * @param action the action to check
     * @param executor the combatant attempting to cast
     * @return true if all conditions pass, false if any fail
     */
    public static boolean canCast(InputAction action, Combatant executor) {
        if (action == null) return false;
        return readiness(action, executor) == ReadinessState.READY;
    }

    /**
     * Returns a {@link TextColor} representing soulfire readiness. Used by the HUD and
     * post-cast subtitle display. Interpolates from dark blue-gray {@code (20, 20, 60)} at
     * 0% to bright blue {@code (80, 80, 220)} at 100%.
     *
     * @param pct fraction of soulfire available relative to the required cost (0.0–1.0, clamped)
     * @return interpolated TextColor
     */
    public static TextColor soulfireStatusColor(float pct) {
//        TextColor.color(40,40,40);
//        TextColor.color(150,150,150);
        int r0 = 0;
        int g0 = 0;
        int b0 = 0;
        int r1 = 140;
        int g1 = 140;
        int b1 = 140;
        int r_diff = r1-r0;
        int g_diff = g1-g0;
        int b_diff = b1-b0;
        float clamped = Math.max(0f, Math.min(1f, pct));
        int r = (int) (r0 + clamped * (r_diff));
        int g = (int) (g0 + clamped * (g_diff));
        int b = (int) (b0 + clamped * (b_diff));
        return TextColor.color(r, g, b);
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
