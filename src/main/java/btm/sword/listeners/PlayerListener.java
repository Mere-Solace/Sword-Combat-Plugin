package btm.sword.listeners;

import java.util.Objects;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.item.special.NonMovableItem;
import btm.sword.utility.ChatInputCapture;
import btm.sword.utility.Debug;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;


/**
 * Handles all player-related lifecycle and inventory events for the Sword plugin.
 * <p>
 * This listener manages player registration into the {@link SwordEntityArbiter},
 * monitors joining, quitting, death, respawn, and inventory interactions, and
 * also captures chat input for developer/debug commands such as sound or particle testing.
 * </p>
 */
public class PlayerListener implements Listener {
    /**
     * Handles when a player joins the server.
     * <p>
     * Registers the player with the {@link SwordEntityArbiter} to create
     * their {@link SwordPlayer} instance and sends a greeting message.
     * </p>
     *
     * @param event the {@link PlayerJoinEvent} triggered when a player joins the server
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
//        SwordEntityArbiter.register(p);
        p.sendMessage("Hello!");
    }

    /**
     * Handles when a player leaves the server.
     * <p>
     * Ensures that any {@link SwordPlayer} instance is properly cleaned up,
     * invoking {@link SwordPlayer#onLeave()} before removal from the {@link SwordEntityArbiter}.
     * Logs the departure in the server console.
     * </p>
     *
     * @param event the {@link PlayerQuitEvent} triggered when a player quits the server
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (SwordEntityArbiter.get(event.getPlayer()) instanceof SwordPlayer sp) {
            sp.onLeave();
            SwordEntityArbiter.remove(sp.self());
            Sword.getInstance().getLogger().info(event.getPlayer().getName() + " has left the server ;(");
        }
    }

    /**
     * Handles player death events.
     * <p>
     * Currently unimplemented, but can be used in the future to track death-related
     * statistics or handle cleanup of transient effects.
     * </p>
     *
     * @param event the {@link PlayerDeathEvent} triggered when a player dies
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        SwordEntityArbiter.getOrAdd(event.getPlayer()).onDeath();
    }

    /**
     * Handles player respawn events.
     * <p>
     * Re-registers the player in the {@link SwordEntityArbiter} to restore
     * their {@link SwordPlayer} state after respawn. Also calls {@link SwordPlayer#onSpawn()}.
     * </p>
     *
     * @param event the {@link PlayerRespawnEvent} triggered when a player respawns
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        SwordEntityArbiter.register(event.getPlayer());
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());
        swordPlayer.onSpawn();
    }

    /**
     * Handles item pickup events.
     * <p>
     * Prevents entities that are not allowed to pick up items from doing so.
     * Determined via {@link SwordEntity#isAbleToPickup()}.
     * </p>
     *
     * @param event the {@link EntityPickupItemEvent} triggered when an entity attempts to pick up an item
     */
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        SwordEntity e = SwordEntityArbiter.getOrAdd(event.getEntity());

        if (!e.isAbleToPickup()) event.setCancelled(true);

        if (e.isMainHandEmpty()) {
            event.getItem().remove();
            e.setItemStackInHand(event.getItem().getItemStack(), true);
            event.setCancelled(true);
        }
    }

    /**
     * Handles general inventory events.
     * <p>
     * Broadcasts debug messages to player viewers about inventory state.
     * Primarily used for diagnostic output rather than gameplay logic.
     * </p>
     *
     * @param event the {@link InventoryEvent} triggered during any inventory interaction
     */
    @EventHandler
    public void inventoryEvent(InventoryEvent event) {
        if (!Config.Debug.LOGGING_VERBOSE_INVENTORY) return;
        for (HumanEntity human : event.getViewers()) {
            if (human instanceof Player) {
                SwordEntityArbiter.get(human)
                    .message("getInventory(): " + event.getInventory() + "\n  getView(): " + event.getView());
            }
        }
    }

    /**
     * Tracks when the player opens an inventory, enabling drag-outside-window drop detection.
     *
     * @param event the {@link InventoryOpenEvent} triggered when a player opens an inventory
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Debug.inventory("Opened inventory...");
        if (event.getPlayer() instanceof Player p) {
            SwordPlayer sp = ((SwordPlayer) SwordEntityArbiter.getOrAdd(p));
            sp.setInInventorySession(true);
            Debug.inventory("in session?=" + sp.isInInventorySession());
        }
    }

    /**
     * Tracks when the player closes an inventory, disabling drag-outside-window drop detection.
     *
     * @param event the {@link InventoryCloseEvent} triggered when a player closes an inventory
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player p) {
            ((SwordPlayer) SwordEntityArbiter.getOrAdd(p)).setInInventorySession(false);
        }
    }

    /**
     * Prevents players from dragging non-movable items across inventory slots.
     * <p>
     * Drag events are not routed through {@link SwordPlayer#handleInventoryInput},
     * so this handler independently cancels any drag where the cursor item is
     * tagged as a {@link btm.sword.system.item.special.NonMovableItem}.
     * </p>
     *
     * @param event the {@link InventoryDragEvent} triggered when a player drags an item
     */
    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent event) {
        if (NonMovableItem.isNonMovable(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    /**
     * Handles inventory interaction events (clicks, drags, swaps, drops).
     * <p>
     * Routes click-based inputs through {@link SwordPlayer#handleInventoryInput(InventoryClickEvent)}.
     * If handled, the default action is canceled. The commented-out section below contains
     * prototype logic for special interactions like shift-drops and swaps.
     * </p>
     *
     * @param event the {@link InventoryClickEvent} triggered when a player interacts with an inventory slot
     */
    @EventHandler
    public void inventoryInteractEvent(InventoryClickEvent event) {
        SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getViewers().getFirst());

        Debug.sendInventoryClickDebugMessage(event);

        if (sp.handleInventoryInput(event)) {
            event.setCancelled(true);
        }

        ClickType clickType = event.getClick();
        InventoryAction action = event.getAction();

        ItemStack current = event.getCurrentItem();
        int slot = event.getSlot();

        sp.inventoryInfo("click=" + clickType + " action=" + action);

        switch (clickType) {
            case SWAP_OFFHAND -> sp.setSwappingInInv();
            case DROP, CONTROL_DROP -> {
                sp.setDroppingInInv();
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
            }
            case SHIFT_RIGHT -> {
                sp.inventoryInfo("shift-right click, dropping that thang");

                if (current == null || current.isEmpty() || NonMovableItem.isNonMovable(current)) {
                    event.setCancelled(true);
                    break;
                }

                sp.spawnInventoryDrop(current);
                sp.setItemAtIndex(new ItemStack(Material.AIR), slot);

                event.setCancelled(true);
            }
            default -> {}
        }

        switch (action) {
            case DROP_ALL_SLOT, DROP_ALL_CURSOR, DROP_ONE_SLOT, DROP_ONE_CURSOR -> {
                sp.setDroppingInInv();
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
            }
            case SWAP_WITH_CURSOR, HOTBAR_SWAP -> {
                sp.inventoryInfo("swap");
                sp.setSwappingInInv();
            }
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME ->
                sp.inventoryInfo("pickup");
            case PLACE_ALL, PLACE_SOME, PLACE_ONE ->
                sp.inventoryInfo("place");
            default -> {}
        }
    }

    /**
     * Handles chat input events.
     * <p>
     * Parses developer chat commands for sound testing, particle spawning,
     * and item giving. Messages starting with "sound", "particle", or "give"
     * are intercepted and processed accordingly.
     * </p>
     *
     * @param event the {@link AsyncChatEvent} triggered when a player sends a chat message
     */
    @EventHandler
    public void onMessage(AsyncChatEvent event) {
        if (ChatInputCapture.handle(event)) return;

        Player player = event.getPlayer();

        Component msg = event.message();

        String cleaned = PlainTextComponentSerializer.plainText().serialize(msg).trim();

        Sword.getInstance().getLogger().info("Chat input: " + cleaned);

        if (cleaned.startsWith("sound")) {
            String[] parts = cleaned.split("\\s+");
            if (parts.length >= 2) {
                @Subst("king.phylum.classy") String soundKey = parts[1];
                float volume = 1f;
                float pitch = 1f;
                if (parts.length >= 3) {
                    volume = Float.parseFloat(parts[2]);
                }
                if (parts.length >= 4) {
                    pitch = Float.parseFloat(parts[3]);
                }
                Sound sound = Sound.sound(
                        Key.key(soundKey),
                        Sound.Source.PLAYER,
                        volume,
                        pitch
                );
                player.playSound(sound);
                player.sendMessage("§aPlayed sound: " + soundKey);
                event.setCancelled(true);
            }
        }
        else if (cleaned.startsWith("particle ")) {
            String[] parts = cleaned.split("\\s+");
            if (parts.length >= 2) {
                String particleKey = parts[1];
                int count = 10;
                if (parts.length >= 3) {
                    count = Integer.parseInt(parts[2]);
                }
                try {
                    Particle particle = Particle.valueOf(particleKey.toUpperCase());
                    player.getWorld().spawnParticle(
                            particle,
                            player.getLocation().add(0, 1, 0),
                            count
                    );
                    player.sendMessage("§bDisplayed particle: " + particleKey);
                } catch (IllegalArgumentException ex) {
                    player.sendMessage("§cUnknown particle: " + particleKey);
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles player shield disable events.
     * <p>
     * Cancels any attempt to disable a player's shield,
     * effectively making shields indestructible during blocking.
     * </p>
     *
     * @param event the {@link PlayerShieldDisableEvent} triggered when a player's shield would normally disable
     */
    @EventHandler
    public void playerShieldBreakEvent(PlayerShieldDisableEvent event) {
//        event.setCancelled(true);
    }

    @EventHandler
    public void gameChangeEvent(PlayerGameModeChangeEvent event) {
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer());

        if (event.getNewGameMode().equals(GameMode.SPECTATOR)) {
            swordPlayer.requestUmbralBladeState(BladeRequest.DEACTIVATE);
        }
        else if (Objects.equals(event.getPlayer().getGameMode(), GameMode.SPECTATOR)) {
            swordPlayer.requestUmbralBladeState(BladeRequest.ACTIVATE_TO_PREVIOUS);
        }
    }

//    @EventHandler
//    public void onItemBreak(PlayerItemBreakEvent event) {
//
//    }
}
