package btm.sword.system.scene;

import org.bukkit.entity.Player;

/**
 * Represents an active packet-driven camera attachment for a single player.
 * <p>
 * Created by {@link CameraService#attach(Player, org.bukkit.entity.Entity)} and
 * consumed by {@link CameraService#detach(CameraSession)} (or directly via
 * {@link #detach()}). Calling {@link #detach()} sends a reset camera packet to the
 * player, restoring their first-person view, and marks the session inactive.
 * </p>
 * <p>
 * Sessions are single-use; a detached session cannot be reattached.
 * </p>
 */
public class CameraSession {

    private final Player owner;
    private boolean active;

    /**
     * Constructs a new active session for the given player.
     * Package-private — use {@link CameraService#attach} to create sessions.
     *
     * @param owner the player whose camera was redirected
     */
    CameraSession(Player owner) {
        this.owner = owner;
        this.active = true;
    }

    /**
     * Detaches the camera, restoring the player's first-person view.
     * No-op if already detached.
     */
    public void detach() {
        if (!active) return;
        active = false;
        PacketAdapter.resetCamera(owner);
    }

    /**
     * Returns {@code true} if this session has not yet been detached.
     *
     * @return {@code true} while the camera is still attached
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the player this session belongs to.
     *
     * @return the camera owner
     */
    public Player getOwner() {
        return owner;
    }
}
