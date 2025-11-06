package btm.sword.listeners;

import btm.sword.system.entity.SwordEntityArbiter;
import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

public class SystemListener implements Listener {
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        SwordEntityArbiter.removeAllDisplays();
        // Clean up any status displays in this world
        for (Entity entity : event.getWorld().getEntitiesByClass(TextDisplay.class)) {
            if (entity.getScoreboardTags().contains("remove_on_shutdown") && entity.isValid() && !entity.isDead()) {
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onStop(ServerTickEndEvent event) {

    }

    @EventHandler
    public void onStop(ServerExceptionEvent event) {

    }
}
