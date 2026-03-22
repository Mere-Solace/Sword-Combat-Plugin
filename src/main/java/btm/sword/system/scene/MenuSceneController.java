package btm.sword.system.scene;

import org.bukkit.Location;

import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Camera controller for the static character-preview menu scene.
 * <p>
 * Extends {@link StaticSceneController} with the full pipeline orchestration:
 * </p>
 * <ul>
 *   <li><b>onStart</b> — delegates to {@link StaticSceneController} for camera lock, then
 *       records the player's return location for later.</li>
 *   <li><b>onStop</b> — delegates to {@link StaticSceneController} for camera restore,
 *       then despawns the fake-player NPC and teleports the real player back to their
 *       original location.</li>
 * </ul>
 * <p>
 * The NPC spawning and safe-anchor teleport happen before this controller is started;
 * {@link SceneManager} is responsible for ordering those steps. This controller only
 * handles teardown cleanup that must run when the controller stops (e.g., on SHIFT exit).
 * </p>
 */
class MenuSceneController extends StaticSceneController {

    private final Location returnLocation;
    private SwordPlayer cachedPlayer;

    /**
     * Creates a menu scene controller.
     *
     * @param cameraLocation world location the camera entity will spawn at (yaw/pitch set)
     * @param returnLocation the location to teleport the real player back to on exit
     */
    MenuSceneController(Location cameraLocation, Location returnLocation) {
        super(cameraLocation);
        this.returnLocation = returnLocation.clone();
    }

    @Override
    protected void onStart() {
        // Cache owner before super — owner is nulled in stop() before onStop() is called,
        // and super.onStart() assigns cachedOwner internally, but we need our own ref.
        cachedPlayer = owner;
        super.onStart();
    }

    @Override
    protected void onTick() {
        super.onTick();
    }

    @Override
    protected void onStop() {
        SwordPlayer player = cachedPlayer;
        cachedPlayer = null;

        // Restore camera, unlock movement, exit scene overlay, restore activation context
        super.onStop();

        if (player == null) return;

        // Despawn the NPC and return the real player to where they were
        FakePlayerManager.despawnFakePlayer(player);
        player.player().teleport(returnLocation);
    }
}
