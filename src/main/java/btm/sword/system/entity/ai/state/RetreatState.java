package btm.sword.system.entity.ai.state;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookWhereGoingGoal;
import btm.sword.system.entity.ai.goal.RetreatBackoffGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Retreat AI state for Hostile entities.
 *
 * <p>After landing an attack, the mob backs off 8 blocks directly away from the target
 * via {@link RetreatBackoffGoal} and holds position for the configured retreat duration
 * before re-engaging.
 */
public class RetreatState extends HostileAIFacade {

    @Override
    public String name() {
        return "RETREAT";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setRetreatTimer(Config.Hostile.RETREAT_TICKS);
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new RetreatBackoffGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookWhereGoingGoal(h.mob()));
    }

    @Override
    public void onTick(Hostile h) {
        h.setRetreatTimer(h.getRetreatTimer() - 1);
    }

    @Override
    public void onExit(Hostile h) {
        h.setRetreatTimer(0);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
