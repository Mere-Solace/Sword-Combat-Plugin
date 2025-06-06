package btm.sword.system;

import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordNPC;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.system.playerdata.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class SwordEntityArbiter {
	private static final HashMap<UUID, SwordNPC> existingSwordNPCs = new HashMap<>();
	private static final HashMap<UUID, SwordPlayer> onlineSwordPlayers = new HashMap<>();
	
	public static void register(LivingEntity entity) {
		UUID entityUUID = entity.getUniqueId();
		if (entity instanceof Player) {
			Objects.requireNonNull(Bukkit.getPlayer(entityUUID)).sendMessage("You're being registered as online.");
			PlayerDataManager.register(entityUUID);
			onlineSwordPlayers.put(entityUUID, new SwordPlayer(entity, PlayerDataManager.getPlayerData(entityUUID)));
		} else if (!entity.isDead()) {
			existingSwordNPCs.putIfAbsent(entityUUID, new SwordNPC(entity));
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
	
	public static HashSet<SwordEntity> convertToSwordEntities(List<LivingEntity> livingEntities) {
		HashSet<SwordEntity> swordEntities = new HashSet<>();
		for (LivingEntity le : livingEntities) {
			swordEntities.add(getOrAdd(le.getUniqueId()));
		}
		return swordEntities;
	}
}
