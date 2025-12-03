package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;


public class SheathedState extends UmbralStateFacade {
    @Override
    public String name() { return "SHEATHED"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        // Instead of putting whenever blade enters a diff state, just deactivate when it should be
        InteractiveItemArbiter.remove(blade.getDisplay(), false);

        blade.endIdleMovement();
        blade.getThrower().setItemInInventory(0, blade.getLink());
    }

    @Override
    public void onExit(UmbralBlade blade) {
        InteractiveItemArbiter.put(blade);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        blade.updateSheathedPosition();
    }
}
