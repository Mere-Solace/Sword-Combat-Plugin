package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.statemachine.State;

public class SheathedState extends State<UmbralBlade> {
    @Override
    public String name() { return "SHEATHED"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setDisplayTransformation(UmbralState.SHEATHED);
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
