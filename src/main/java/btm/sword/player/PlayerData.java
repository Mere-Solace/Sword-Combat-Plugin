package btm.sword.player;

import java.util.UUID;

public class PlayerData {
	private final UUID uuid;
	
	public PlayerData(UUID uuid) {
		this.uuid = uuid;
	}
	
	public UUID getUUID() {
		return uuid;
	}
}
