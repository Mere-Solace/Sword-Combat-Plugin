package btm.sword.event.listener;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.runtime.scheduler.SwordScheduler;

/**
 * Listener for world-level events: block interactions and item consumption.
 *
 * <p>Block breaking and placing are gated behind
 * {@link btm.sword.config.Config.World#BLOCK_INTERACTION_ALLOW_BLOCK_PLACING}. When
 * placing is forbidden the event is cancelled outright; when allowed, any block placed
 * by a player is immediately restored to its pre-placement stack so that blocks are never
 * truly consumed (preserving the combat-focused design intent of not depleting inventory).
 * Item-consumption events are similarly intercepted so consumables are never removed from
 * the player's hand.</p>
 */
public class WorldListener implements Listener {

    /**
     * Cancels block-break events when block interactions are disabled.
     *
     * @param event the {@link BlockBreakEvent} to gate
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels block-place events when block interactions are disabled, or restores the used
     * item stack one tick later so players are never charged a block from their inventory.
     *
     * @param event the {@link BlockPlaceEvent} to gate
     */
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

    /**
     * Prevents consumables from being removed by replacing the consumed item with a clone
     * of itself, effectively making all food/potions infinite.
     *
     * @param event the {@link PlayerItemConsumeEvent} to intercept
     */
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        event.setReplacement(event.getItem().clone());
    }
}
