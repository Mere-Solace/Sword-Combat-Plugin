package btm.sword.system.statemachine;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class StateMachine<T> {
    protected final T context;
    protected State<T> currentState;
    protected final Map<Transition<T>, Predicate<T>> transitions = new HashMap<>();

    public StateMachine(T context, State<T> initialState) {
        this.context = context;
        this.currentState = initialState;
        currentState.onEnter(context);
    }

    public void tick() {
        currentState.onTick(context);
        for (var t : transitions.keySet()) {
            if (t.from().isAssignableFrom(currentState.getClass())
                && t.condition().test(context)) {

                t.onTransition().accept(context);
                setState(createState(t.to()));
                return;
            }
        }
    }

    protected State<T> createState(Class<? extends State<T>> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
