package btm.sword.system.scene;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import btm.sword.config.Config;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import net.kyori.adventure.sound.Sound;

/**
 * Higher-level coordinator for scene playback.
 * <p>
 * Manages the music loop independently of the camera controller, and tracks which
 * players are currently viewing a scene so that their input can be gated.
 * </p>
 *
 * <h2>Separation of concerns</h2>
 * <ul>
 *   <li>{@link CameraSystem} — camera ownership protocol (delegates to player field).</li>
 *   <li>{@code SceneManager} — music loop + scene-viewer tracking.</li>
 * </ul>
 */
public class SceneManager {

    /** Players currently viewing any scene. Used for input gating. */
    private static final Set<UUID> sceneViewers = new HashSet<>();

    /** Players who have already seen the main menu scene this session. */
    private static final Set<UUID> seenThisSession = new HashSet<>();

    /** Active music loop tasks per player. */
    private static final Map<UUID, TimeArbiter.TaskHandle> musicLoops = new ConcurrentHashMap<>();

    private SceneManager() {}

    /**
     * Starts the main menu scene for the given player.
     * <p>
     * Constructs a {@link GentleDriftCameraController}, starts it, starts the music loop,
     * and marks the player as an active scene viewer.
     * </p>
     *
     * @param player the player to show the scene to
     */
    public static void startMainMenuScene(SwordPlayer player) {
        sceneViewers.add(player.player().getUniqueId());
        seenThisSession.add(player.player().getUniqueId());

        GentleDriftCameraController controller = new GentleDriftCameraController();
        controller.start(player);

        startMusicLoop(player.player());
    }

    /**
     * Stops the scene for the given player — stops music, stops camera, removes viewer flag.
     *
     * @param player the player whose scene should end
     */
    public static void stopScene(SwordPlayer player) {
        stopMusicLoop(player.player());
        CameraSystem.stopController(player);
        sceneViewers.remove(player.player().getUniqueId());
    }

    /**
     * Returns true if the player is currently viewing a scene.
     *
     * @param player the Bukkit player to check
     * @return {@code true} if this player is in a scene
     */
    public static boolean isInScene(Player player) {
        return sceneViewers.contains(player.getUniqueId());
    }

    /**
     * Returns true if the player has already seen the main menu scene this session.
     *
     * @param player the Bukkit player to check
     * @return {@code true} if the scene has already been shown this session
     */
    public static boolean hasSeenScene(Player player) {
        return seenThisSession.contains(player.getUniqueId());
    }

    /**
     * Handles a Shift input from a player who is in a scene — stops the scene.
     *
     * @param player the player pressing Shift to exit the scene
     */
    public static void onShiftInput(SwordPlayer player) {
        if (isInScene(player.player())) {
            stopScene(player);
        }
    }

    private static void startMusicLoop(Player player) {
        stopMusicLoop(player);
        int periodMs = Config.Scene.MENU_MUSIC_DURATION_TICKS * 50;
        TimeArbiter.TaskHandle task = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> player.playSound(Sound.sound(
                Config.Scene.MENU_MUSIC,
                Sound.Source.MASTER,
                Config.Scene.MENU_MUSIC_VOLUME,
                Config.Scene.MENU_MUSIC_PITCH
            )),
            null,
            0, periodMs,
            SceneManager.class, "musicLoop"
        );
        musicLoops.put(player.getUniqueId(), task);
    }

    private static void stopMusicLoop(Player player) {
        TimeArbiter.TaskHandle task = musicLoops.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        player.stopSound(Sound.sound(
            Config.Scene.MENU_MUSIC,
            Sound.Source.MASTER,
            Config.Scene.MENU_MUSIC_VOLUME,
            Config.Scene.MENU_MUSIC_PITCH
        ));
    }
}
