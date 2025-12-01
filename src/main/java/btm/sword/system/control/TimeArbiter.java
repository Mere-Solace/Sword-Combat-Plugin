package btm.sword.system.control;

import io.papermc.paper.entity.TeleportFlag;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class TimeArbiter {
    // Slowness and Slow falling have but 5 effectively different levels. Keep that in mind.
    public static int CURRENT_TIME_SCALE_MULTIPLIER = 1;

    // these methods provide a central location that every movement/speed call must go through



    public static void setVelocity(Entity entity, Vector velocity) {
        entity.setVelocity(velocity);
    }
}
