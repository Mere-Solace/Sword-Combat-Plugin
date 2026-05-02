package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.control.TimeArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/** State entered after a throw, removing the thrown display and resetting the blade's visual to the hand slot. */
public class RecoverState extends UmbralStateFacade {
    private UmbralBlade blade;
    private final Runnable recoverBlade = () -> {
        try {
            if (blade.getDisplay() != null) blade.getDisplay().remove();
            blade.setDisplay(null);
            blade.resetWeaponDisplay();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
    private TimeArbiter.TaskHandle recoverTask;

    @Override
    public String name() {
        return "RECOVERY";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        this.blade = blade;
        recoverTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            recoverBlade, null,
            0, 4,
            RecoverState.class, "onEnter"
        );
    }

    @Override
    public void onExit(UmbralBlade blade) {
        if (recoverTask != null && !recoverTask.isCancelled()) recoverTask.cancel();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
