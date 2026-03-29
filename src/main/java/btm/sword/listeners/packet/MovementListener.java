package btm.sword.listeners.packet;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

import btm.sword.Sword;
import btm.sword.utility.Debug;

/**
 * ProtocolLib packet listener that handles two concerns:
 * <ol>
 *   <li><b>Self-interaction cancellation</b> — during DEU camera animations the client's camera
 *       attaches to a virtual entity looking back at the player's model; a right-click therefore
 *       sends {@code USE_ENTITY} targeting the player's own entity ID, which would normally
 *       trigger a vanilla "Cannot interact with self" disconnect. This listener silently cancels
 *       those packets.</li>
 *   <li><b>Movement locking</b> — UUIDs added to {@link #lockedPlayers} have all movement packets
 *       cancelled, freezing the player client-side (used during grab / throw animations).</li>
 * </ol>
 *
 * <p><b>Known issue (TODO):</b> The {@code USE_ENTITY} read can throw a
 * {@code FieldAccessException} if the packet structure differs between client versions.
 * The exception is silently swallowed to prevent server log spam; see the inline comment for
 * the tracked issue.</p>
 */
public class MovementListener implements PacketListener {

    /**
     * Set of player UUIDs whose movement packets should be cancelled.
     * Add a UUID here to freeze a player; remove it to restore normal movement.
     */
    public static final HashSet<UUID> lockedPlayers = new HashSet<>();

    @Override
    public void onPacketSending(PacketEvent event) {

    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        // Prevent "Cannot interact with self" server kick.
        // During DEU camera animations the client's camera is attached to a virtual entity
        // looking back at the player's model. A right-click sends USE_ENTITY targeting the
        // player's own entity ID, which triggers a vanilla disconnect. Cancel it silently.
        // TODO: Fix FieldAccessException — Field index 0 is out of bounds for some packet variants.
        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            try {
                Integer targetId = (Integer) event.getPacket().getModifier().withType(Integer.class).read(0);   // <=== * HERE *
                if (targetId != null && targetId == player.getEntityId()) {
                    event.setCancelled(true);
                }
            }
            catch (Exception ignored) {
                // Silently ignored — see class-level TODO for tracking this issue.
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK ||
            event.getPacketType() == PacketType.Play.Client.POSITION ||
            event.getPacketType() == PacketType.Play.Client.LOOK ||
            event.getPacketType() == PacketType.Play.Client.GROUND ||
            event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {

            Debug.listener("Received a packet.\nPacketType=" + event.getPacketType());

            if (!lockedPlayers.contains(player.getUniqueId())) return;

            event.setCancelled(true);
        }
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder()
            .priority(ListenerPriority.NORMAL)
            .types(
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.GROUND,
                PacketType.Play.Client.ARM_ANIMATION
            )
            .build();
    }

    @Override
    public Plugin getPlugin() {
        return Sword.getInstance();
    }
}
