package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/** Sentinel state that signals {@link btm.sword.system.entity.umbral.statemachine.UmbralStateMachine} to restore the previous state instead of instantiating a new one. */
public final class PreviousState extends UmbralStateFacade {
    @Override
    public String name() {
        return "PREVIOUS";
    }

    @Override
    public void onEnter(UmbralBlade context) {

    }

    @Override
    public void onExit(UmbralBlade context) {

    }

    @Override
    public void onTick(UmbralBlade context) {

    }
}
