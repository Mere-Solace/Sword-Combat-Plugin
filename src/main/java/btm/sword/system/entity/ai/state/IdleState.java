package btm.sword.system.entity.ai.state;

import java.util.Collection;
import java.util.Comparator;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.IdleWanderGoal;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Idle AI state for Hostile entities.
 *
 * <p>Patrols near the mob's spawn origin via {@link IdleWanderGoal}, scanning
 * periodically for nearby players to aggro. Transitions to {@link ApproachState} when a
 * player enters aggro range.
 */
public class IdleState extends HostileAIFacade {

    @Override
    public String name() {
        return "IDLE";
    }

    @Override
    public void onEnter(Hostile h) {
        h.getMob().setAware(true);
        h.getMob().setAI(true);
        h.setIdleWanderTimer(0);
        h.setAggroScanTimer(0);
        h.setNearestScannedTarget(null);
        h.getPathfinder().stopPathfinding();
        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new IdleWanderGoal(h.mob(), h));
    }

    @Override
    public void onTick(Hostile h) {
        handleAggroScan(h);
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
    }

    private void handleAggroScan(Hostile h) {
        if (!h.shouldRunAggroScan()) return;

        // If the mob remembers a previous target, re-engage them immediately when in range.
        SwordEntity remembered = h.getAggroTarget();
        if (remembered != null) {
            if (!remembered.self().isValid()) {
                h.setAggroTarget(null); // target died; forget them
            } else if (!Hostile.isInvulnerableGameMode(remembered)) {
                double distSq = h.self().getLocation().distanceSquared(remembered.self().getLocation());
                if (distSq <= Config.Hostile.AGGRO_RANGE_SQUARED) {
                    h.setNearestScannedTarget(remembered);
                    return;
                }
            }
        }

        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        Collection<Entity> nearby = h.self().getWorld().getNearbyEntities(
            h.self().getLocation(), aggroRadius, aggroRadius, aggroRadius,
            e -> e instanceof Player
        );

        SwordEntity nearest = nearby.stream()
            .map(e -> SwordEntityArbiter.get((Player) e))
            .filter(se -> se instanceof SwordPlayer)
            .filter(se -> !Hostile.isInvulnerableGameMode(se))
            .min(Comparator.comparingDouble(
                se -> h.self().getLocation().distanceSquared(se.self().getLocation())
            ))
            .orElse(null);

        h.setNearestScannedTarget(nearest);
    }
}
