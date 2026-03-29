package btm.sword.system.scene;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import btm.sword.Sword;

/**
 * ProtocolLib wrapper for sending {@code ClientboundSetCameraPacket} to players.
 * <p>
 * Reassigns a player's client camera to any entity by sending
 * {@link PacketType.Play.Server#CAMERA}. The player remains in their original game mode
 * — no spectator mode is required.
 * </p>
 *
 * <h2>Packet structure</h2>
 * {@code ClientboundSetCameraPacket} contains a single integer field (index 0):
 * the numeric entity ID of the entity the client should attach the camera to.
 * Sending the player's own entity ID restores the default first-person view.
 *
 * <h2>Prerequisite</h2>
 * The target entity must be spawned and tracked client-side before the packet is sent.
 * A 1-tick delay after entity spawn is sufficient to ensure this.
 */
public final class PacketAdapter {

    private PacketAdapter() {}

    /**
     * Returns {@code true} if ProtocolLib is loaded and its {@link ProtocolManager}
     * is available. If {@code false}, callers should fall back to spectator-mode targeting.
     *
     * @return {@code true} when ProtocolLib is ready to send packets
     */
    public static boolean isAvailable() {
        try {
            return ProtocolLibrary.getProtocolManager() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attaches the player's camera to the given entity by sending
     * {@code ClientboundSetCameraPacket} ({@link PacketType.Play.Server#CAMERA}).
     *
     * @param player the player whose camera to redirect
     * @param entity the entity to use as the new camera viewpoint
     */
    public static void setCameraToEntity(Player player, Entity entity) {
        setCameraTarget(player, entity.getEntityId());
    }

    /**
     * Attaches the player's camera to the entity with the given numeric ID.
     *
     * @param player   the player whose camera to redirect
     * @param entityId the entity ID to attach the camera to
     */
    public static void setCameraTarget(Player player, int entityId) {
        try {
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, entityId);
            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[PacketAdapter] Failed to send CAMERA packet: " + e.getMessage());
        }
    }

    /**
     * Resets the player's camera to their own first-person view by sending
     * {@code ClientboundSetCameraPacket} targeting the player themselves.
     *
     * @param player the player whose camera to restore
     */
    public static void resetCamera(Player player) {
        setCameraTarget(player, player.getEntityId());
    }
}
