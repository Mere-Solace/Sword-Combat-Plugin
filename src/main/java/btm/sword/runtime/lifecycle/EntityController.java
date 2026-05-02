package btm.sword.runtime.lifecycle;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import io.papermc.paper.entity.TeleportFlag;

/** Provides controlled entity operations such as teleportation with passenger retention. */
public final class EntityController {

    private EntityController() {}

    /** Teleports the entity to the given location, retaining any passenger entities. */
    public static void teleport(Entity entity, Location location) {
        entity.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }
}
