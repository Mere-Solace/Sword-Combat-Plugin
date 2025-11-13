package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.statemachine.State;

public class StandbyState extends State<UmbralBlade> {
    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
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
