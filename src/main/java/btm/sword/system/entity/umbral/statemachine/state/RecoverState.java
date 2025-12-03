package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

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
            recoverBlade, null, 0, 4);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        if (recoverTask != null && !recoverTask.isCancelled()) recoverTask.cancel();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
