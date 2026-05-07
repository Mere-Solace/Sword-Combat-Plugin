package btm.sword.event.listener;


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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.intellij.lang.annotations.Subst;

import btm.sword.Sword;
import btm.sword.combat.dev.AnimationMode;
import btm.sword.combat.dev.AttackDevSession;
import btm.sword.combat.dev.DevMode;
import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.base.SwordEntity;
import btm.sword.entity.player.DevSwordPlayer;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.gamemode.QueueManager;
import btm.sword.gamemode.type.CaptureTheFlag1v1;
import btm.sword.item.core.KeyRegistry;
import btm.sword.item.material.MaterialType;
import btm.sword.item.special.NonMovableItem;
import btm.sword.playerdata.PlayerStorage;
import btm.sword.util.misc.ChatInputCapture;
import btm.sword.util.misc.Debug;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
     * Handles player death events.
     * <p>
     * Forwards the death to the player's active CTF match (if any) so the match can drop
     * the carried flag and schedule a respawn.
     * </p>
     *
     * @param event the {@link PlayerDeathEvent} triggered when a player dies
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        SwordEntity entity = SwordEntityArbiter.getOrAdd(player);
        entity.onDeath();

        AttackDevSession session = AttackDevSession.get(player.getUniqueId());
        if (session != null) {
            if (entity instanceof DevSwordPlayer devPlayer && devPlayer.isInAnimationMode()) {
                AnimationMode.exitSilent(devPlayer);
            }
            session.stopCurrentSession();
            AttackDevSession.remove(player.getUniqueId());
        }

        CaptureTheFlag1v1 match = QueueManager.getActiveCtfMatch(player.getUniqueId());
        if (match != null) {
            match.onPlayerDeath((SwordPlayer) entity);
        }
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

        if (!e.isAbleToPickup()) {
            event.setCancelled(true);
            return;
        }

        // Auto-pickup routing for tagged economy items (players only).
        if (e instanceof SwordPlayer sp) {
            ItemStack picked = event.getItem().getItemStack();
            PlayerStorage storage = sp.getPlayerStorage();

            MaterialType materialType = MaterialType.fromItem(picked);
            if (materialType != null && storage.isAutoPickupMaterials()) {
                int available = SwordPlayer.MATERIAL_SLOTS_TOTAL - storage.getTotalMaterialSlots();
                int toStore = Math.min(picked.getAmount(), available);
                if (toStore > 0) {
                    storage.addMaterial(materialType, toStore);
                    sp.player().sendActionBar(
                        Component.text("[+", NamedTextColor.GREEN)
                            .append(Component.text(toStore, Config.SwordColor.TEXT_COOL))
                            .append(Component.text(" ", NamedTextColor.GREEN))
                            .append(materialType.displayName())
                            .append(Component.text("] → Material Pouch", NamedTextColor.GREEN))
                    );
                    if (toStore >= picked.getAmount()) {
                        event.setCancelled(true);
                        event.getItem().remove();
                    } else {
                        picked.setAmount(picked.getAmount() - toStore);
                    }
                    return;
                }
            }

            Integer creditValue = KeyRegistry.getKeyField(picked, KeyRegistry.CREDIT_ITEM_KEY, PersistentDataType.INTEGER);
            if (creditValue != null && storage.isAutoPickupCredits()) {
                int total = creditValue * picked.getAmount();
                storage.addCredits(total);
                sp.player().sendActionBar(
                    Component.text("[+", NamedTextColor.GREEN)
                        .append(Component.text(total + " ✦", Config.SwordColor.TEXT_COOL))
                        .append(Component.text("] → Currency Pouch", NamedTextColor.GREEN))
                );
                event.setCancelled(true);
                event.getItem().remove();
                return;
            }
        }

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
     * tagged as a {@link btm.sword.item.special.NonMovableItem}.
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
        SwordPlayer sp;
        try {
            sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getViewers().getFirst());
        } catch (Exception e) {
            e.fillInStackTrace();
            return;
        }
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

                if (sp.player().getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
                    event.setCancelled(true);
                    return;
                }

                sp.spawnInventoryDrop(current);
                sp.setItemAtIndex(ItemStack.of(Material.AIR), slot);

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
                player.sendMessage(Component.text("Played sound: ", NamedTextColor.GREEN)
                    .append(Component.text(soundKey, NamedTextColor.WHITE)));
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
                    player.sendMessage(Component.text("Displayed particle: ", NamedTextColor.AQUA)
                        .append(Component.text(particleKey, NamedTextColor.WHITE)));
                } catch (IllegalArgumentException ex) {
                    player.sendMessage(Component.text("Unknown particle: ", NamedTextColor.RED)
                        .append(Component.text(particleKey, NamedTextColor.WHITE)));
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

    /**
     * Locks player XZ position during a recording session.
     *
     * <p>If the player has an active {@link DevMode#RECORDING} session, any XZ movement
     * beyond 0.1 blocks from the locked origin is silently cancelled by teleporting
     * the player back. Y movement (jumping, falling) is allowed so the game remains
     * playable while recording.</p>
     *
     * @param event the {@link PlayerMoveEvent} triggered on any position or look change
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        AttackDevSession session = AttackDevSession.get(event.getPlayer().getUniqueId());
        if (session == null || session.getMode() != DevMode.RECORDING) return;

        org.bukkit.Location locked = session.getLockedOrigin();
        if (locked == null) return;

        org.bukkit.Location to = event.getTo();
        double dx = to.getX() - locked.getX();
        double dz = to.getZ() - locked.getZ();
        if (dx * dx + dz * dz > 0.01) {
            event.setTo(locked.clone().add(0, to.getY() - locked.getY(), 0));
        }
    }

    /** Blade lifecycle (including spectator destroy/recreate) is owned by {@code Combatant.handleUmbralBladeTick()}. */
    @EventHandler
    public void gameChangeEvent(PlayerGameModeChangeEvent event) {
        // intentionally empty
    }

//    @EventHandler
//    public void onItemBreak(PlayerItemBreakEvent event) {
//
//    }
}
