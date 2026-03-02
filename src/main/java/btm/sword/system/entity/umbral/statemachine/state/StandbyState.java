package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import net.kyori.adventure.text.Component;

public class StandbyState extends UmbralStateFacade {
    private TimeArbiter.TaskHandle followTask;

    @Override
    public String name() { return "STANDBY"; }

    @Override
    public void onEnter(UmbralBlade blade) {
        // Instead of putting whenever blade enters a diff state, just deactivate when it should be
        InteractiveItemArbiter.remove(blade.getDisplay(), false);

        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.UMBRAL_GLOW);

        followTask = blade.hoverBehindWielder();
        blade.startIdleMovement();
        if (blade.getThrower() instanceof SwordPlayer swordPlayer) {
            swordPlayer.displayTitle(null, Component.text("Ready.").color(Config.SwordColor.TEXT_COOL),
                50, 500, 50);
        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        InteractiveItemArbiter.put(blade);

        blade.getDisplay().setGlowing(false);
        blade.endIdleMovement();
        if (followTask != null)
            followTask.cancel();
    }

    // TODO: #121 - Move idle movement into this onTick method
    @Override
    public void onTick(UmbralBlade blade) {
        // Idle movement handled by BukkitRunnable; tick may monitor attack triggers
    }
}
