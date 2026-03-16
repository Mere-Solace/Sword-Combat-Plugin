package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class InactiveState extends UmbralStateFacade {
    @Override
    public String name() {
        return "INACTIVE";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        if (blade.getDisplay() != null) {
            blade.getDisplay().setViewRange(0f);
        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        if (blade.getDisplay() != null) {
            blade.getDisplay().setViewRange(300f);
        }
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
