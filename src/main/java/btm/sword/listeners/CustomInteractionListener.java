package btm.sword.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.hud.HudOverrideManager;
import btm.sword.system.interaction.CustomInteractionContext;
import btm.sword.system.interaction.CustomInteractionManager;
import btm.sword.system.interaction.CustomInventoryInteraction;

/**
 * Bridges custom inventory interactions into Bukkit inventory events.
 */
public class CustomInteractionListener implements Listener {

    /** Enables the custom interaction HUD override when a smithing table is opened. */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.SMITHING && event.getPlayer() instanceof Player player) {
            HudOverrideManager.enableCustomInteractionOverride(player);
        }
    }

    /** Disables the custom interaction HUD override when a smithing table is closed. */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.SMITHING && event.getPlayer() instanceof Player player) {
            HudOverrideManager.disableCustomInteractionOverride(player);
        }
    }

    /** Delegates smithing preparation to the registered {@link CustomInventoryInteraction} if one matches. */
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        CustomInteractionContext context = new CustomInteractionContext(player, event.getView());
        CustomInventoryInteraction interaction = CustomInteractionManager.find(context);
        if (interaction != null) {
            event.setResult(interaction.createResult(context));
        }
    }

    /** Handles result slot clicks in smithing tables, consuming inputs and delivering the crafted result. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.SMITHING) {
            return;
        }
        if (event.getRawSlot() != CustomInteractionContext.SMITHING_RESULT_SLOT) {
            return;
        }

        CustomInteractionContext context = new CustomInteractionContext(player, event.getView());
        CustomInventoryInteraction interaction = CustomInteractionManager.find(context);
        if (interaction == null) {
            return;
        }

        ItemStack result = interaction.createResult(context);
        if (!canPlaceResult(event.getCursor(), result)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        interaction.consumeInputs(context);
        event.getView().setCursor(result);
        context.setTopItem(CustomInteractionContext.SMITHING_RESULT_SLOT, ItemStack.of(Material.AIR));
        player.updateInventory();
    }

    private boolean canPlaceResult(ItemStack cursor, ItemStack result) {
        return cursor == null || cursor.isEmpty()
            || (cursor.isSimilar(result) && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize());
    }
}
