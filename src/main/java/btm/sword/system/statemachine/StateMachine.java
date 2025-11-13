package btm.sword.system.statemachine;

public class StateMachine<T> {
    private final T context;
    private State<T> currentState;

    public StateMachine(T context, State<T> initialState) {
        this.context = context;
        this.currentState = initialState;
        currentState.onEnter(context);
    }

    public void tick() {
        currentState.onTick(context);
        for (var entry : currentState.transitions().entrySet()) {
            if (entry.getValue().test(context)) {
                switchState(entry.getKey());
                break;
            }
        }
    }

    public void switchState(Class<? extends State<T>> nextClass) {
        try {
            currentState.onExit(context);
            currentState = nextClass.getDeclaredConstructor().newInstance();
            currentState.onEnter(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to switch state", e);
        }
    }

    public State<T> getState() {
        return currentState;
    }
}
