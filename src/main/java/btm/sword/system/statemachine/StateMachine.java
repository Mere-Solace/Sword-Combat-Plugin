package btm.sword.system.statemachine;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

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

        for (var entry : transitions.entrySet()) {
            var transition = entry.getKey();

            if (transition.from().getClass().equals(currentState.getClass()) && transition.condition().test(context)) {
                transition.onTransition().accept(context);
                setState(transition.to());
                return;
            }
        }
    }

    public void addTransition(Transition<T> transition) {
        transitions.put(transition, transition.condition());
    }

    public boolean inState(State<T> check) {
        return check.getClass().equals(currentState.getClass());
    }

    public void setState(State<T> next) {
        currentState.onExit(context);
        currentState = next;
        currentState.onEnter(context);
    }

    public State<T> getState() {
        return currentState;
    }
}
