package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.statemachine.State;

// TODO: In each of these State class implementations, make them extend StateNode instead
// TODO: Remove the generic implementations once no dependencies on them.
public class StandbyState extends State<UmbralBlade> {
    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setDisplayTransformation(UmbralState.STANDBY);
        blade.hoverBehindWielder();
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.endIdleMovement();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Idle movement handled by BukkitRunnable; tick may monitor attack triggers
    }
}
