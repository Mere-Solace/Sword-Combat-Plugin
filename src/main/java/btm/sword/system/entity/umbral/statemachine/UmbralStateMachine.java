package btm.sword.system.entity.umbral.statemachine;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.state.PreviousState;
import btm.sword.system.statemachine.State;
import btm.sword.system.statemachine.StateMachine;
import lombok.Getter;
import lombok.Setter;

public class UmbralStateMachine extends StateMachine<UmbralBlade> {
    @Getter
    private UmbralStateFacade previousState;
    @Setter
    private boolean deactivated;

    public UmbralStateMachine(UmbralBlade context, State<UmbralBlade> initialState) {
        super(context, initialState);
    }

    @Override
    public void tick() {
        if (deactivated) return;

        currentState.onTick(context);
        for (var t : transitions.keySet()) {
            if (t.from().isAssignableFrom(currentState.getClass())
                && t.condition().test(context)) {

                t.onTransition().accept(context);
                if (t.to() == PreviousState.class) {
                    setState(previousState);
                } else {
                    setState(createState(t.to()));
                }
                return;
            }
        }
    }

    @Override
    public void setState(State<UmbralBlade> next) {
        previousState = (UmbralStateFacade) currentState;
        context.getThrower().message("Previous State: " + previousState.name() + ", New State: " + next.name());
        super.setState(next);

        @SuppressWarnings("unchecked")
        Class<? extends State<UmbralBlade>> stateClass = (Class<? extends State<UmbralBlade>>) next.getClass();
        context.setDisplayTransformation(stateClass);
    }
}
