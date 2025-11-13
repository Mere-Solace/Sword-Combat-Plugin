package btm.sword.system.entity.umbral.statemachine;

import java.util.List;
import java.util.Map;

public class UmbralStateMachine {
    private UmbralState currentState;
    private final Map<UmbralState, StateNode> stateNodes;
    private final List<StateTransition> transitions;

    public UmbralStateMachine(Map<UmbralState, StateNode> stateNodes, List<StateTransition> transitions) {
        this.stateNodes = stateNodes;
        this.transitions = transitions;
    }

    public boolean transitionTo(UmbralState newState) {
        // Validate transition is allowed
        // Execute exit actions on current state
        // Execute transition actions
        // Execute entry actions on new state
    }
}
