package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.utility.display.DisplayUtil;
import net.kyori.adventure.text.Component;

/** State where the blade hovers near the wielder in a ready position, following their movement. */
public class StandbyState extends UmbralStateFacade {
    private TimeArbiter.TaskHandle followTask;

    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        // Instead of putting whenever blade enters a diff state, just deactivate when it should be
        InteractiveItemArbiter.remove(blade.getDisplay(), false);

        followTask = DisplayUtil.itemDisplayFollowLerp(
            blade.getThrower(), blade.getDisplay(),
            new Vector(0.7, 0.7, -0.5),
            5, 150, false
        );
        blade.startIdleMovement();
        if (blade.getThrower() instanceof SwordPlayer swordPlayer) {
            swordPlayer.displayTitle(null, Component.text("Ready.").color(Config.SwordColor.TEXT_COOL),
                50, 500, 50);
        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        InteractiveItemArbiter.put(blade);

        blade.endIdleMovement();
        if (followTask != null)
            followTask.cancel();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Idle movement handled by a TaskHandle
    }
}
