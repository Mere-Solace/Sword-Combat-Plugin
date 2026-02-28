package btm.sword.system.entity.ai.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Location;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

/**
 * Surround AI state for Hostile entities.
 * <p>
 * Distributes allied mobs evenly around the target in an arc formation.
 * The mob assigned arc slot 0 (the "front slot") transitions to {@link PreAttackState}
 * to attack, while others hold their positions. Arc assignments are computed by sorting
 * allies by UUID to produce a deterministic, consistent ordering.
 * </p>
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
    }

    @Override
    public void onTick(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        h.setAllyScanTimer(h.getAllyScanTimer() + 1);
        if (h.getAllyScanTimer() >= 20) {
            h.setAllyScanTimer(0);
            evaluateArcPosition(h);
        }

        pathfindToArcPosition(h);
    }

    @Override
    public void onExit(Hostile h) {
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

        // Build the full list including this mob, sorted by UUID for a stable arc order
        List<Hostile> allMobs = new ArrayList<>(allies);
        allMobs.add(h);
        allMobs.sort(Comparator.comparing(a -> a.self().getUniqueId()));

        int totalMobs = allMobs.size();
        int myIndex = allMobs.indexOf(h);
        h.setArcSlotIndex(myIndex);
        h.setFrontSlot(myIndex == 0);
    }

    private void pathfindToArcPosition(Hostile h) {
        if (h.getCurrentTarget() == null) return;

        double radius = Math.sqrt(Config.Hostile.APPROACH_DISTANCE_SQUARED);
        int totalMobs = h.getNearbyAlliesCount() + 1;
        double angle = 2 * Math.PI * h.getArcSlotIndex() / Math.max(1, totalMobs);

        Location targetLoc = h.getCurrentTarget().self().getLocation();
        double x = targetLoc.getX() + radius * Math.cos(angle);
        double z = targetLoc.getZ() + radius * Math.sin(angle);
        Location arcPos = new Location(targetLoc.getWorld(), x, targetLoc.getY(), z);

        h.getPathfinder().moveTo(arcPos, 1.0);
    }
}
