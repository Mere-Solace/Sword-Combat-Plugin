package btm.sword.system.state;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a transition between two states in a state machine.
 * <p>
 * Each transition defines:
 * <ul>
 *   <li>Source state (from)</li>
 *   <li>Target state (to)</li>
 *   <li>Guard condition - determines if transition is allowed</li>
 *   <li>Transition action - executed during the transition</li>
 * </ul>
 * </p>
 * <p>
 * Guard conditions enable context-sensitive transitions. For example, an attack
 * transition might only be valid if the entity has enough stamina.
 * </p>
 *
 * @param <S> The state enum type
 * @param <C> The context type that owns the state machine
 *
 * @author Claude Code
 * @since 1.0
 */
public class StateTransition<S extends Enum<S>, C> {
    /** The source state */
    private final S from;

    /** The target state */
    private final S to;

    /** Guard condition that must be true for transition to occur (may be null for always-allowed) */
    private final Predicate<C> guard;

    /** Action to execute during the transition (may be null) */
    private final Consumer<C> action;

    /**
     * Constructs a new state transition with guard condition and action.
     *
     * @param from The source state
     * @param to The target state
     * @param guard Guard condition (may be null for always-allowed transitions)
     * @param action Action to execute during transition (may be null)
     */
    public StateTransition(S from, S to, Predicate<C> guard, Consumer<C> action) {
        this.from = from;
        this.to = to;
        this.guard = guard;
        this.action = action;
    }

    /**
     * Constructs a new state transition with only a guard condition.
     *
     * @param from The source state
     * @param to The target state
     * @param guard Guard condition (may be null for always-allowed transitions)
     */
    public StateTransition(S from, S to, Predicate<C> guard) {
        this(from, to, guard, null);
    }

    /**
     * Constructs a new state transition with no guard or action.
     * <p>
     * This transition will always be allowed if registered.
     * </p>
     *
     * @param from The source state
     * @param to The target state
     */
    public StateTransition(S from, S to) {
        this(from, to, null, null);
    }

    /**
     * Checks if this transition can occur given the current context.
     * <p>
     * If no guard is set, always returns true.
     * </p>
     *
     * @param context The context object to evaluate
     * @return true if transition is allowed, false otherwise
     */
    public boolean canTransition(C context) {
        return guard == null || guard.test(context);
    }

    /**
     * Executes the transition action, if one is defined.
     * <p>
     * This is called by the state machine during the transition, after
     * the exit action of the source state but before the entry action
     * of the target state.
     * </p>
     *
     * @param context The context object
     */
    public void executeTransition(C context) {
        if (action != null) {
            action.accept(context);
        }
    }

    /**
     * Returns the source state of this transition.
     *
     * @return The source state
     */
    public S getFrom() {
        return from;
    }

    /**
     * Returns the target state of this transition.
     *
     * @return The target state
     */
    public S getTo() {
        return to;
    }

    /**
     * Returns a string representation of this transition.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("Transition[%s -> %s]", from.name(), to.name());
    }

    /**
     * Builder class for creating state transitions with a fluent API.
     *
     * @param <S> The state enum type
     * @param <C> The context type
     */
    public static class Builder<S extends Enum<S>, C> {
        private final S from;
        private final S to;
        private Predicate<C> guard;
        private Consumer<C> action;

        /**
         * Constructs a new transition builder.
         *
         * @param from The source state
         * @param to The target state
         */
        public Builder(S from, S to) {
            this.from = from;
            this.to = to;
        }

        /**
         * Sets the guard condition for this transition.
         *
         * @param guard The guard predicate
         * @return This builder for method chaining
         */
        public Builder<S, C> guard(Predicate<C> guard) {
            this.guard = guard;
            return this;
        }

        /**
         * Sets the action to execute during this transition.
         *
         * @param action The transition action
         * @return This builder for method chaining
         */
        public Builder<S, C> action(Consumer<C> action) {
            this.action = action;
            return this;
        }

        /**
         * Builds the state transition.
         *
         * @return The constructed state transition
         */
        public StateTransition<S, C> build() {
            return new StateTransition<>(from, to, guard, action);
        }
    }

    /**
     * Creates a new transition builder.
     *
     * @param <S> The state enum type
     * @param <C> The context type
     * @param from The source state
     * @param to The target state
     * @return A new transition builder
     */
    public static <S extends Enum<S>, C> Builder<S, C> builder(S from, S to) {
        return new Builder<>(from, to);
    }
}
