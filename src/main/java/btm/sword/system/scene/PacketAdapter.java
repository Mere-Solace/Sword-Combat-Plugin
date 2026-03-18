package btm.sword.system.scene;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import btm.sword.Sword;

/**
 * Thin NMS reflection wrapper for sending camera-control packets to players.
 * <p>
 * Provides access to {@code ClientboundSetCameraPacket} without requiring compile-time
 * NMS dependencies. All reflection is performed once on first use and cached; the
 * {@link #isAvailable()} flag is set to {@code false} if initialisation fails so
 * callers can degrade gracefully.
 * </p>
 *
 * <h2>Packet</h2>
 * {@code ClientboundSetCameraPacket(int cameraId)} — reassigns the client camera to any
 * entity by its numeric ID. Sending the player's own entity ID restores the default
 * first-person view. The target entity must already be tracked client-side.
 *
 * <h2>Thread safety</h2>
 * {@link #ensureInitialized()} is {@code synchronized}; all packet sends must be called
 * from the main server thread (as with all Bukkit entity operations).
 */
public class PacketAdapter {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Constructor<?> cameraPacketCtor;
    private static Method getHandleMethod;
    private static Field connectionField;
    private static Method sendMethod;

    private PacketAdapter() {}

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            // ClientboundSetCameraPacket(int cameraId) — Paper 1.21.x record constructor
            Class<?> packetClass = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundSetCameraPacket");
            cameraPacketCtor = packetClass.getDeclaredConstructor(int.class);
            cameraPacketCtor.setAccessible(true);

            // CraftPlayer.getHandle() → ServerPlayer
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");

            // ServerPlayer.connection → ServerGamePacketListenerImpl (public field)
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            connectionField = serverPlayerClass.getField("connection");

            // send(Packet<?>) — declared on ServerCommonPacketListenerImpl, inherited publicly
            Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
            sendMethod = connectionField.getType().getMethod("send", packetInterface);

            available = true;
            Sword.getInstance().getLogger().info("[PacketAdapter] Initialized — packet camera available.");
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[PacketAdapter] Initialization failed (" + e.getClass().getSimpleName()
                    + ": " + e.getMessage() + "). Camera will fall back to spectator mode.");
        }
    }

    /**
     * Returns {@code true} if reflection initialisation succeeded and packet sending
     * is available. If {@code false}, callers should fall back to spectator-mode camera.
     *
     * @return {@code true} when the adapter is ready to send packets
     */
    public static boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    /**
     * Sends {@code ClientboundSetCameraPacket} to {@code player}, attaching their
     * client camera to the entity with the given ID.
     * <p>
     * The target entity must be spawned and tracked client-side before this is called
     * (a 1-tick delay after entity spawn is usually sufficient).
     * </p>
     *
     * @param player   the player whose camera to redirect
     * @param entityId the numeric entity ID to attach the camera to
     */
    public static void setCameraTarget(Player player, int entityId) {
        ensureInitialized();
        if (!available) return;
        try {
            Object packet = cameraPacketCtor.newInstance(entityId);
            Object nmsPlayer = getHandleMethod.invoke(player);
            Object conn = connectionField.get(nmsPlayer);
            sendMethod.invoke(conn, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[PacketAdapter] setCameraTarget failed: " + e.getMessage());
        }
    }

    /**
     * Sends {@code ClientboundSetCameraPacket} to {@code player}, attaching their
     * client camera to the given Bukkit entity.
     *
     * @param player the player whose camera to redirect
     * @param entity the entity to use as the new camera viewpoint
     */
    public static void setCameraToEntity(Player player, Entity entity) {
        setCameraTarget(player, entity.getEntityId());
    }

    /**
     * Resets the player's camera back to their own first-person view by sending
     * a camera packet targeting the player themselves.
     *
     * @param player the player whose camera to restore
     */
    public static void resetCamera(Player player) {
        setCameraTarget(player, player.getEntityId());
    }
}
