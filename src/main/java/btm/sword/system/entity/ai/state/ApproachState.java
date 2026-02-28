package btm.sword.system.entity.ai.state;

import java.util.Comparator;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

/**
 * Approach AI state for Hostile entities.
 * <p>
 * Moves the mob toward its current target until it reaches attack range.
 * Periodically scans for allied Hostiles targeting the same player; if enough
 * allies are found the mob transitions to {@link SurroundState} instead of attacking alone.
 * </p>
 */
public class ApproachState extends HostileAIFacade {

    /** Hysteresis squared distance (2 blocks) to prevent start/stop oscillation at the boundary. */
    private static final double HYSTERESIS_SQ = 4.0;

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
    }

    @Override
    public void onTick(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        moveTowardTarget(h);
        handleAllyScan(h);
    }

    @Override
    public void onExit(Hostile h) {
        h.getPathfinder().stopPathfinding();
    }

    private void moveTowardTarget(Hostile h) {
        double distSq = h.self().getLocation().distanceSquared(h.getCurrentTarget().self().getLocation());

        if (distSq > Config.Hostile.APPROACH_DISTANCE_SQUARED + HYSTERESIS_SQ) {
            h.getPathfinder().moveTo(h.getCurrentTarget().self(), 1.1);
        } else if (distSq < Config.Hostile.APPROACH_DISTANCE_SQUARED) {
            h.getPathfinder().stopPathfinding();
        }
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
