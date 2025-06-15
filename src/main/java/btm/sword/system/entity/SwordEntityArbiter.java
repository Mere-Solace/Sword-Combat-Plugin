package btm.sword.system.entity;

import btm.sword.system.playerdata.CombatProfile;
import btm.sword.system.playerdata.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class SwordEntityArbiter {
	private static final HashMap<UUID, SwordEntity> existingSwordNPCs = new HashMap<>();
	private static final HashMap<UUID, SwordPlayer> onlineSwordPlayers = new HashMap<>();
	
	public static void register(LivingEntity entity) {
		UUID entityUUID = entity.getUniqueId();
		if (entity instanceof Player) {
			Objects.requireNonNull(Bukkit.getPlayer(entityUUID)).sendMessage("You're being registered as online.");
			
			PlayerDataManager.register(entityUUID);
			if (onlineSwordPlayers.getOrDefault(entityUUID, null) == null) {
				onlineSwordPlayers.put(entityUUID, new SwordPlayer(entity, PlayerDataManager.getPlayerData(entityUUID)));
			}
			else {
				onlineSwordPlayers.get(entityUUID).setAssociatedEntity(entity);
			}
		} else if (!entity.isDead()) {
			existingSwordNPCs.putIfAbsent(entityUUID, initializeNPC(entity));
		}
	}
	
	public static void remove(UUID uuid) {
		if (onlineSwordPlayers.remove(uuid) == null)
			existingSwordNPCs.remove(uuid);
	}
	
	public static SwordEntity get(UUID uuid) {
		SwordEntity entity = onlineSwordPlayers.get(uuid);
		return entity != null ? entity : existingSwordNPCs.get(uuid);
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
			case ZOMBIE, SKELETON -> {
				return new Hostile(entity, new CombatProfile());
			}
			default -> {
				return new Passive(entity);
			}
		}
	}
}
