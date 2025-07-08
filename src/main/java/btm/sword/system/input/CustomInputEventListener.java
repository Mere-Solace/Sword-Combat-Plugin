package btm.sword.system.input;

import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

public class CustomInputEventListener {
	public void startListeningForRightHold(Plugin plugin) {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		
		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ITEM) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
				sp.message("USING AN ITEM (ur probably right clickin)");
				sp.act(InputType.RIGHT);
			}
		});
		
		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				SwordPlayer sp = (SwordPlayer) SwordEntityArbiter.getOrAdd(event.getPlayer().getUniqueId());
				sp.message("USING AN *ENTITY* (ur probably right clickin)");
				sp.act(InputType.RIGHT);
			}
		});
	}
}
