package btm.sword.system.control;

import io.papermc.paper.entity.TeleportFlag;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class EntityController {

    public static void teleport(Entity entity, Location location) {
        entity.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS);
    }
}
