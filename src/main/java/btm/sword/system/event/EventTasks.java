package btm.sword.system.event;

import btm.sword.Sword;
import btm.sword.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class EventTasks {
	public static final HashMap<UUID, Boolean> playerGroundedMap = new HashMap<>(50);
	public static BukkitTask playerGroundedUpdateEventTask;
	
	public static void startPlayerGroundedTask () {
		playerGroundedUpdateEventTask = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					boolean isGroundedNow = EntityUtil.isOnGround(player);
					boolean wasGrounded = playerGroundedMap.getOrDefault(player.getUniqueId(), isGroundedNow);
					
					if (isGroundedNow != wasGrounded) {
						Bukkit.getPluginManager().callEvent(new PlayerGroundedUpdateEvent(player, isGroundedNow));
						playerGroundedMap.put(player.getUniqueId(), isGroundedNow);
					}
				}
			}
		}.runTaskTimer(Sword.getInstance(), 0, 2L);
	}
}
