package btm.sword.listeners.packet;

import java.util.List;

import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.WrappedDataValue;

import btm.sword.Sword;
import btm.sword.utility.Debug;

public class EntityPacketListener implements PacketListener {

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketType type = event.getPacketType();

        // Only handle entity-related packets
        if (!type.name().startsWith("ENTITY") && type != PacketType.Play.Server.ENTITY_METADATA) return;

        Debug.listener("Sending packet: " + type);

        try {
            List<WrappedDataValue> metadata = event.getPacket().getDataValueCollectionModifier().read(0);
            if (metadata != null) {
                StringBuilder indices = new StringBuilder();
                for (WrappedDataValue e : metadata) {
                    indices.append(e.getIndex()).append(" ");
                }
                Debug.listener("Entity ID: " + event.getPacket().getIntegers().read(0)
                    + " Metadata indices: [" + indices.toString().trim() + "]");
            }
        } catch (Exception e) {
            Debug.listener("Error reading packet metadata: " + e);
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        // No incoming handling for now
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder()
            .priority(ListenerPriority.NORMAL)
            .types(
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.ENTITY_TELEPORT,
                PacketType.Play.Server.ENTITY_HEAD_ROTATION,
                PacketType.Play.Server.ENTITY_LOOK,
                PacketType.Play.Server.ENTITY_STATUS,
                PacketType.Play.Server.ENTITY_EFFECT,
                PacketType.Play.Server.ENTITY_DESTROY,
                PacketType.Play.Server.ENTITY_SOUND,
                PacketType.Play.Server.ENTITY_MOVE_LOOK
            )
            .build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.EMPTY_WHITELIST;
    }

    @Override
    public Plugin getPlugin() {
        return Sword.getInstance();
    }
}
