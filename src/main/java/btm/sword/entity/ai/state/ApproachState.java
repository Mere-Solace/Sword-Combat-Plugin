package btm.sword.entity.ai.state;

import java.util.Comparator;
import java.util.List;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.section.HostileConfig;
import btm.sword.entity.ai.HostileAIFacade;
import btm.sword.entity.ai.MobGoalArbiter;
import btm.sword.entity.ai.goal.ApproachGoal;
import btm.sword.entity.ai.goal.LookAtTargetGoal;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.mob.Hostile;

/**
 * Approach AI state for Hostile entities.
 *
 * <p>Pathfinding toward the target is handled by {@link ApproachGoal}. This state manages
 * the ally-scan cadence and supplies the transition conditions for {@link SurroundState}
 * and {@link PreAttackState}.
 */
public class ApproachState extends HostileAIFacade {

    /** Creates an {@code ApproachState}. */
    public ApproachState() {}

    @Override
    public String name() {
        return "APPROACH";
    }

    @Override
    public void onEnter(Hostile h) {
        if (h.getCurrentTarget() == null) return;
        h.getMob().setTarget(h.getCurrentTarget().self());
        h.getMob().setAware(true);
        h.setAllyScanTimer(0);
        h.setNearbyAlliesCount(0);
        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new ApproachGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookAtTargetGoal(h.mob(), h));
    }

    @Override
    public void onTick(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        handleAllyScan(h);
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }

    private void handleAllyScan(Hostile h) {
        if (!h.shouldRunAllyScan()) return;

        double aggroRadius = Math.sqrt(HostileConfig.AGGRO_RANGE_SQUARED);
        List<Hostile> allies = h.self().getWorld().getNearbyLivingEntities(
                h.self().getLocation(), aggroRadius
            ).stream()
            .filter(e -> e != h.self())
            .map(e -> {
                var se = SwordEntityArbiter.get(e);
                return se instanceof Hostile hostile ? hostile : null;
            })
            .filter(ally -> ally != null
                && ally.getCurrentTarget() != null
                && h.getCurrentTarget() != null
                && ally.getCurrentTarget().getUniqueId().equals(h.getCurrentTarget().getUniqueId()))
            .sorted(Comparator.comparing(a -> a.self().getUniqueId()))
            .toList();

        h.setNearbyAlliesCount(allies.size());
    }
}
