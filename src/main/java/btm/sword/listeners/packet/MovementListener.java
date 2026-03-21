package btm.sword.listeners.packet;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;

import btm.sword.Sword;
import btm.sword.utility.Debug;

public class MovementListener implements com.comphenix.protocol.events.PacketListener {
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
        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            Integer targetId = (Integer) event.getPacket().getModifier().withType(Integer.class).read(0);
            if (targetId != null && targetId == player.getEntityId()) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.POSITION_LOOK ||
            event.getPacketType() == PacketType.Play.Client.POSITION ||
            event.getPacketType() == PacketType.Play.Client.LOOK ||
            event.getPacketType() == PacketType.Play.Client.GROUND ||
            event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {

            Debug.listener("Received a packet.\nPacketType="+event.getPacketType());

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
