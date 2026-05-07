package btm.sword.umbral.statemachine.state;

import org.bukkit.util.Vector;

import btm.sword.action.throwing.InteractiveItemArbiter;
import btm.sword.config.Config;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.motion.drivers.AnchoredFollowDriver;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import net.kyori.adventure.text.Component;

/** State where the blade hovers near the wielder in a ready position, following their movement. */
public class StandbyState extends UmbralStateFacade {

    private static final Vector STANDBY_LOCAL_OFFSET = new Vector(0.7, 0.7, -0.5);
    private static final int STANDBY_TELEPORT_DURATION = 5;

    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        // Instead of putting whenever blade enters a diff state, just deactivate when it should be
        InteractiveItemArbiter.remove(blade.getDisplay(), false);

        blade.getBladeMotion().install(new AnchoredFollowDriver(
            STANDBY_LOCAL_OFFSET, false, STANDBY_TELEPORT_DURATION
        ));
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
        blade.getBladeMotion().stop();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Position tracking handled by AnchoredFollowDriver via BladeMotion; bobbing handled by idle movement task.
    }
}
