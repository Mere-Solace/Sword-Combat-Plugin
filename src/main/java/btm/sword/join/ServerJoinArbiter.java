package btm.sword.join;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import btm.sword.Sword;
import btm.sword.config.section.JoinSequenceConfig;
import btm.sword.config.section.MenuGridConfig;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.join.menu.JoinRouterMenu;
import btm.sword.playerdata.PlayerData;
import btm.sword.playerdata.PlayerDataManager;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.scene.camera.CameraSystem;
import btm.sword.ui.bossbar.BossBarManager;
import btm.sword.ui.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.invui.window.WindowManager;

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
 *   <li><b>Route</b> (deferred {@link MenuGridConfig#LOADING_WAIT_MS} after staging) —
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

    private static final HashMap<UUID, Integer> joins = new HashMap<>();

    /** Time (ms) after join before staging the player in their dark-room slot. */
    private static final int STAGE_DELAY_MS = 50;

    @EventHandler
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        MenuSlotGrid.acquireSlot(uuid).ifPresentOrElse(
            event::setSpawnLocation,
            () -> {} // put player into a waiting loop that checks for an open slot.
        );

        joins.merge(uuid, 1, Integer::sum); // either add a new entry with 1 join, or sum the joins



        SwordEntityArbiter.register(player);
        PlayerDataManager.register(player);

        SwordScheduler.runBukkitTaskLater(() -> {
//            if (!player.isValid()) return;
            try {
                SwordEntityArbiter.getOrAdd(player).message("Joins: " + joins.get(uuid));
                stagePlayer(player, uuid);
            }
            catch (Exception e) {

            }
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
     * and zeroes their velocity. Then waits {@link MenuGridConfig#LOADING_WAIT_MS} for the
     * client to finish loading before routing to the join sequence.
     */
    private static void stagePlayer(Player player, UUID uuid) {
//        MenuSlotGrid.acquireSlot(uuid).ifPresent(player::teleport);

        // clear and save their inventory, turn off all special item checks, disallow everything,
        // fill their inventory with dark gray items

        player.setGameMode(GameMode.CREATIVE);
        player.setInvisible(true);
        Sword.getInstance().getLogger().info("player got staged");

        SwordScheduler.runBukkitTaskLater(() -> {
            if (!player.isOnline()) return;
            beginMainMenuSelectionTask(player);
        }, MenuGridConfig.LOADING_WAIT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param player
     */
    private static void beginMainMenuSelectionTask(Player player) {
        SwordPlayer swordPlayer = (SwordPlayer) SwordEntityArbiter.getOrAdd(player);
        AtomicBoolean madeSelection = new AtomicBoolean(false);
        AtomicReference<Window> openWindow = new AtomicReference<>();

        // Routing consumer: fires exactly once when the player clicks a destination.
        // Closes the menu, runs the configurable countdown, then teleports and flips
        // madeSelection — at which point the velocity-zero ticker below tears down.
        // Transitional; the upcoming JoinSession owns this countdown directly.
        JoinRouterMenu joinRouterMenu = new JoinRouterMenu(
            swordPlayer,
            destination -> {
                if (openWindow.get() != null) openWindow.get().close();
                startRoutingCountdown(swordPlayer, destination, madeSelection);
            }
        );

        // zero velocity and display a selection menu consistently
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> {
                openWindow.set(WindowManager.getInstance().getOpenWindow(player));
                if (openWindow.get() == null && !joinRouterMenu.isSelected()) {
                    joinRouterMenu.open();
                }
                player.setVelocity(new Vector(0, 0, 0));
            },
            0, 50,
            ServerJoinArbiter.class, "stagePlayer",
            new PredicateRunnablePair(
                madeSelection::get,
                () -> {
                    if (openWindow.get() != null) {
                        openWindow.get().close();
                    }
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setInvisible(false);
                }
            )
        );
    }

    /**
     * Runs the routing countdown for the selected destination, then teleports the player
     * and flips {@code madeSelection} to signal the velocity-zero ticker to tear down.
     *
     * <p>Iteration count is derived from
     * {@link JoinSequenceConfig#ROUTING_DURATION_MS} and
     * {@link JoinSequenceConfig#ROUTING_TICK_PERIOD_MS} as
     * {@code (duration / period) - 1} so the lastIterationCallback fires at exactly
     * {@code duration} ms after the click.</p>
     */
    private static void startRoutingCountdown(SwordPlayer swordPlayer,
                                              Destination destination,
                                              AtomicBoolean madeSelection) {
        AtomicInteger ai = new AtomicInteger(0);
        int periodMs = Math.max(1, JoinSequenceConfig.ROUTING_TICK_PERIOD_MS);
        int iterations = Math.max(0,
            (JoinSequenceConfig.ROUTING_DURATION_MS / periodMs) - 1);

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> swordPlayer.displayTitle(
                Component.text(ai.incrementAndGet()),
                Component.text("Preparing to teleport..."),
                0L, periodMs + 100L, 0L
            ),
            0, periodMs, iterations,
            ServerJoinArbiter.class, "startRoutingCountdown",
            () -> {
                swordPlayer.displayTitle(
                    Component.text(ai.incrementAndGet()),
                    Component.text("Teleporting"),
                    0L, periodMs + 100L, 0L
                );
                swordPlayer.teleport(destination.spawn());
                madeSelection.set(true);
            }
        );
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
        World world = Bukkit.getWorld(JoinSequenceConfig.WORLD);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().getFirst();
        }
        if (world == null) return null;
        return new Location(
            world,
            JoinSequenceConfig.SPAWNPOINT_X,
            JoinSequenceConfig.SPAWNPOINT_Y,
            JoinSequenceConfig.SPAWNPOINT_Z,
            JoinSequenceConfig.SPAWNPOINT_YAW,
            0.0f
        );
    }
}
