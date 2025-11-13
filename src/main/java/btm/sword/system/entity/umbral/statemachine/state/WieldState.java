package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.statemachine.State;

public class WieldState extends State<UmbralBlade> {
    @Override
    public String name() { return "WIELD"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.getDisplay().setViewRange(0);
        blade.getThrower().setItemStackInHand(blade.getBlade(), true);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getDisplay().setViewRange(300);
        blade.getThrower().setItemStackInHand(blade.getLink(), true);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // TODO some cool functionality for while you wield the blade
        //
    }
}
