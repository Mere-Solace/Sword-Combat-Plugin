package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class WieldState extends UmbralStateFacade {
    @Override
    public String name() { return "WIELD"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.getDisplay().setViewRange(0);
        blade.getThrower().setItemInInventory(0, blade.getBlade());
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getDisplay().setViewRange(300);
        blade.getThrower().setItemInInventory(0, blade.getLink());
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // TODO some cool functionality for while you wield the blade
        //
    }
}
