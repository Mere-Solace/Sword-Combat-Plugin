package btm.sword.scene.camera;

import btm.sword.entity.player.SwordPlayer;

/**
 * Thin static utility for camera controller ownership operations.
 * <p>
 * All state lives on the {@link SwordPlayer} itself — this class simply provides
 * convenience methods that delegate to {@link SwordPlayer#getActiveCameraController()}.
 * There are no maps or tick loops here.
 * </p>
 */
public final class CameraSystem {

    private CameraSystem() {}

    /**
     * Returns true if the player currently has an active camera controller.
     *
     * @param player the player to check
     * @return {@code true} if a controller is active for this player
     */
    public static boolean hasActiveController(SwordPlayer player) {
        return player.getActiveCameraController() != null;
    }

    /**
     * Stops the active camera controller for the given player, if one exists.
     * No-op if no controller is active.
     *
     * @param player the player whose controller should be stopped
     */
    public static void stopController(SwordPlayer player) {
        CameraController controller = player.getActiveCameraController();
        if (controller != null) {
            controller.stop();
        }
    }
}
