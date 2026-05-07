package btm.sword.umbral.statemachine.state;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.combat.style.AttackType;
import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.input.BladeRequest;
import btm.sword.umbral.motion.drivers.LungingDriver;
import btm.sword.umbral.motion.drivers.SlerpToOffsetDriver;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.misc.Debug;

/**
 * State that lunges the blade toward a grabbed target and impales it on contact.
 *
 * <p>Two phases:
 * <ol>
 *   <li><b>Slerp</b> — approach a ready position above the grabbed entity using a
 *       {@link SlerpToOffsetDriver}. On arrival schedule a 200 ms delay before transitioning to
 *       the lunge phase.</li>
 *   <li><b>Lunge</b> — fly through the grabbed entity along a cubic-Bézier trajectory using a
 *       {@link LungingDriver} with the same callback shape as
 *       {@link LungingState}.</li>
 * </ol>
 */
public class GrabImpaleState extends UmbralStateFacade {

    private static final double SLERP_SPEED = 1.0;
    private static final double SLERP_ARRIVAL_DISTANCE = 2.0;
    private static final long SLERP_TIMEOUT_TICKS = 50;
    private static final int SLERP_TELEPORT_DURATION = 2;
    private static final int LUNGE_TELEPORT_DURATION = 2;
    private static final int LUNGE_DELAY_MS = 200;
    private static final double EYE_HEIGHT_MULTIPLIER = 6.0;

    private ScheduledFuture<?> attackTask;

    @Override
    public String name() {
        return "GRAB_IMPALE";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);
        moveToReadyPosition(blade);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setFinishedLunging(false);
        blade.getBladeMotion().stop();
        if (attackTask != null) attackTask.cancel(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Driver advances motion; FSM transitions read hitEntity / finishedLunging.
    }

    private void moveToReadyPosition(UmbralBlade blade) {
        SwordEntity grabbed = blade.getThrower().getGrabbedEntity();
        Vector offset = new Vector(-1, grabbed.getEyeHeight() * EYE_HEIGHT_MULTIPLIER, -1);

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
        blade.setCtrlPointsForLunge(AttackType.LUNGE1.controlVectors());

        long durationTicks = Math.max(1, Config.UmbralBlade.LUNGE_TIME_SCALING_FACTOR);
        double timeStep = Config.UmbralBlade.LUNGE_TIME_CUTOFF / durationTicks;
        Basis basis = resolveGrabLungeBasis(blade);

        Debug.umbral("GRAB_LUNGE install: durationTicks=" + durationTicks + " timeStep=" + timeStep
            + " basisFwd=" + basis.forward());

        blade.getBladeMotion().install(new LungingDriver(
            blade.getCtrlPointsForLunge(),
            basis,
            1.0,
            durationTicks,
            timeStep,
            LUNGE_TELEPORT_DURATION,
            buildEntityFilter(blade),
            (entityHit, direction) -> {
                if (!(entityHit.entity() instanceof LivingEntity le)) return;
                Debug.umbral("GRAB_LUNGE entity hit: " + le.getName() + " dir=" + direction);
                blade.setLastVelocity(direction);
                blade.setHitEntity(SwordEntityArbiter.getOrAdd(le));
                blade.impale(le);
            },
            (blockHit, direction) -> {
                Debug.umbral("GRAB_LUNGE block hit: " + blockHit.block().getType() + " dir=" + direction);
                blade.setLastVelocity(direction);
                blade.emitStuckBlockEffect(blockHit.block(), blockHit.position());
                blade.request(BladeRequest.RECALL);
            },
            () -> {
                Debug.umbral("GRAB_LUNGE complete: timeout reached");
                blade.setFinishedLunging(true);
            }
        ));
    }

    /**
     * Builds the basis aimed from the blade toward the grabbed entity's chest. Includes pitch
     * so the lunge correctly handles airborne or elevated grabs.
     */
    private static Basis resolveGrabLungeBasis(UmbralBlade blade) {
        Location bladeLoc = blade.getBladeDisplay().currentLocation();
        if (bladeLoc == null) bladeLoc = blade.getThrower().self().getEyeLocation();
        SwordEntity grabbed = blade.getThrower().getGrabbedEntity();
        Vector dir = grabbed != null
            ? grabbed.getChestLocation().toVector().subtract(bladeLoc.toVector())
            : blade.getThrower().dir().multiply(20);
        return VectorUtil.getBasis(bladeLoc.clone().setDirection(dir), dir);
    }

    private static Predicate<Entity> buildEntityFilter(UmbralBlade blade) {
        return entity -> {
            if (!(entity instanceof LivingEntity le) || le.isDead()) return false;
            if (entity.getUniqueId().equals(blade.getThrower().getUniqueId())) return false;
            return blade.getDisplay() == null
                || !entity.getUniqueId().equals(blade.getDisplay().getUniqueId());
        };
    }
}
