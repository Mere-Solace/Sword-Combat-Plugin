package btm.sword.system.statemachine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

// TODO: Use some of this logic in the new UmbralStateMachine class
// TODO: Make Umbral State machine have globally defined transitions
public class StateMachine<T> {
    private final T context;
    private State<T> currentState;
    private final Map<Transition<T>, Predicate<T>> transitions = new HashMap<>();

    public StateMachine(T context, State<T> initialState) {
        this.context = context;
        this.currentState = initialState;
        currentState.onEnter(context);
    }

    public void tick() {
        currentState.onTick(context);

        // Then global transitions
        for (var entry : transitions.entrySet()) {
            var transition = entry.getKey();
            if (transition.from().equals(currentState) && transition.condition().test(context)) {
                transition.onTransition().accept(context);
                setState(transition.to());
                return;
            }
        }
    }

    // Keep your API simple:
    public void addTransition(Transition<T> transition) {
        transitions.put(transition, transition.condition());
    }

    public boolean inState(State<T> check) {
        return check.getClass().equals(currentState.getClass());
    }

    private void setState(State<T> next) {
        currentState.onExit(context);
        currentState = next;
        currentState.onEnter(context);
    }

    public State<T> getState() {
        return currentState;
    }
}
