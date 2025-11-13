package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class SheathedState extends UmbralStateFacade {
    @Override
    public String name() { return "SHEATHED"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.getThrower().setItemStackInHand(blade.getLink(), true);
    }

    @Override
    public void onExit(UmbralBlade blade) {

    }

    @Override
    public void onTick(UmbralBlade blade) {
        blade.updateSheathedPosition();
    }
}
