package btm.sword.utility.statemachine;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic finite state machine (FSM).
 *
 * <p>Each tick the machine calls {@link State#onTick} on the current state, then evaluates
 * registered {@link Transition}s in insertion order. The first transition whose {@code from}
 * state matches the current state and whose condition is satisfied fires: the machine calls
 * {@link #onAnyTransition()}, executes the transition's action, advances to the target state,
 * then calls {@link #afterAnyTransition()}.</p>
 *
 * <p>Extend this class to add domain-specific hooks around transitions (see
 * {@link btm.sword.system.entity.umbral.statemachine.UmbralStateMachine} for an example).</p>
 *
 * @param <T> the context type passed to every {@link State} callback
 */
public class StateMachine<T> {

    /** The shared context object passed to all state callbacks. */
    protected final T context;

    /** The currently active state. */
    protected State<T> currentState;

    /** Ordered list of transitions; evaluated top-to-bottom each tick. */
    protected final List<Transition<T>> transitions = new ArrayList<>();

    /**
     * Per-state-class cache of the filtered, ordered subset of {@link #transitions} that apply
     * to that state. Computed lazily on first encounter; avoids the O(N) full scan and
     * {@link Class#isAssignableFrom} calls every tick.
     */
    private final Map<Class<?>, List<Transition<T>>> effectiveTransitionCache = new HashMap<>();

    /**
     * Creates the state machine with the given context and enters the initial state immediately.
     *
     * @param context      the shared context object
     * @param initialState the state to enter first
     */
    public StateMachine(T context, State<T> initialState) {
        this.context = context;
        this.currentState = initialState;
        currentState.onEnter(context);
    }

    /**
     * Hook called immediately before a transition's action is executed.
     * Override for cross-cutting pre-transition logic.
     */
    public void onAnyTransition() {}

    /**
     * Hook called immediately after the new state has been entered following a transition.
     * Override for cross-cutting post-transition logic.
     */
    public void afterAnyTransition() {}

    /**
     * Ticks the current state and evaluates only the transitions applicable to the current state.
     * <p>
     * The applicable subset is computed lazily on first encounter via
     * {@link #effectiveTransitionsFor(Class)} and cached for subsequent ticks,
     * eliminating the O(N) full scan and {@link Class#isAssignableFrom} calls every tick.
     */
    public void tick() {
        currentState.onTick(context);
        for (var t : effectiveTransitionsFor(currentState.getClass())) {
            if (t.condition().test(context)) {
                onAnyTransition();
                t.onTransition().accept(context);
                setState(createState(t.to()));
                afterAnyTransition();
                return;
            }
        }
    }

    /**
     * Returns the cached subset of {@link #transitions} whose {@code from} class is assignable
     * from {@code stateClass}. Built once per unique state class and reused on every subsequent tick.
     *
     * @param stateClass the concrete state class currently active
     * @return ordered list of transitions applicable to that state
     */
    private List<Transition<T>> effectiveTransitionsFor(Class<?> stateClass) {
        return effectiveTransitionCache.computeIfAbsent(stateClass,
            cls -> transitions.stream()
                .filter(t -> t.from().isAssignableFrom(cls))
                .collect(Collectors.toList()));
    }

    /**
     * Instantiates a state by its class via reflection.
     * Override to supply states with constructor arguments.
     *
     * @param clazz the state class to instantiate
     * @return a new instance of the given state
     */
    protected State<T> createState(Class<? extends State<T>> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers a transition to be evaluated each tick.
     * Transitions are evaluated in the order they are added.
     *
     * @param transition the transition to register
     */
    public void addTransition(Transition<T> transition) {
        transitions.add(transition);
        effectiveTransitionCache.clear();
    }

    /**
     * Returns {@code true} if the machine is currently in the same state class as {@code check}.
     *
     * @param check the state instance whose class to compare against
     * @return {@code true} if the current state is an instance of {@code check}'s class
     */
    public boolean inState(State<T> check) {
        return check.getClass().equals(currentState.getClass());
    }

    /**
     * Immediately transitions to {@code next}, calling exit/enter hooks on both states.
     * Does not evaluate transition conditions — use for forced state changes.
     *
     * @param next the state to transition to
     */
    public void setState(State<T> next) {
        currentState.onExit(context);
        currentState = next;
        currentState.onEnter(context);
    }

    /**
     * Returns the currently active state.
     *
     * @return the current state
     */
    public State<T> getState() {
        return currentState;
    }
}
