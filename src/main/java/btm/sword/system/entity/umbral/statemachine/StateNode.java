package btm.sword.system.entity.umbral.statemachine;

import java.util.Set;

public class StateNode {
    private final UmbralState state;
    private final Runnable onEnter;
    private final Runnable onExit;
    private final Set<UmbralState> allowedTransitions;

    public StateNode(UmbralState state, Runnable onEnter, Runnable onExit, Set<UmbralState> allowedTransitions) {
        this.state = state;
        this.onEnter = onEnter;
        this.onExit = onExit;
        this.allowedTransitions = allowedTransitions;
    }
}
