package btm.sword.system.entity.ai.state;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.FleeGoal;
import btm.sword.system.entity.ai.goal.LookWhereGoingGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Flee AI state for Hostile entities.
 *
 * <p>Triggered when the mob's health drops below
 * {@link btm.sword.config.Config.Hostile#FLEE_HEALTH_FRACTION}. All pathfinding is
 * delegated to {@link FleeGoal}, which recalculates the flee direction away from the
 * nearest player every 20 ticks. Transitions back to {@link IdleState} once no players
 * remain within aggro range.
 */
public class FleeState extends HostileAIFacade {

    @Override
    public String name() {
        return "FLEE";
    }

    @Override
    public void onEnter(Hostile h) {
        h.getMob().setAware(true);
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new FleeGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookWhereGoingGoal(h.mob()));
    }

    @Override
    public void onTick(Hostile h) {
        // Pathfinding and direction recalculation handled by FleeGoal.
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
