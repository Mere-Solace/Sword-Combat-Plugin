package btm.sword.input;

import java.util.concurrent.TimeUnit;

import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

import com.destroystokyo.paper.event.player.PlayerAttackEntityCooldownResetEvent;

import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.util.misc.Debug;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/**
 * Bukkit transport adapter for the Sword input system.
 *
 * <p>This class is intentionally thin. Each Bukkit handler does only three things:</p>
 * <ol>
 *   <li>Resolve the {@link SwordPlayer} for the firing event.</li>
 *   <li>Build an immutable {@link InputIntent}.</li>
 *   <li>Hand it to {@link InputRouter#route(InputIntent, SwordPlayer)} and apply the
 *       returned {@link InputDecision} to the source event via
 *       {@link #applyDecision(Cancellable, InputDecision)}.</li>
 * </ol>
 *
 * <p>Three Bukkit-event-ordering workarounds remain in this class because they are
 * concerns of the transport layer rather than gameplay routing:</p>
 * <ul>
 *   <li><b>Right-click autorepeat suppression</b> — Minecraft fires {@code RIGHT_CLICK_BLOCK}
 *       every tick while the player aims at a block while holding right-click. Cancelling
 *       these synchronously here is the only way to suppress the block-interaction sound
 *       and particle feedback; cancelling later is too late.</li>
 *   <li><b>Spawn-egg restoration</b> — keeps debug spawn eggs from being consumed.</li>
 *   <li><b>1-tick dispatch deferral</b> on {@link PlayerInteractEvent} — gives Bukkit's
 *       inventory-related events time to fire first so {@code isInInventorySession},
 *       {@code isDroppingInInv}, and {@code isSwappingInInv} are accurate when the router
 *       reads them.</li>
 * </ul>
 */
public class InputListener implements Listener {

    /** Pre-attack hook fires before the vanilla attack so left-clicks become combo input. */
    @EventHandler
    public void onNormalAttack(PrePlayerAttackEntityEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = new InputIntent.Attack(event.getPlayer().getUniqueId(), now());
        applyDecision(event, InputRouter.route(intent, player));
    }

    /** Debug-only hook for the vanilla attack-cooldown reset event. */
    @EventHandler
    public void onAttackCooldownResetEvent(PlayerAttackEntityCooldownResetEvent event) {
        Debug.input("PlayerAttackEntityCooldownResetEvent");
    }

    /** Generic player interaction (left/right click on air or block). */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        Action action = event.getAction();
        ItemStack item = player.getItemStackInHand(true);

        // Transport quirk: suppress Minecraft's auto-repeated right-click events while a hold
        // is in progress. Cancelling synchronously also suppresses block-interaction feedback
        // (sounds, particles); cancelling on a later tick is too late.
        if ((action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) && player.isHoldingRight()) {
            event.setCancelled(true);
            return;
        }

        // Transport quirk: restore spawn eggs after use so debug spawning is infinite.
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && item != null && !item.isEmpty() && item.getType().name().endsWith("_SPAWN_EGG")) {
            ItemStack snapshot = item.clone();
            SwordScheduler.runBukkitTaskLater(
                () -> event.getPlayer().getInventory().setItemInMainHand(snapshot),
                1, TimeUnit.MILLISECONDS);
        }

        InputIntent.Side side;
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            side = InputIntent.Side.LEFT;
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            side = InputIntent.Side.RIGHT;
        } else {
            return;
        }

        InputIntent intent = new InputIntent.Interact(
            event.getPlayer().getUniqueId(),
            now(),
            side,
            event.hasBlock() ? event.getClickedBlock() : null
        );

        // Transport quirk: defer one tick so inventory events fire first and the router sees
        // the correct InventoryMode flags on the player.
        SwordScheduler.runBukkitTaskLater(
            () -> applyDecision(event, InputRouter.route(intent, player)),
            Config.Timing.RIGHT_INTERACT_DELAY,
            TimeUnit.MILLISECONDS
        );
    }

    /** Right-click directly on an entity. */
    @EventHandler
    public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = new InputIntent.InteractEntity(event.getPlayer().getUniqueId(), now());
        applyDecision(event, InputRouter.route(intent, player));
    }

    /** Reserved handler; no behaviour is currently implemented. */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // intentionally empty
    }

    /** Item drop. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropEvent(PlayerDropItemEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = new InputIntent.Drop(
            event.getPlayer().getUniqueId(),
            now(),
            event.getItemDrop().getItemStack()
        );
        applyDecision(event, InputRouter.route(intent, player));
    }

    /** Sneak toggle (begin or end). */
    @EventHandler
    public void onSneakEvent(PlayerToggleSneakEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = event.isSneaking()
            ? new InputIntent.SneakBegin(event.getPlayer().getUniqueId(), now())
            : new InputIntent.SneakEnd(event.getPlayer().getUniqueId(), now());
        applyDecision(event, InputRouter.route(intent, player));
    }

    /** Offhand-swap key. */
    @EventHandler
    public void onSwapEvent(PlayerSwapHandItemsEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = new InputIntent.Swap(event.getPlayer().getUniqueId(), now());
        applyDecision(event, InputRouter.route(intent, player));
    }

    /** Hotbar slot change. */
    @EventHandler
    public void onChangeItemEvent(PlayerItemHeldEvent event) {
        SwordPlayer player = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        InputIntent intent = new InputIntent.HotbarChange(
            event.getPlayer().getUniqueId(),
            now(),
            event.getNewSlot()
        );
        applyDecision(event, InputRouter.route(intent, player));
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static void applyDecision(Cancellable event, InputDecision decision) {
        if (decision.cancelEvent()) event.setCancelled(true);
    }
}
