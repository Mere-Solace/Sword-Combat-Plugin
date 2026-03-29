package btm.sword.utility.entity;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.entity.base.SwordEntity;

/**
 * Utility class providing helpful static methods for operations on {@link Entity} objects
 * and {@link SwordEntity} wrappers, particularly checking ground status and managing
 * visual following behavior of item displays.
 */
public final class EntityUtil {
    private EntityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    /**
     * Checks whether the specified {@link Entity} is currently on the ground.
     * This method checks blocks slightly below the entity's location to determine if it stands on solid ground.
     *
     * @param entity the entity to check
     * @return true if the entity is on ground, false otherwise
     */
    public static boolean isOnGround(Entity entity) {
        double maxCheckDist = Config.Detection.GROUND_CHECK_MAX_DISTANCE;
        Location base = entity.getLocation().add(new Vector(0, -maxCheckDist, 0));

        double[] offsets = {-0.3, 0, 0.3};

        for (double x : offsets) {
            for (double z : offsets) {
                if (!base.clone().add(x, 0, z).getBlock().isPassable()) {
                    return true;
                }
            }
        }
        return false;
    }
}
