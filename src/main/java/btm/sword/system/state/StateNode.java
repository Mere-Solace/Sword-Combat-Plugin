package btm.sword.system.state;

import java.util.function.Consumer;

/**
 * Represents a node in a state machine, defining behavior for a specific state.
 * <p>
 * Each state node encapsulates the entry and exit actions that should be executed
 * when transitioning into or out of this state. This allows for clean separation
 * of state-specific logic from transition logic.
 * </p>
 *
 * @param <S> The state enum type
 * @param <C> The context type that owns the state machine
 *
 * @author Claude Code
 * @since 1.0
 */
public class StateNode<S extends Enum<S>, C> {
    /** The state enum value this node represents */
    private final S state;

    /** Action to execute when entering this state (may be null) */
    private final Consumer<C> onEnter;

    /** Action to execute when exiting this state (may be null) */
    private final Consumer<C> onExit;

    /**
     * Constructs a new state node with entry and exit actions.
     *
     * @param state The state enum value this node represents
     * @param onEnter Action to execute on state entry (may be null)
     * @param onExit Action to execute on state exit (may be null)
     */
    public StateNode(S state, Consumer<C> onEnter, Consumer<C> onExit) {
        this.state = state;
        this.onEnter = onEnter;
        this.onExit = onExit;
    }

    /**
     * Constructs a new state node with only an entry action.
     *
     * @param state The state enum value this node represents
     * @param onEnter Action to execute on state entry (may be null)
     */
    public StateNode(S state, Consumer<C> onEnter) {
        this(state, onEnter, null);
    }

    /**
     * Constructs a new state node with no actions.
     * <p>
     * Useful for states that don't require specific entry/exit behavior.
     * </p>
     *
     * @param state The state enum value this node represents
     */
    public StateNode(S state) {
        this(state, null, null);
    }

    /**
     * Executes the entry action for this state.
     * <p>
     * This is called by the state machine when transitioning INTO this state.
     * </p>
     *
     * @param context The context object owning the state machine
     */
    public void onEnter(C context) {
        if (onEnter != null) {
            onEnter.accept(context);
        }
    }

    /**
     * Executes the exit action for this state.
     * <p>
     * This is called by the state machine when transitioning OUT OF this state.
     * </p>
     *
     * @param context The context object owning the state machine
     */
    public void onExit(C context) {
        if (onExit != null) {
            onExit.accept(context);
        }
    }

    /**
     * Returns the state enum value this node represents.
     *
     * @return The state enum value
     */
    public S getState() {
        return state;
    }

    /**
     * Returns a string representation of this state node.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("StateNode[%s]", state.name());
    }
}
