package btm.sword.system.entity.ai.state;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

/**
 * Flee AI state for Hostile entities.
 * <p>
 * Triggered when the mob's health drops below {@link Config.Hostile#FLEE_HEALTH_FRACTION}.
 * The mob runs away from the nearest player at maximum speed. Flee direction is
 * recalculated every 20 ticks. Transitions back to {@link IdleState} once no players
 * remain within aggro range.
 * </p>
 */
public class FleeState extends HostileAIFacade {

    /** Distance in blocks to flee toward. */
    private static final double FLEE_DISTANCE = 16.0;

    @Override
    public String name() {
        return "FLEE";
    }

    @Override
    public void onEnter(Hostile h) {
        h.getMob().setAware(true);
        h.setFleeScanTimer(0);
        recalculateFleeDirection(h);
    }

    @Override
    public void onTick(Hostile h) {
        h.setFleeScanTimer(h.getFleeScanTimer() + 1);
        if (h.getFleeScanTimer() >= 20) {
            h.setFleeScanTimer(0);
            recalculateFleeDirection(h);
        }
    }

    @Override
    public void onExit(Hostile h) {
    }

    private void recalculateFleeDirection(Hostile h) {
        double aggroRadius = Math.sqrt(Config.Hostile.AGGRO_RANGE_SQUARED);
        Collection<Player> nearbyPlayers = h.self().getWorld().getNearbyPlayers(
            h.self().getLocation(), aggroRadius
        );

        if (nearbyPlayers.isEmpty()) return;

        Optional<Player> nearest = nearbyPlayers.stream()
            .min(Comparator.comparingDouble(
                p -> p.getLocation().distanceSquared(h.self().getLocation())
            ));

        if (nearest.isEmpty()) return;

        Vector away = h.self().getLocation()
            .subtract(nearest.get().getLocation())
            .toVector();

        if (away.lengthSquared() < 0.001) {
            away = new Vector(1, 0, 0); // fallback if coincident
        }

        Location fleePos = h.self().getLocation().add(away.normalize().multiply(FLEE_DISTANCE));
        h.getPathfinder().moveTo(fleePos, 1.5);
    }
}
