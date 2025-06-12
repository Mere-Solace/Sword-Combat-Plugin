package btm.sword.system.entity;

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
	
	public static HashSet<SwordEntity> convertToSwordEntities(List<LivingEntity> livingEntities) {
		HashSet<SwordEntity> swordEntities = new HashSet<>();
		for (LivingEntity le : livingEntities) {
			swordEntities.add(getOrAdd(le.getUniqueId()));
		}
		return swordEntities;
	}
	
	public static HashSet<LivingEntity> convertToLivingEntities(List<SwordEntity> swordEntities) {
		HashSet<LivingEntity> livingEntities = new HashSet<>();
		for (SwordEntity se : swordEntities) {
			livingEntities.add(se.getAssociatedEntity());
		}
		return livingEntities;
	}
	
	public static SwordNPC initializeNPC(LivingEntity entity) {
		switch (entity.getType()) {
			case ZOMBIE, SKELETON -> { return new Hostile(entity); }
			case VILLAGER, COW -> {return new Passive(entity); }
			default -> { return new SwordNPC(entity); }
		}
	}
	
	public static void reassign(LivingEntity entity) {
		if (entity instanceof Player)
			onlineSwordPlayers.get(entity.getUniqueId()).setAssociatedEntity(entity);
		else
			if (existingSwordNPCs.get(entity.getUniqueId()) != null)
				existingSwordNPCs.get(entity.getUniqueId()).setAssociatedEntity(entity);
	}
}
