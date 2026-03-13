package btm.sword.listeners;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;

public class WorldListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack before = event.getItemInHand().clone();

        SwordScheduler.runBukkitTaskLater(() -> {
            ItemStack current = player.getInventory().getItem(hand);
            if (current == null || current.isEmpty()) {
                player.getInventory().setItem(hand, before);
            } else {
                current.setAmount(before.getAmount());
            }
        }, 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        event.setReplacement(event.getItem().clone());
    }
}
