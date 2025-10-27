package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class SwordEntityArbiter {
	private static final HashMap<UUID, SwordEntity> existingSwordNPCs = new HashMap<>();
	private static final HashMap<UUID, SwordEntity> onlineSwordPlayers = new HashMap<>();
	
	private static final String[] developerUsernames = new String[2];
	
	static {
		developerUsernames[0] = "BladeSworn";
		developerUsernames[1] = "3e9";
	}
	
	public static boolean checkIfDev(Player player) {
		return Arrays.stream(developerUsernames).anyMatch(str -> str.equals(player.getName()));
	}
	
	public static void register(Entity entity) {
		if (!(entity instanceof LivingEntity)) return;
		
		UUID entityUUID = entity.getUniqueId();
		if (entity instanceof Player player) {
			Objects.requireNonNull(Bukkit.getPlayer(entityUUID)).sendMessage("You're being registered as online.");
			
			PlayerDataManager.register(entityUUID);
			if (onlineSwordPlayers.get(entityUUID) == null) {
				if (checkIfDev(player))
					onlineSwordPlayers.put(entityUUID, new Developer(player, PlayerDataManager.getPlayerData(entityUUID)));
				else
					onlineSwordPlayers.put(entityUUID, new SwordPlayer(player, PlayerDataManager.getPlayerData(entityUUID)));
			}
			else
				onlineSwordPlayers.get(entityUUID).setSelf(player);
			
		}
		else if (!entity.isDead())
			existingSwordNPCs.putIfAbsent(entityUUID, initializeNPC((LivingEntity) entity));
	}
	
	public static void remove(UUID uuid) {
		if (onlineSwordPlayers.remove(uuid) == null) existingSwordNPCs.remove(uuid);
	}
	
	public static SwordEntity get(UUID uuid) {
		return onlineSwordPlayers.getOrDefault(uuid, existingSwordNPCs.get(uuid));
	}
	
	public static SwordEntity getOrAdd(UUID uuid) {
		SwordEntity swordEntity = get(uuid);
		if (swordEntity != null) return swordEntity;
		
		LivingEntity bukkitEntity = (LivingEntity) Bukkit.getEntity(uuid);
		assert bukkitEntity != null;
		register(bukkitEntity);
		
		return get(uuid);
	}
	
	public static SwordEntity initializeNPC(LivingEntity entity) {
		switch (entity.getType()) {
            case ZOMBIE, SKELETON, WITHER_SKELETON, ENDERMAN, WARDEN -> {
				return new Hostile(entity, new CombatProfile());
			}
			default -> {
				return new Passive(entity, new CombatProfile());
			}
		}
	}
}
