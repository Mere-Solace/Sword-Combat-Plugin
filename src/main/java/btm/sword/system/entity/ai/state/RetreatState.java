package btm.sword.system.entity.ai.state;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

/**
 * Retreat AI state for Hostile entities.
 * <p>
 * After landing an attack, the mob backs off 8 blocks directly away from the target
 * and holds position for the configured retreat duration before re-engaging.
 * </p>
 */
public class RetreatState extends HostileAIFacade {

    /** Distance in blocks to retreat from the target. */
    private static final double RETREAT_DISTANCE = 8.0;

    @Override
    public String name() {
        return "RETREAT";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setRetreatTimer(Config.Hostile.RETREAT_TICKS);

        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        Vector away = h.self().getLocation()
            .subtract(h.getCurrentTarget().self().getLocation())
            .toVector();

        if (away.lengthSquared() < 0.001) {
            away = new Vector(1, 0, 0); // fallback if coincident
        }

        Location retreatPos = h.self().getLocation().add(away.normalize().multiply(RETREAT_DISTANCE));
        h.getPathfinder().moveTo(retreatPos, 1.1);
    }

    @Override
    public void onTick(Hostile h) {
        h.setRetreatTimer(h.getRetreatTimer() - 1);
    }

    @Override
    public void onExit(Hostile h) {
        h.setRetreatTimer(0);
    }
}
