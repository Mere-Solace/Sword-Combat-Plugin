package btm.sword.system.scene;

import btm.sword.Sword;
import btm.sword.system.entity.impl.SwordPlayer;

/**
 * Abstract base class for all camera controllers in the scene system.
 * <p>
 * Each controller governs a single player's camera for the duration of a scene.
 * Ownership is enforced at the player level: exactly one controller may be active
 * per player at any time. If a second controller is started while one is already
 * active, the existing controller is forcibly stopped first and a warning is logged.
 * </p>
 * <p>
 * Subclasses implement the three lifecycle hooks {@link #onStart()}, {@link #onTick()},
 * and {@link #onStop()} to set up entities/state, advance the camera each frame,
 * and clean up on teardown respectively.
 * </p>
 */
public abstract class CameraController {

    /** The player this controller is currently driving. Null when inactive. */
    protected SwordPlayer owner;

    /**
     * Called once when the controller becomes active.
     * Subclasses should spawn entities, save player state, and start tick loops here.
     */
    protected abstract void onStart();

    /**
     * Called once when the controller is stopped.
     * Subclasses must cancel tick tasks, remove spawned entities, and restore player state here.
     */
    protected abstract void onStop();

    /**
     * Called each frame (every 50 ms) while the controller is active.
     * Subclasses should update camera position here.
     */
    protected abstract void onTick();

    /**
     * Starts this controller for the given player.
     * <p>
     * If the player already has an active controller, it is forcibly stopped first
     * and a warning is logged. Then this controller is registered on the player and
     * {@link #onStart()} is called.
     * </p>
     *
     * @param player the player whose camera this controller will drive
     */
    public final void start(SwordPlayer player) {
        CameraController existing = player.getActiveCameraController();
        if (existing != null) {
            Sword.getInstance().getLogger().warning(
                "[CameraController] Forcing stop of existing controller "
                    + existing.getClass().getSimpleName()
                    + " for player " + player.player().getName()
                    + " — second controller " + getClass().getSimpleName() + " taking over."
            );
            existing.stop();
        }
        this.owner = player;
        player.setActiveCameraController(this);
        onStart();
    }

    /**
     * Stops this controller and clears the ownership reference on the player.
     * <p>
     * Clears {@code player.activeCameraController} before calling {@link #onStop()}
     * so that re-entrant stop calls (e.g., via tick cleanup) are safe.
     * Subclasses must cache the player reference in {@link #onStart()} if they need
     * it inside {@link #onStop()}.
     * </p>
     */
    public final void stop() {
        if (owner != null) {
            owner.setActiveCameraController(null);
            owner = null;
        }
        onStop();
    }

    /**
     * Returns true if this controller is currently active for its owner player.
     *
     * @return {@code true} if owned and the player's active controller field still points here
     */
    public boolean isActive() {
        return owner != null && owner.getActiveCameraController() == this;
    }
}
