package btm.sword.system.scene;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Static service for packet-driven camera attachment and detachment.
 * <p>
 * This is the low-level packet layer of the camera stack. It delegates to
 * {@link PacketAdapter} for NMS packet sending and returns {@link CameraSession}
 * handles that callers use to later restore the player's view.
 * </p>
 *
 * <h2>Stack position</h2>
 * <pre>
 *   CameraController  (abstract — owns lifecycle, used by CameraSystem / SceneManager)
 *        │
 *   CameraService     (packet attach / detach — this class)
 *        │
 *   CameraSession     (per-call active state)
 *        │
 *   PacketAdapter     (NMS reflection — ClientboundSetCameraPacket)
 * </pre>
 *
 * <h2>Availability</h2>
 * {@link PacketAdapter} initialises lazily via reflection. If initialisation fails
 * (e.g. on an unsupported server version), {@link #isAvailable()} returns {@code false}
 * and callers should fall back to spectator-mode targeting.
 */
public class CameraService {

    private CameraService() {}

    /**
     * Returns {@code true} if the underlying {@link PacketAdapter} is ready to send
     * camera packets. If {@code false}, fall back to spectator-mode targeting.
     *
     * @return {@code true} when packet camera is available
     */
    public static boolean isAvailable() {
        return PacketAdapter.isAvailable();
    }

    /**
     * Attaches the player's camera to the given entity via {@code ClientboundSetCameraPacket}.
     * <p>
     * The entity must already be spawned and tracked client-side. A 1-tick delay after
     * spawning the entity is usually sufficient to ensure this.
     * </p>
     *
     * @param player       the player whose camera to redirect
     * @param cameraEntity the entity to use as the camera viewpoint
     * @return a {@link CameraSession} that can be used to restore the player's view
     */
    public static CameraSession attach(Player player, Entity cameraEntity) {
        PacketAdapter.setCameraToEntity(player, cameraEntity);
        return new CameraSession(player);
    }

    /**
     * Detaches the camera by restoring the player's first-person view.
     * Delegates to {@link CameraSession#detach()}.
     *
     * @param session the session returned by a prior {@link #attach} call
     */
    public static void detach(CameraSession session) {
        session.detach();
    }
}
