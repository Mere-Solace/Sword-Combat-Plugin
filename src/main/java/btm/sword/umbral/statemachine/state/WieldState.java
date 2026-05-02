package btm.sword.umbral.statemachine.state;

import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.statemachine.UmbralStateFacade;

/** State where the blade is actively held in the wielder's hand, ready to attack. */
public class WieldState extends UmbralStateFacade {
    @Override
    public String name() { return "WIELD"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setBladeWielded(true);
        blade.getDisplay().setViewRange(0);
        blade.getThrower().setItemInInventory(0, blade.getBlade());
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setBladeWielded(false);
        TimeArbiter.teleportDisplay(blade.getDisplay(), blade.getThrower().getLocation(), null, 0,
            WieldState.class, 20);
        blade.getDisplay().setViewRange(300);
        blade.getThrower().setItemInInventory(0, blade.getLink());
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // TODO some cool functionality for while you wield the blade
        //
    }
}
