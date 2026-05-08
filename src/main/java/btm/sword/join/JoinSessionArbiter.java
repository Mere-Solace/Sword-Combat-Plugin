package btm.sword.join;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import btm.sword.Sword;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.playerdata.PlayerDataManager;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.scene.camera.CameraSystem;
import btm.sword.ui.bossbar.BossBarManager;
import btm.sword.ui.scoreboard.ScoreboardManager;

/**
 * Top-level Bukkit listener that owns the {@code UUID → JoinSession} map and routes
 * player-lifecycle events into the matching {@link JoinSession}.
 *
 * <h2>Layering</h2>
 * <p>This class translates Bukkit events into one method call on the matching session.
 * It never reads or mutates session state directly. The session itself enforces phase
 * preconditions for every transition; the listener simply asks for the transition to
 * happen and trusts the session to ignore it when invalid.</p>
 *
 * <h2>Spawn override</h2>
 * <p>By listening to {@link PlayerSpawnLocationEvent} the arbiter sets the player's
 * spawn location to a {@link MenuSlotGrid} dark-room slot before the player is placed
 * in the world. This deterministically wins against Paper's saved-position restore on
 * rejoin — no chunk-load or teleport race because the player materializes directly in
 * the slot.</p>
 *
 * <h2>State ownership</h2>
 * <ul>
 *   <li>{@link #sessions} — the only place a {@link JoinSession} reference lives. The
 *       map is the single source of truth for which players are currently in the join
 *       lifecycle. Sessions are evicted via the {@link JoinSession.EvictionCallback}
 *       that the arbiter passes to each session at construction.</li>
 *   <li>{@link MenuSlotGrid} slots — acquired here on spawn, released by
 *       {@link JoinSession#terminate()} for the happy path. The
 *       {@link #onPlayerQuit(PlayerQuitEvent)} handler releases the slot directly only
 *       when no session was created (the player quit during the construction-deferral
 *       window).</li>
 * </ul>
 */
public final class JoinSessionArbiter implements Listener {

    /**
     * Delay (ms) between {@link PlayerSpawnLocationEvent} and the deferred
     * {@link JoinSession} construction. Gives the player connection time to fully
     * materialize so {@link SwordEntityArbiter#getOrAdd} returns a usable wrapper.
     */
    private static final int SESSION_CONSTRUCTION_DELAY_MS = 50;

    private final PlayerWaitingGate gate;
    private final Map<UUID, JoinSession> sessions = new ConcurrentHashMap<>();

    /**
     * Constructs an arbiter bound to the given waiting-phase gate.
     *
     * @param gate the gate used for every session this arbiter creates; never null
     */
    public JoinSessionArbiter(PlayerWaitingGate gate) {
        this.gate = gate;
    }

    // ── Bukkit handlers ──────────────────────────────────────────────────────

    /**
     * Sets the spawn location to a dark-room slot, registers the player with the entity
     * arbiter and player-data cache, and schedules the {@link JoinSession} construction
     * after a short delay.
     *
     * <p>If no slot is available the player falls through to the vanilla spawn location
     * and no session is created. This is logged for ops visibility.</p>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Optional<Location> slot = MenuSlotGrid.acquireSlot(uuid);
        if (slot.isEmpty()) {
            Sword.getInstance().getLogger().warning(
                "No staging slot available for " + player.getName() + " — vanilla spawn used");
            return;
        }
        Location stagingSlot = slot.get();
        event.setSpawnLocation(stagingSlot);

        SwordEntityArbiter.register(player);
        PlayerDataManager.register(player);

        SwordScheduler.runBukkitTaskLater(
            () -> beginSession(player, uuid, stagingSlot),
            SESSION_CONSTRUCTION_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Tears down the session if one exists, then performs the non-session quit cleanup
     * (scoreboards, boss bars, camera controllers, entity-arbiter eviction, player-data
     * persistence). Idempotent on re-entry.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        JoinSession session = sessions.get(uuid);
        if (session != null) {
            session.terminate(); // self-evicts via callback; releases slot internally
        } else {
            // Player quit during the session-construction deferral window; the slot was
            // acquired by onSpawnLocation but never handed to a session.
            MenuSlotGrid.releaseSlot(uuid);
        }

        ScoreboardManager.onPlayerQuit(player);
        BossBarManager.onPlayerQuit(player);

        if (SwordEntityArbiter.get(player) instanceof SwordPlayer sp) {
            CameraSystem.stopController(sp);
            sp.onLeave();
            SwordEntityArbiter.remove(player);
        }

        PlayerDataManager.saveAsync(uuid);
        PlayerDataManager.evict(uuid);
    }

    /**
     * On sneak-begin, asks the matching session to cancel its routing countdown. The
     * session enforces the phase precondition so the listener itself never inspects phase.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        JoinSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) session.cancelRouting();
    }

    // ── Session creation ─────────────────────────────────────────────────────

    /**
     * Constructs and starts a session for the given player. Called from the deferred
     * scheduler task launched by {@link #onSpawnLocation(PlayerSpawnLocationEvent)}.
     *
     * <p>If the player went offline during the deferral window, releases the slot and
     * returns. If a stale session exists for the UUID (shouldn't be possible under normal
     * Bukkit listener ordering but the arbiter guards anyway), it is terminated before
     * the new session is registered.</p>
     */
    private void beginSession(Player player, UUID uuid, Location stagingSlot) {
        if (!player.isOnline()) {
            MenuSlotGrid.releaseSlot(uuid);
            return;
        }

        JoinSession existing = sessions.get(uuid);
        if (existing != null) existing.terminate();

        SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(player);
        JoinSession session = new JoinSession(sp, stagingSlot, gate, this::evict);
        sessions.put(uuid, session);
        session.enterStaging();
    }

    // ── Eviction callback ────────────────────────────────────────────────────

    /**
     * Removes the session for {@code uuid} from the map. Bound to every session via
     * {@link JoinSession.EvictionCallback} so a session can self-evict on its happy-path
     * completion ({@link JoinPhase#ACTIVE}) or on {@link JoinSession#terminate()}.
     */
    private void evict(UUID uuid) {
        sessions.remove(uuid);
    }
}
