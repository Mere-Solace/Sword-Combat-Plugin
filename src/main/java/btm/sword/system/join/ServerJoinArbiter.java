package btm.sword.system.join;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.system.playerdata.PlayerDataManager;
import btm.sword.system.scene.CameraSystem;
import btm.sword.system.scene.SceneManager;

/**
 * Manages the full server-join lifecycle for each player.
 *
 * <h2>Join pipeline</h2>
 * <ol>
 *   <li><b>Register</b> — player is registered with {@link SwordEntityArbiter} and
 *       {@link PlayerDataManager}.</li>
 *   <li><b>Stage</b> (deferred 100 ms) — claim a {@link MenuSlotGrid} slot, teleport the
 *       player there, set them invisible and zero their velocity. This hides the player
 *       body in an off-screen staging area while the scene loads.</li>
 *   <li><b>Route</b> (deferred 100 ms after staging) — check the player's
 *       {@link PlayerData#isJoinSequenceCompleted()} flag and send them to either:
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

    /** Time (ms) after join before staging the player on a grid slot. */
    private static final int STAGE_DELAY_MS = 100;

    /** Additional time (ms) after staging before routing to a scene or cutscene. */
    private static final int ROUTE_DELAY_MS = 100;

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
        MenuSlotGrid.releaseSlot(player.getUniqueId());

        if (SwordEntityArbiter.get(player) instanceof SwordPlayer sp) {
            CameraSystem.stopController(sp);
            sp.onLeave();
            SwordEntityArbiter.remove(player);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Teleports the player to their assigned grid slot, makes them invisible, and
     * zeroes their velocity; then schedules the routing step.
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
        }, ROUTE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Routes the player based on their join-sequence completion flag.
     * <ul>
     *   <li>{@code joinSequenceCompleted == false} → initial join sequence</li>
     *   <li>{@code joinSequenceCompleted == true}  → main menu character scene</li>
     * </ul>
     */
    private static void routePlayer(SwordPlayer sp) {
        PlayerData data = PlayerDataManager.getPlayerData(sp.player().getUniqueId());
        if (data == null || !data.isJoinSequenceCompleted()) {
            enterInitialJoinSequence(sp);
        } else {
            SceneManager.enterStaticMenuScene(sp);
        }
    }

    /**
     * Entry point for first-time (or incomplete) join sequences.
     * <p>
     * Currently falls through to the main menu scene as a placeholder. Replace with
     * the initial cutscene controller once it is built (see issue #253).
     * </p>
     *
     * @param sp the player to route
     */
    private static void enterInitialJoinSequence(SwordPlayer sp) {
        // TODO: #253 - Replace with initial join cutscene once it is implemented
        SceneManager.enterStaticMenuScene(sp);
    }
}
