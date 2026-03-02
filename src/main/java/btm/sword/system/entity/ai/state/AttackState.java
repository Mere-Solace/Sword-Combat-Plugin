package btm.sword.system.entity.ai.state;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Particle;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.AttackChargeGoal;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Attack AI state for Hostile entities.
 *
 * <p>Charges toward the target at 160% speed via {@link AttackChargeGoal}. Once within
 * melee distance (2.5 blocks), executes one of the mob's available attacks via
 * {@link Hostile#randomAttack()}, then sets a random {@code attackPostRoll} (0–2) that
 * selects the post-attack branch:
 * <ul>
 *   <li>0 → {@link OnGuardState}</li>
 *   <li>1 → {@link AttackReadyState}</li>
 *   <li>2 → {@link AttackState} (combo re-entry)</li>
 * </ul>
 * A {@link org.bukkit.Particle#CRIT} burst is played on entry when this is a combo re-entry.
 */
public class AttackState extends HostileAIFacade {

    /** Melee reach threshold in blocks squared (2.5 blocks). */
    private static final double MELEE_REACH_SQ = 6.25;

    @Override
    public String name() {
        return "ATTACK";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setAttackDone(false);
        if (h.isCombo()) {
            h.self().getWorld().spawnParticle(
                Particle.CRIT,
                h.self().getEyeLocation(),
                8, 0.3, 0.3, 0.3, 0.2
            );
            h.setCombo(false);
        }
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new AttackChargeGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookAtTargetGoal(h.mob(), h));
    }

    @Override
    public void onTick(Hostile h) {
        if (h.isAttackDone()) return;
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        if (h.getPossibleAttacks().isEmpty()) return;

        double distSq = h.self().getLocation().distanceSquared(h.getCurrentTarget().self().getLocation());
        if (distSq <= MELEE_REACH_SQ) {
            h.randomAttack();
            h.setAttackDone(true);
            h.setAttackPostRoll(ThreadLocalRandom.current().nextInt(3));
        }
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
