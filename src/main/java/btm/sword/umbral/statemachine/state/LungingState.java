package btm.sword.umbral.statemachine.state;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import btm.sword.combat.style.AttackType;
import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.input.BladeRequest;
import btm.sword.umbral.motion.drivers.LungingDriver;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;

/**
 * State that propels the blade forward along a Bézier trajectory toward a targeted entity.
 *
 * <p>Drives motion via a {@link LungingDriver} on the blade's motion subsystem. The driver
 * runs per-tick collision scans and hands back results via three callbacks composed at this
 * call site:
 * <ul>
 *   <li>{@code onEntity} — sets {@code hitEntity} + {@code lastVelocity} on the blade and
 *       calls {@code blade.impale(...)} so the FSM transition action can apply knockback;
 *       the LungingState -> LodgedState transition then fires on {@code getHitEntity() != null}.</li>
 *   <li>{@code onBlock} — emits the stuck-block dust pillar effect and requests a recall.</li>
 *   <li>{@code onComplete} — sets {@code finishedLunging = true} so the
 *       LungingState -> RecallingState transition fires.</li>
 * </ul>
 */
public class LungingState extends UmbralStateFacade {

    private static final int LUNGE_TELEPORT_DURATION = 2;

    @Override
    public String name() {
        return "LUNGING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setHitEntity(null);
        blade.setFinishedLunging(false);
        blade.setCtrlPointsForLunge(AttackType.LUNGE1.controlVectors());

        long durationTicks = (long) Math.max(1, Config.UmbralBlade.LUNGE_TIME_CUTOFF);
        double timeStep = Config.UmbralBlade.LUNGE_TIME_SCALING_FACTOR;

        Basis basis = VectorUtil.getBasisWithoutPitch(blade.getThrower().self());

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
                blade.setLastVelocity(direction);
                blade.setHitEntity(SwordEntityArbiter.getOrAdd(le));
                blade.impale(le);
            },
            (blockHit, direction) -> {
                blade.setLastVelocity(direction);
                blade.emitStuckBlockEffect(blockHit.block(), blockHit.position());
                blade.request(BladeRequest.RECALL);
            },
            () -> blade.setFinishedLunging(true)
        ));
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setFinishedLunging(false);
        blade.getBladeMotion().stop();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Driver advances motion; FSM transitions read hitEntity / finishedLunging.
    }

    /** Build an entity filter that excludes the thrower and the blade's own display entity. */
    private static java.util.function.Predicate<Entity> buildEntityFilter(UmbralBlade blade) {
        return entity -> {
            if (!(entity instanceof LivingEntity le) || le.isDead()) return false;
            if (entity.getUniqueId().equals(blade.getThrower().getUniqueId())) return false;
            return blade.getDisplay() == null
                || !entity.getUniqueId().equals(blade.getDisplay().getUniqueId());
        };
    }
}
