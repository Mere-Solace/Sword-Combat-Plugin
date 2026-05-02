package btm.sword.action.constraint;

import java.util.function.Predicate;

import btm.sword.entity.base.Combatant;

/**
 * A composable predicate evaluated before an {@link btm.sword.input.InputAction} may be cast.
 * <p>
 * Constraints replace scattered {@code canPerform*()} calls in {@link Combatant} with
 * reusable, named conditions that can be declared inline in the input tree builder:
 * <pre>{@code
 * InputAction.builder()
 *     .action(executor -> AttackAction.basicAttack(executor, 1))
 *     .constraints(CommonConstraints.CAN_ACT, CommonConstraints.HOLDING_ITEM)
 *     .build()
 * }</pre>
 * </p>
 */
@FunctionalInterface
public interface ActionConstraint extends Predicate<Combatant> {

    /**
     * Returns a human-readable explanation of why this constraint was not met.
     * Used for debug messaging and future UI feedback.
     *
     * @return failure reason string
     */
    default String failureReason() {
        return "Constraint not met";
    }
}
