package btm.sword.system.entity.ai.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.ai.goal.SurroundHoldGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * Surround AI state for Hostile entities.
 *
 * <p>Distributes allied mobs evenly around the target in an arc formation.
 * Pathfinding to each mob's arc slot is handled by {@link SurroundHoldGoal}. This state
 * manages the arc-slot recalculation cadence. The mob assigned arc slot 0 (the "front slot")
 * transitions to {@link PreAttackState} to attack while others hold position.
 */
public class SurroundState extends HostileAIFacade {

    @Override
    public String name() {
        return "SURROUND";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setAllyScanTimer(0);
        h.setFrontSlot(false);
        evaluateArcPosition(h);
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new SurroundHoldGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 3, new LookAtTargetGoal(h.mob(), h));
    }

    @Override
    public void onTick(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        if (h.shouldRunAllyScan()) {
            evaluateArcPosition(h);
        }
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }

    private void evaluateArcPosition(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        List<Hostile> allies = h.self().getWorld().getNearbyLivingEntities(
                h.getCurrentTarget().self().getLocation(), aggroRadius
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
            .toList();

        h.setNearbyAlliesCount(allies.size());

        // Build the full group list sorted by UUID for a stable arc order
        List<Hostile> allMobs = new ArrayList<>(allies);
        allMobs.add(h);
        allMobs.sort(Comparator.comparing(a -> a.self().getUniqueId()));

        int myIndex = allMobs.indexOf(h);
        h.setArcSlotIndex(myIndex);
        h.setFrontSlot(myIndex == 0);
    }
}
