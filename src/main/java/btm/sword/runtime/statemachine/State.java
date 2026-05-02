package btm.sword.runtime.statemachine;

/**
 * Abstract base for a single state in a {@link StateMachine}.
 *
 * <p>Subclasses implement the three lifecycle hooks that the machine calls at the appropriate
 * moments, plus {@link #name()} for debug output and logging.</p>
 *
 * @param <T> the context type held by the owning {@link StateMachine}
 */
public abstract class State<T> {

    /**
     * Returns a human-readable name for this state, used in debug logs.
     *
     * @return the state's display name
     */
    public abstract String name();

    /**
     * Called once when the machine transitions <em>into</em> this state.
     *
     * @param context the shared context object
     */
    public abstract void onEnter(T context);

    /**
     * Called once when the machine transitions <em>out of</em> this state.
     *
     * @param context the shared context object
     */
    public abstract void onExit(T context);

    /**
     * Called every server tick while this state is active.
     *
     * @param context the shared context object
     */
    public abstract void onTick(T context);
}
