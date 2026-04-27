package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * Behavioural suspend state for the UmbralBlade.
 * <p>
 * Removes the visible display entity and freezes the FSM. Does NOT dispose the
 * blade instance — that is the lifecycle layer's responsibility
 * ({@link btm.sword.system.entity.impl.Combatant#handleUmbralBladeTick()}). Once
 * the FSM is frozen here, the lifecycle layer detects the deactivated flag and
 * destroys the blade, allowing it to be respawned cleanly when conditions allow.
 * </p>
 */
public class InactiveState extends UmbralStateFacade {
    @Override
    public String name() {
        return "INACTIVE";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        if (blade.getDisplay() != null && blade.getDisplay().isValid()) {
            blade.getDisplay().remove();
        }
        blade.getBladeStateMachine().setDeactivated(true);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // FSM is frozen while in InactiveState — onExit is only invoked through dispose().
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // No-op: FSM is frozen via setDeactivated(true). Lifecycle layer handles recreation.
    }
}
