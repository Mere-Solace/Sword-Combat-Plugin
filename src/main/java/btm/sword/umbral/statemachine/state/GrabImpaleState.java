package btm.sword.umbral.statemachine.state;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.util.Vector;

import btm.sword.combat.style.AttackType;
import btm.sword.config.Config;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.motion.drivers.SlerpToOffsetDriver;
import btm.sword.umbral.statemachine.UmbralStateFacade;

/**
 * State that lunges the blade toward a grabbed target and impales it on contact.
 * <p>
 * Two phases:
 * <ol>
 *   <li><b>Slerp</b> — the blade approaches a ready position above the grabbed entity using a
 *       {@link SlerpToOffsetDriver} on the blade's motion subsystem. On arrival the
 *       driver schedules a 200 ms delay before transitioning to the lunge phase.</li>
 *   <li><b>Lunge</b> — the blade flies through the grabbed entity along an inherited cubic-Bézier
 *       trajectory driven by the parent {@code stepFlight} loop. The driver-based migration of
 *       this phase happens together with {@link LungingState} so the two paths share the same
 *       {@code BezierDriver} integration.</li>
 * </ol>
 */
public class GrabImpaleState extends UmbralStateFacade {

    private static final double SLERP_SPEED = 1.0;
    private static final double SLERP_ARRIVAL_DISTANCE = 2.0;
    private static final long SLERP_TIMEOUT_TICKS = 50;
    private static final int SLERP_TELEPORT_DURATION = 2;
    private static final int LUNGE_DELAY_MS = 200;
    private static final double EYE_HEIGHT_MULTIPLIER = 6.0;

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
        blade.getBladeMotion().stop();
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
        Vector offset = new Vector(-1, grabbed.getEyeHeight() * EYE_HEIGHT_MULTIPLIER, -1);

        // Composed end predicate: abandon the slerp if the grab context evaporates (target lost,
        // thrower dead, or FSM transitioned away from this state). The driver remains generic;
        // FSM-aware logic stays at the call site.
        Supplier<Boolean> abandonIfGrabLost = () -> {
            Combatant thrower = blade.getThrower();
            return thrower.getGrabbedEntity() == null
                || thrower.getGrabbedEntity().isDead()
                || thrower.isDead()
                || !thrower.getUmbralBlade().inState(GrabImpaleState.class);
        };

        blade.getBladeMotion().install(new SlerpToOffsetDriver(
            grabbed,
            offset,
            SLERP_SPEED,
            SLERP_ARRIVAL_DISTANCE,
            SLERP_TIMEOUT_TICKS,
            abandonIfGrabLost,
            () -> attackTask = SwordScheduler.runBukkitTaskLater(
                () -> attackEnemy(blade),
                LUNGE_DELAY_MS, TimeUnit.MILLISECONDS
            ),
            SLERP_TELEPORT_DURATION
        ));
    }

    private void attackEnemy(UmbralBlade blade) {
        if (!blade.inState(GrabImpaleState.class)) return;
        blade.getBladeMotion().stop();
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);
        blade.setTimeCutoff(Config.UmbralBlade.LUNGE_TIME_CUTOFF);
        blade.setTimeScalingFactor(Config.UmbralBlade.LUNGE_TIME_SCALING_FACTOR);
        blade.setCtrlPointsForLunge(AttackType.LUNGE1.controlVectors());
        blade.initFlight(Config.UmbralBlade.LUNGE_ON_RELEASE_VELOCITY);
        lungeActive = true;
    }
}
