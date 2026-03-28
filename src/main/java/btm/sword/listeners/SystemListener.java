package btm.sword.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

import com.destroystokyo.paper.event.server.ServerExceptionEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;

import btm.sword.system.entity.SwordEntityArbiter;

/**
 * Server-lifecycle Bukkit event listener.
 * <p>
 * Handles global cleanup tasks on world unload (removing tracked display entities and
 * any {@code remove_on_shutdown}-tagged {@link org.bukkit.entity.TextDisplay}s) and
 * provides hooks for server tick-end and exception events.
 * </p>
 */
public class SystemListener implements Listener {

    /**
     * Cleans up all tracked display entities and shutdown-tagged {@link TextDisplay}s
     * when a world is unloaded.
     *
     * @param event the world unload event
     */
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
