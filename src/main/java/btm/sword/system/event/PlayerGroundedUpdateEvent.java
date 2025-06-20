package btm.sword.system.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerGroundedUpdateEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	
	private final Player player;
	private final boolean grounded;
	
	public PlayerGroundedUpdateEvent(Player player, boolean grounded) {
		this.player = player;
		this.grounded = grounded;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean isGrounded() {
		return grounded;
	}
	
	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
