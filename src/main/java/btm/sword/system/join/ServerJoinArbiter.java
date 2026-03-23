package btm.sword.system.join;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.display.BossBarManager;
import btm.sword.system.display.ScoreboardManager;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.system.playerdata.PlayerDataManager;
import btm.sword.system.scene.CameraSystem;

/**
 * Manages the full server-join lifecycle for each player.
 *
 * <h2>Join pipeline</h2>
 * <ol>
 *   <li><b>Register</b> — player is registered with {@link SwordEntityArbiter} and
 *       {@link PlayerDataManager}.</li>
 *   <li><b>Stage</b> (deferred 1 tick) — claim a {@link MenuSlotGrid} dark-room slot,
 *       teleport the player there, set them invisible and zero their velocity. The player
 *       waits here while the client loads chunks and terrain.</li>
 *   <li><b>Route</b> (deferred {@link Config.MenuGrid#LOADING_WAIT_MS} after staging) —
 *       check the player's {@link PlayerData#isJoinSequenceCompleted()} flag and send them
 *       to either:
 *       <ul>
 *         <li>the initial join sequence (first-time players), or</li>
 *         <li>the main menu / character selection scene (returning players).</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h2>Quit pipeline</h2>
 * On disconnect the player's grid slot is released, any active camera controller is
 * stopped, and the {@link SwordPlayer} is cleaned up and removed from the arbiter.
 */
public final class ServerJoinArbiter implements Listener {

    /** Time (ms) after join before staging the player in their dark-room slot. */
    private static final int STAGE_DELAY_MS = 50;

    /**
     * Registers the player with core systems, then defers the staging + routing sequence.
     *
     * @param event the join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        SwordEntityArbiter.register(player);
        PlayerDataManager.register(player);

        SwordScheduler.runBukkitTaskLater(() -> {
            if (!player.isOnline()) return;
            stagePlayer(player, uuid);
        }, STAGE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Releases the player's grid slot, stops any active camera controller, and cleans up
     * the {@link SwordPlayer} instance.
     *
     * @param event the quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        MenuSlotGrid.releaseSlot(uuid);

        // Remove player from any active scoreboards and boss bars
        ScoreboardManager.onPlayerQuit(player);
        BossBarManager.onPlayerQuit(player);

        if (SwordEntityArbiter.get(player) instanceof SwordPlayer sp) {
            CameraSystem.stopController(sp);
            sp.onLeave();
            SwordEntityArbiter.remove(player);
        }

        // Persist this player's data before evicting from cache
        PlayerDataManager.saveAsync(uuid);
        PlayerDataManager.evict(uuid);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Teleports the player into their assigned dark-room staging slot, makes them invisible,
     * and zeroes their velocity. Then waits {@link Config.MenuGrid#LOADING_WAIT_MS} for the
     * client to finish loading before routing to the join sequence.
     */
    private static void stagePlayer(Player player, UUID uuid) {
        Optional<Location> slot = MenuSlotGrid.acquireSlot(uuid);
        if (slot.isPresent()) {
            player.teleport(slot.get());
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setInvisible(true);
        player.setVelocity(new Vector(0, 0, 0));

        SwordScheduler.runBukkitTaskLater(() -> {
            if (!player.isOnline()) return;
            SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(player);
            routePlayer(sp);
        }, Config.MenuGrid.LOADING_WAIT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Routes the player based on their join-sequence completion flag.
     * <ul>
     *   <li>{@code joinSequenceCompleted == false} → {@link InitialJoinCutscene}</li>
     *   <li>{@code joinSequenceCompleted == true}  → spawnpoint → main menu scene</li>
     * </ul>
     */
    private static void routePlayer(SwordPlayer sp) {
        PlayerData data = PlayerDataManager.getPlayerData(sp.player().getUniqueId());
        if (data == null || !data.isJoinSequenceCompleted()) {
            InitialJoinCutscene.play(sp);
        } else {
            enterMainMenuScene(sp);
        }
    }

    /**
     * Teleports a returning player to the configured spawnpoint and readies them for gameplay.
     *
     * <p>The player arrives from the dark-room staging slot in an invisible state. This method
     * restores visibility, sets normal activation context, and places them at the spawnpoint.
     * The full world-space main menu will replace this in the future.</p>
     *
     * @param sp the returning player
     */
    private static void enterMainMenuScene(SwordPlayer sp) {
        Location spawnpoint = spawnpointLocation();
        if (spawnpoint != null) {
            sp.player().teleport(spawnpoint);
        }
        sp.player().setInvisible(false);
    }

    /**
     * Builds the returning-player spawnpoint {@link Location} from config.
     * Returns {@code null} if the configured world cannot be resolved.
     */
    private static Location spawnpointLocation() {
        World world = Bukkit.getWorld(Config.JoinSequence.WORLD);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) return null;
        return new Location(
            world,
            Config.JoinSequence.SPAWNPOINT_X,
            Config.JoinSequence.SPAWNPOINT_Y,
            Config.JoinSequence.SPAWNPOINT_Z,
            Config.JoinSequence.SPAWNPOINT_YAW,
            0.0f
        );
    }
}
