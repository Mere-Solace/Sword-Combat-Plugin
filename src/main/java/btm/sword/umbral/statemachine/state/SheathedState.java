package btm.sword.umbral.statemachine.state;

import btm.sword.action.throwing.InteractiveItemArbiter;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.statemachine.UmbralStateFacade;


/** State representing the blade tucked away on the wielder's back with reduced visual presence. */
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
        if (blade.getDisplay() == null) return;

        TimeArbiter.teleportDisplay(
            blade.getDisplay(),
            blade.getThrower().self().getLocation(),
            blade.getThrower().getFlatDir(),
            2,
            SheathedState.class, 36
        );
        blade.getThrower().self().addPassenger(blade.getDisplay());
    }
}
