package btm.sword.system.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Generic finite state machine implementation that manages state transitions and lifecycle.
 * <p>
 * This class provides a type-safe, extensible framework for implementing state machines
 * with validation, history tracking, and event callbacks. It enforces transition rules,
 * executes state entry/exit actions, and maintains a history of state changes for debugging.
 * </p>
 *
 * @param <S> The state enum type (e.g., UmbralState)
 * @param <C> The context type that owns this state machine (e.g., UmbralBlade)
 *
 * @author Claude Code
 * @since 1.0
 */
public class StateMachine<S extends Enum<S>, C> {
    /** The context object that owns this state machine */
    protected final C context;

    /** Current active state */
    protected S currentState;

    /** Map of state enum values to their corresponding state nodes */
    protected final Map<S, StateNode<S, C>> stateNodes;

    /** List of all defined state transitions */
    protected final List<StateTransition<S, C>> transitions;

    /** History of state changes for debugging (most recent first) */
    protected final List<StateChange<S>> history;

    /** Maximum number of state changes to keep in history */
    protected final int maxHistorySize;

    /** Optional callback invoked after any state transition completes */
    protected Consumer<StateChange<S>> onTransitionComplete;

    /**
     * Constructs a new state machine with the given context and initial state.
     *
     * @param context The context object that owns this state machine
     * @param initialState The initial state to start in
     * @param maxHistorySize Maximum number of state changes to keep in history
     */
    public StateMachine(C context, S initialState, int maxHistorySize) {
        this.context = context;
        this.currentState = initialState;
        this.stateNodes = new HashMap<>();
        this.transitions = new ArrayList<>();
        this.history = new ArrayList<>();
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Constructs a new state machine with default history size of 50.
     *
     * @param context The context object that owns this state machine
     * @param initialState The initial state to start in
     */
    public StateMachine(C context, S initialState) {
        this(context, initialState, 50);
    }

    /**
     * Registers a state node for a given state enum value.
     * <p>
     * State nodes define the behavior (entry/exit actions) for each state.
     * This method should be called during state machine initialization for all valid states.
     * </p>
     *
     * @param state The state enum value
     * @param node The state node defining behavior for this state
     * @return This state machine for method chaining
     */
    public StateMachine<S, C> registerState(S state, StateNode<S, C> node) {
        stateNodes.put(state, node);
        return this;
    }

    /**
     * Registers a state transition between two states.
     * <p>
     * Transitions define the allowed paths between states and any associated
     * guard conditions or transition actions.
     * </p>
     *
     * @param transition The transition to register
     * @return This state machine for method chaining
     */
    public StateMachine<S, C> registerTransition(StateTransition<S, C> transition) {
        transitions.add(transition);
        return this;
    }

    /**
     * Sets a callback to be invoked after any state transition completes.
     *
     * @param callback The callback consumer accepting a StateChange object
     * @return This state machine for method chaining
     */
    public StateMachine<S, C> onTransitionComplete(Consumer<StateChange<S>> callback) {
        this.onTransitionComplete = callback;
        return this;
    }

    /**
     * Attempts to transition to a new state.
     * <p>
     * The transition will only succeed if:
     * <ol>
     *   <li>A valid transition exists from current state to target state</li>
     *   <li>The transition's guard condition (if any) evaluates to true</li>
     *   <li>The current state is different from the target state</li>
     * </ol>
     * </p>
     * <p>
     * On success, the following occurs in order:
     * <ol>
     *   <li>Current state's {@link StateNode#onExit(Object)} is called</li>
     *   <li>Transition's action (if any) is executed</li>
     *   <li>Target state's {@link StateNode#onEnter(Object)} is called</li>
     *   <li>Current state is updated</li>
     *   <li>Transition is recorded in history</li>
     *   <li>Transition complete callback (if set) is invoked</li>
     * </ol>
     * </p>
     *
     * @param newState The target state to transition to
     * @return true if the transition succeeded, false otherwise
     */
    public boolean transitionTo(S newState) {
        // Don't transition to the same state
        if (currentState == newState) {
            return false;
        }

        // Find a valid transition
        Optional<StateTransition<S, C>> validTransition = transitions.stream()
                .filter(t -> t.getFrom() == currentState && t.getTo() == newState)
                .filter(t -> t.canTransition(context))
                .findFirst();

        if (validTransition.isEmpty()) {
            // No valid transition found
            return false;
        }

        StateTransition<S, C> transition = validTransition.get();
        S previousState = currentState;

        // Execute exit action on current state
        StateNode<S, C> currentNode = stateNodes.get(currentState);
        if (currentNode != null) {
            currentNode.onExit(context);
        }

        // Execute transition action
        transition.executeTransition(context);

        // Execute entry action on new state
        StateNode<S, C> newNode = stateNodes.get(newState);
        if (newNode != null) {
            newNode.onEnter(context);
        }

        // Update current state
        currentState = newState;

        // Record transition in history
        StateChange<S> change = new StateChange<>(previousState, newState, System.currentTimeMillis());
        history.add(0, change); // Add to front of list
        if (history.size() > maxHistorySize) {
            history.remove(history.size() - 1); // Remove oldest
        }

        // Invoke callback if set
        if (onTransitionComplete != null) {
            onTransitionComplete.accept(change);
        }

        return true;
    }

    /**
     * Forces a transition to a new state without validation.
     * <p>
     * <b>WARNING:</b> This bypasses all transition guards and validation.
     * Use only for initialization or emergency state recovery.
     * </p>
     *
     * @param newState The state to force transition to
     */
    public void forceTransition(S newState) {
        if (currentState == newState) {
            return;
        }

        S previousState = currentState;

        // Execute exit action on current state
        StateNode<S, C> currentNode = stateNodes.get(currentState);
        if (currentNode != null) {
            currentNode.onExit(context);
        }

        // Execute entry action on new state
        StateNode<S, C> newNode = stateNodes.get(newState);
        if (newNode != null) {
            newNode.onEnter(context);
        }

        // Update current state
        currentState = newState;

        // Record forced transition in history
        history.add(0, new StateChange<>(previousState, newState, System.currentTimeMillis()));
        if (history.size() > maxHistorySize) {
            history.remove(history.size() - 1);
        }
    }

    /**
     * Checks if a transition from current state to target state is valid.
     * <p>
     * A transition is valid if:
     * <ol>
     *   <li>A registered transition exists for the path</li>
     *   <li>The transition's guard condition evaluates to true</li>
     * </ol>
     * </p>
     *
     * @param targetState The target state to check
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(S targetState) {
        return transitions.stream()
                .anyMatch(t -> t.getFrom() == currentState
                        && t.getTo() == targetState
                        && t.canTransition(context));
    }

    /**
     * Returns the current active state.
     *
     * @return The current state enum value
     */
    public S getCurrentState() {
        return currentState;
    }

    /**
     * Checks if the state machine is currently in the given state.
     *
     * @param state The state to check
     * @return true if current state matches, false otherwise
     */
    public boolean isInState(S state) {
        return currentState == state;
    }

    /**
     * Returns an unmodifiable view of the transition history.
     * <p>
     * The list is ordered with most recent transitions first.
     * </p>
     *
     * @return List of state changes
     */
    public List<StateChange<S>> getHistory() {
        return List.copyOf(history);
    }

    /**
     * Returns the most recent state change, if any.
     *
     * @return Optional containing the most recent state change, or empty if no history exists
     */
    public Optional<StateChange<S>> getLastTransition() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(0));
    }

    /**
     * Clears the transition history.
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Returns the context object that owns this state machine.
     *
     * @return The context object
     */
    public C getContext() {
        return context;
    }

    /**
     * Records a state transition event for history and debugging.
     *
     * @param <S> The state enum type
     */
    public record StateChange<S extends Enum<S>>(S from, S to, long timestamp) {
        /**
         * Returns a human-readable representation of this state change.
         *
         * @return String representation
         */
        @Override
        public String toString() {
            return String.format("%s -> %s (at %d)", from.name(), to.name(), timestamp);
        }
    }
}
