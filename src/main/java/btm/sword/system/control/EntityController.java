package btm.sword.system.control;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import io.papermc.paper.entity.TeleportFlag;

public final class EntityController {

    private EntityController() {}

    public static void teleport(Entity entity, Location location) {
        entity.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }
}
