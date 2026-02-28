package btm.sword.system.entity.ai.state;

import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

import btm.sword.system.entity.ai.MobGoalArbiter;

import btm.sword.system.entity.ai.goal.RandomWanderGoal;

import com.destroystokyo.paper.entity.ai.Goal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Idle AI state for Hostile entities.
 * <p>
 * Patrols randomly near the mob's spawn origin, scanning periodically for nearby
 * players to aggro. Transitions to {@link ApproachState} when a player enters aggro range.
 * </p>
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
//        Bukkit.getMobGoals().removeAllGoals(h.mob());
        Bukkit.getMobGoals().addGoal(h.mob(), 1, new RandomWanderGoal(h.mob()));
    }

    @Override
    public void onTick(Hostile h) {


//        handleWander(h);
        handleAggroScan(h);
    }

    @Override
    public void onExit(Hostile h) {
        h.getMob().setAware(true);
    }

//    private void handleWander(Hostile h) {
//        lastTimeMoved++;
//        if (lastTimeMoved < 100) return;
//
//        lastTimeMoved = 0;
//
////        h.setIdleWanderTimer(h.getIdleWanderTimer() + 1);
////        if (h.getIdleWanderTimer() < 60) return;
////        h.setIdleWanderTimer(0);
//
//        Location origin = h.getOrigin();
//        double offsetX = (RANDOM.nextDouble() * 2 - 1) * 8;
//        double offsetZ = (RANDOM.nextDouble() * 2 - 1) * 8;
//        Location wanderTarget = origin.clone().add(offsetX, 0, offsetZ);
//        h.getPathfinder().moveTo(wanderTarget, 1.0);
//
//        Goal<Mob> goal = new Go
//
//        Bukkit.getMobGoals().addGoal(h.mob(), 1, );
//        h.broadcastMessage(20, "I'm movin around now! : x_off = " + offsetX + " z_off = " + offsetZ);
//    }

    private void handleAggroScan(Hostile h) {
        h.setAggroScanTimer(h.getAggroScanTimer() + 1);
        if (h.getAggroScanTimer() < 10) return;
        h.setAggroScanTimer(0);

        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        Collection<Entity> nearby = h.self().getWorld().getNearbyEntities(
            h.self().getLocation(), aggroRadius, aggroRadius, aggroRadius,
            e -> e instanceof Player
        );

        SwordEntity nearest = nearby.stream()
            .map(e -> SwordEntityArbiter.get((Player) e))
            .filter(se -> se instanceof SwordPlayer)
            .min(Comparator.comparingDouble(
                se -> h.self().getLocation().distanceSquared(se.self().getLocation())
            ))
            .orElse(null);

        h.setNearestScannedTarget(nearest);
    }
}
