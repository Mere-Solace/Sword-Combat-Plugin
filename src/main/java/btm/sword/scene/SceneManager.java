package btm.sword.scene;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import btm.sword.config.section.SceneConfig;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.scene.camera.CameraController;
import btm.sword.scene.camera.CameraSystem;
import btm.sword.scene.fake.FakePlayerManager;

/**
 * Public API for orchestrating the static menu scene pipeline.
 * <p>
 * This is a thin static coordinator — all state lives in
 * {@link StaticSceneController} and {@link FakePlayerManager}.
 * Callers should use only {@link #enterStaticMenuScene} and
 * {@link #exitStaticMenuScene}; the full cleanup runs automatically when
 * the controller stops (including on SHIFT exit via {@link btm.sword.scene.animation.CutsceneInputHandler}).
 * </p>
 *
 * <h2>Pipeline — enter</h2>
 * <ol>
 *   <li>Snapshot the player's current location (return anchor).</li>
 *   <li>Teleport the real player body to the configured safe anchor (off-screen).</li>
 *   <li>Spawn a fake player NPC at the snapshot location (display position).</li>
 *   <li>Compute the camera location (config-driven distance + height offset, facing NPC).</li>
 *   <li>Start a {@link MenuSceneController} which locks the camera and input.</li>
 * </ol>
 *
 * <h2>Pipeline — exit</h2>
 * Triggered by SHIFT (via {@link btm.sword.scene.animation.CutsceneInputHandler}) or by
 * an explicit {@link #exitStaticMenuScene} call. The {@link MenuSceneController} handles:
 * <ul>
 *   <li>Camera restore and input unlock (inherited from {@link StaticSceneController}).</li>
 *   <li>NPC despawn via {@link FakePlayerManager}.</li>
 *   <li>Real player teleport back to the return anchor.</li>
 * </ul>
 */
public final class SceneManager {

    private SceneManager() {}

    /**
     * Enters the static menu scene for the given player.
     * <p>
     * The fake player NPC spawns at the player's current location; the real player body
     * is moved to the configured safe anchor. Use
     * {@link #enterStaticMenuScene(SwordPlayer, Location)} to specify an explicit
     * display position (e.g. for tests that place the NPC at a fixed offset).
     * </p>
     *
     * @param swordPlayer the player to place into the menu scene
     */
    public static void enterStaticMenuScene(SwordPlayer swordPlayer) {
        enterStaticMenuScene(swordPlayer, swordPlayer.player().getLocation());
    }

    /**
     * Enters the static menu scene with an explicit display position for the NPC.
     * <p>
     * Idempotent: if the player already has an active camera controller, the existing
     * controller is stopped first (single-owner enforcement in {@link CameraController}).
     * </p>
     *
     * @param swordPlayer     the player to place into the menu scene
     * @param displayPosition the world location where the fake player NPC will be spawned
     */
    public static void enterStaticMenuScene(SwordPlayer swordPlayer, Location displayPosition) {

        // Move the real player body to a safe off-screen anchor
        Location safeAnchor = getSafeAnchor();
        if (safeAnchor != null) {
            swordPlayer.player().teleport(safeAnchor);
        }

        // Spawn the NPC at the player's original position (equipment snapshotted at call time)
        FakePlayerManager.spawnFakePlayer(swordPlayer, displayPosition);

        // Compute camera position: offset from the display position facing direction
        Location cameraLocation = computeCameraLocation(displayPosition);

        // Start the controller — this locks camera, movement, and input
        new MenuSceneController(cameraLocation, displayPosition).start(swordPlayer);
    }

    /**
     * Exits the static menu scene for the given player.
     * <p>
     * Stopping the controller triggers the full cleanup:
     * NPC despawn, player teleport back, camera restore, and input unlock.
     * </p>
     *
     * @param swordPlayer the player to remove from the menu scene
     */
    public static void exitStaticMenuScene(SwordPlayer swordPlayer) {
        CameraSystem.stopController(swordPlayer);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Builds the safe anchor {@link Location} from the current config values.
     * Returns {@code null} if the configured world does not exist.
     */
    private static Location getSafeAnchor() {
        String worldName = SceneConfig.SAFE_ANCHOR_WORLD;
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().getFirst();
        }
        if (world == null) return null;
        return new Location(world,
            SceneConfig.SAFE_ANCHOR_X,
            SceneConfig.SAFE_ANCHOR_Y,
            SceneConfig.SAFE_ANCHOR_Z
        );
    }

    /**
     * Computes the camera location for the static preview scene.
     * <p>
     * The camera is placed {@link SceneConfig#CAMERA_DISTANCE} blocks in front of the
     * display position (along its facing direction) and {@link SceneConfig#CAMERA_HEIGHT}
     * blocks upward, then aimed back at the display position so the NPC is centered in frame.
     * </p>
     *
     * @param displayPosition the world location of the fake player NPC
     * @return the camera location with yaw/pitch set to face the NPC
     */
    private static Location computeCameraLocation(Location displayPosition) {
        double distance = SceneConfig.CAMERA_DISTANCE;
        double height   = SceneConfig.CAMERA_HEIGHT;

        // Place the camera in front of the NPC (along the direction the NPC faces)
        Location camLoc = displayPosition.clone()
            .add(displayPosition.getDirection().multiply(distance))
            .add(0, height, 0);

        // Face the camera back toward the NPC
        camLoc.setDirection(
            displayPosition.toVector().subtract(camLoc.toVector())
        );

        return camLoc;
    }
}
