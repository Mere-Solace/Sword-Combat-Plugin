package btm.sword.listeners;

import java.util.function.Consumer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.SwordScheduler;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.input.InputType;
import btm.sword.system.item.KeyRegistry;
import btm.sword.util.entity.InputUtil;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/**
 * Handles all player input events and routes them through the {@link SwordPlayer}
 * system for unified input handling and action evaluation.
 * <p>
 * This listener captures and interprets a wide range of Minecraft input actions —
 * including attacks, start-clicks, drops, swaps, and sneaking — and delegates
 * them to the internal {@link InputType}-based system used by the Sword plugin.
 * </p>
 */
public class InputListener implements Listener {
    /**
     * Handles standard attack inputs (left-clicking entities).
     * <p>
     * This event fires before the normal Bukkit {@code EntityDamageByEntityEvent}
     * and is used to interpret left-clicks as input actions rather than vanilla attacks.
     * If the {@link SwordPlayer} recognizes the item input, the vanilla attack is canceled.
     * </p>
     *
     * @param event the {@link PrePlayerAttackEntityEvent} triggered before a player attacks an entity
     */
    @EventHandler
    public void onNormalAttack(PrePlayerAttackEntityEvent event) {
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        ItemStack item = swordPlayer.getItemStackInHand(true);

        if (swordPlayer.handleItemInteraction(item, InputType.LEFT)) {
            event.setCancelled(true);
            return;
        }

        swordPlayer.act(InputType.LEFT);

        event.setCancelled(true);
    }

    /**
     * Handles general player interaction events (left and start clicks).
     * <p>
     * This includes both air and block interactions. The system distinguishes between
     * left and start inputs, checks for contextual blocking (e.g., interacting with blocks),
     * and routes actions through the {@link SwordPlayer#act(InputType)} pipeline.
     * </p>
     *
     * @param event the {@link PlayerInteractEvent} triggered when a player interacts with air or a block
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        ItemStack item = swordPlayer.getItemStackInHand(true);

        Action action = event.getAction();

        if (swordPlayer.hasPerformedDropAction()) return;

        // TODO: #124 - Log all interactions and figure out if this fires when PrePlayerAttack fires.
        // TODO: #124 - If so, use the flag set in the above method to cancel this event and return.

        if ((action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            if (swordPlayer.handleItemInteraction(item, InputType.LEFT)) {
                event.setCancelled(true);
                return;
            }
            swordPlayer.act(InputType.LEFT);
        }
        else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (swordPlayer.isInteractingWithEntity()) {
                return;
            }

            if (swordPlayer.isAtRoot() &&
                    event.hasBlock() &&
                    InputUtil.isInteractible(event.getClickedBlock())) {
                return;
            }

            if (swordPlayer.handleItemInteraction(item, InputType.RIGHT)) {
                event.setCancelled(true);
                return;
            }
            swordPlayer.act(InputType.RIGHT);
        }
    }

    /**
     * Handles interactions directly targeting entities (start-clicking them).
     * <p>
     * Marks the player as currently interacting with an entity to prevent duplicate
     * actions and ensures start-clicks are correctly registered as {@link InputType#RIGHT}.
     * The flag resets one tick later via a scheduled task.
     * </p>
     *
     * @param event the {@link PlayerInteractEntityEvent} triggered when a player start-clicks an entity
     */
    @EventHandler
    public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        ItemStack item = swordPlayer.getItemStackInHand(true);

        swordPlayer.setInteractingWithEntity(true);

        if (swordPlayer.handleItemInteraction(item, InputType.RIGHT)) {
            event.setCancelled(true);
            return;
        }
        swordPlayer.act(InputType.RIGHT);

        Consumer<SwordPlayer> resetInteractingFlag =
                sp -> sp.setInteractingWithEntity(false);
        SwordScheduler.runConsumerNextTick(resetInteractingFlag, swordPlayer);

        event.setCancelled(true);

//        event.getRightClicked(); // For later use, not need currently.
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {

    }

    /**
     * Handles player item drops.
     * <p>
     * Interprets drop actions as potential inputs for the sword system (e.g., skill triggers).
     * Sets a temporary flag to prevent misinterpretation as other actions.
     * </p>
     *
     * @param event the {@link PlayerDropItemEvent} triggered when a player drops an item
     */
    @EventHandler
    public void onPlayerDropEvent(PlayerDropItemEvent event) {
SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        ItemStack item = swordPlayer.getItemStackInHand(true);

        // Prevent dropping the menu button
        if (KeyRegistry.hasKey(item, KeyRegistry.MAIN_MENU_BUTTON_KEY)) {
            event.setCancelled(true);
            return;
        }

        swordPlayer.setPerformedDropAction(true);

        if (swordPlayer.handleItemInteraction(item, InputType.DROP)) {
            event.setCancelled(true);
        }
        else if (!swordPlayer.isDroppingInInv()) {
            swordPlayer.act(InputType.DROP);
            event.setCancelled(true);
        }

        Consumer<SwordPlayer> resetDroppingFlag =
                sp -> sp.setPerformedDropAction(false);
        SwordScheduler.runConsumerNextTick(resetDroppingFlag, swordPlayer);
    }

    /**
     * Handles player sneaking (shift key) actions.
     * <p>
     * When a player begins sneaking, this is interpreted as a {@link InputType#SHIFT}
     * input and processed accordingly. When the player stops sneaking, the sneaking
     * state is cleared via {@link SwordPlayer#endSneaking()}.
     * </p>
     *
     * @param event the {@link PlayerToggleSneakEvent} triggered when a player toggles sneak state
     */
    @EventHandler
    public void onSneakEvent(PlayerToggleSneakEvent event) {
SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());

        if (event.isSneaking()) {
            swordPlayer.act(InputType.SHIFT);
        }
        else {
            swordPlayer.endSneaking();
        }
    }

    /**
     * Handles swapping items between main hand and offhand.
     * <p>
     * Interprets hand-swapping as an {@link InputType#SWAP} action unless performed
     * within an inventory. Prevents normal behavior if the action is recognized.
     * </p>
     *
     * @param event the {@link PlayerSwapHandItemsEvent} triggered when a player presses the swap key
     */
    @EventHandler
    public void onSwapEvent(PlayerSwapHandItemsEvent event) {
SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        ItemStack item = swordPlayer.getItemStackInHand(true);

        if (swordPlayer.handleItemInteraction(item, InputType.SWAP)) {
            event.setCancelled(true);
        }
        else if (!swordPlayer.isSwappingInInv()) {
            swordPlayer.act(InputType.SWAP);
            event.setCancelled(true);
        }
    }

    /**
     * Handles hotbar item switching (scroll wheel or number key).
     * <p>
     * Temporarily flags the {@link SwordPlayer} as changing hand index to prevent
     * conflicting inputs. Cancels ongoing throw actions and resets input trees if needed.
     * </p>
     *
     * @param event the {@link PlayerItemHeldEvent} triggered when a player changes selected hotbar slot
     */
    @EventHandler
    public void onChangeItemEvent(PlayerItemHeldEvent event) {
SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        swordPlayer.setChangingHandIndex(true);

        if (swordPlayer.inputReliantOnItem()) {
            swordPlayer.resetTree();
        }

        if (swordPlayer.isAttemptingThrow()) {
            ThrowAction.throwCancel(swordPlayer);
        }

        Consumer<SwordPlayer> resetChangingHandFlag =
                sp -> sp.setChangingHandIndex(false);
        SwordScheduler.runConsumerNextTick(resetChangingHandFlag, swordPlayer);
    }
}
