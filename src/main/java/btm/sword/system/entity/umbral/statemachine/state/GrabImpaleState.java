package btm.sword.system.entity.umbral.statemachine.state;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.utility.display.DisplayUtil;

/** State that lunges the blade toward a grabbed target and impales it on contact. */
public class GrabImpaleState extends UmbralStateFacade {
    private TimeArbiter.TaskHandle slerpTask;
    private ScheduledFuture<?> attackTask;
    private boolean lungeActive = false;

    @Override
    public String name() {
        return "GRAB_IMPALE";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setTimeStep(0);
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);

        moveToReadyPosition(blade);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        lungeActive = false;
        blade.setFinishedLunging(false);
        if (slerpTask != null && !slerpTask.isCancelled()) slerpTask.cancel();
        if (attackTask != null) attackTask.cancel(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        if (!lungeActive) return;
        if (blade.stepFlight()) {
            blade.onEnd();
            lungeActive = false;
        }
    }

    private void moveToReadyPosition(UmbralBlade blade) {
        SwordEntity grabbed = blade.getThrower().getGrabbedEntity();
        // TODO: potentially -> make random and in cooler more dynamic positions depending on cur blade pos
        Vector offset = new Vector(-1, grabbed.getEyeHeight() * 6, -1);
        slerpTask = DisplayUtil.displaySlerpToOffset(grabbed, blade.getDisplay(), offset,
            1, 2, 50, 2, false,
            50,
            () -> { // predicate for when the movement should end other than when it reaches destination.
                Combatant thrower = blade.getThrower();
                return thrower.getGrabbedEntity() == null ||
                    thrower.getGrabbedEntity().isDead() ||
                    thrower.isDead() ||
                    !thrower.getUmbralBlade().inState(GrabImpaleState.class);
            },
            () -> false,
            () -> attackTask = SwordScheduler.runBukkitTaskLater(() -> attackEnemy(blade),
                200, TimeUnit.MILLISECONDS)
        );
    }

    private void attackEnemy(UmbralBlade blade) {
        if (!blade.inState(GrabImpaleState.class)) return;
        if (slerpTask != null && !slerpTask.isCancelled()) slerpTask.cancel();
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);
        blade.setTimeCutoff(Config.UmbralBlade.LUNGE_TIME_CUTOFF);
        blade.setTimeScalingFactor(Config.UmbralBlade.LUNGE_TIME_SCALING_FACTOR);
        blade.setCtrlPointsForLunge(AttackType.LUNGE1.controlVectors());
        blade.initFlight(Config.UmbralBlade.LUNGE_ON_RELEASE_VELOCITY);
        lungeActive = true;
    }
}
