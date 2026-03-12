package btm.sword.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class WorldListener implements Listener {
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        event.setCancelled(true);
    }
}
