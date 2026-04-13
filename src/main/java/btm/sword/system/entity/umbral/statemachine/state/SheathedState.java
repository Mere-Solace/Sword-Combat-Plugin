package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;


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
