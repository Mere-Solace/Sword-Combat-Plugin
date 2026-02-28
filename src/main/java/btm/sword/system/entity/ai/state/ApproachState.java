package btm.sword.system.entity.ai.state;

import java.util.Comparator;
import java.util.List;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.ApproachGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Approach AI state for Hostile entities.
 *
 * <p>Pathfinding toward the target is handled by {@link ApproachGoal}. This state manages
 * the ally-scan cadence and supplies the transition conditions for {@link SurroundState}
 * and {@link PreAttackState}.
 */
public class ApproachState extends HostileAIFacade {

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
    }

    @Override
    public void onTick(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        handleAllyScan(h);
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
    }

    private void handleAllyScan(Hostile h) {
        h.setAllyScanTimer(h.getAllyScanTimer() + 1);
        if (h.getAllyScanTimer() < 20) return;
        h.setAllyScanTimer(0);

        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
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
