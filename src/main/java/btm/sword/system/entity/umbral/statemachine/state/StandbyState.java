package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.scheduler.BukkitTask;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

public class StandbyState extends UmbralStateFacade {
    private BukkitTask followTask;

    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        followTask = blade.hoverBehindWielder();
        blade.startIdleMovement();
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.endIdleMovement();
        if (followTask != null && followTask.getTaskId() != -1 && !followTask.isCancelled())
            followTask.cancel();
    }

    // TODO: move idle movement into this onTick method!
    @Override
    public void onTick(UmbralBlade blade) {
        // Idle movement handled by BukkitRunnable; tick may monitor attack triggers
    }
}
