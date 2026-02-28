package btm.sword.system.entity.ai.state;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.AttackChargeGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Attack AI state for Hostile entities.
 *
 * <p>Charges toward the target at 160% speed via {@link AttackChargeGoal}. Once within
 * melee distance (2.5 blocks), executes one of the mob's available attacks via
 * {@link Hostile#randomAttack()}. After the attack fires, the mob transitions to
 * {@link RetreatState}.
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
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new AttackChargeGoal(h.mob(), h));
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
        }
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
    }
}
