package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/** State representing a fully deactivated UmbralBlade with no display entity or active behaviour. */
public class InactiveState extends UmbralStateFacade {
    @Override
    public String name() {
        return "INACTIVE";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.dispose();
    }

    @Override
    public void onExit(UmbralBlade blade) {
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // since we turn the FSM off entirely upon entering,
        // the inactive state must run its own checks to turn the
        // system back on.

        // check if we should turn this thing back on;
        // the knowledge of which comes from one bool in Combatant
        // setting that bool will turn
    }
}
