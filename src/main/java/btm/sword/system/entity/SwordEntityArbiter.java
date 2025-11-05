package btm.sword.system.entity;

import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Hostile;
import btm.sword.system.entity.types.Passive;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.playerdata.PlayerDataManager;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Manages registration, storage, and retrieval of SwordEntity instances,
 * differentiating between player-controlled entities and NPCs.
 * <p>
 * This class handles wrapping of Bukkit {@link Player} and {@link LivingEntity}
 * objects into the corresponding {@link SwordEntity} types such as {@link SwordPlayer},
 * {@link Hostile}, and {@link Passive}. It also keeps track of online players separately from NPCs.
 * </p>
 */
public class SwordEntityArbiter {
    private static final HashMap<UUID, SwordEntity> existingSwordNPCs = new HashMap<>();
    private static final HashMap<UUID, SwordEntity> onlineSwordPlayers = new HashMap<>();

    /**
     * Registers an {@link Entity} as a {@link SwordEntity} in the system.
     * <p>
     * If the entity is a {@link Player}, registers as a {@link SwordPlayer}
     * <br>
     * If the entity is a non-player LivingEntity and not dead, initializes as NPC with appropriate subclass.
     * </p>
     *
     * @param entity the Bukkit entity to register
     */
    public static void register(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;

        UUID entityUUID = entity.getUniqueId();
        if (entity instanceof Player player) {
            Objects.requireNonNull(Bukkit.getPlayer(entityUUID)).sendMessage("You're being registered as online.");

            PlayerDataManager.register(entityUUID);
            if (onlineSwordPlayers.get(entityUUID) == null) {
                onlineSwordPlayers.put(entityUUID, new SwordPlayer(player, PlayerDataManager.getPlayerData(entityUUID)));
            }
            else
                onlineSwordPlayers.get(entityUUID).setSelf(player);

        }
        else if (!entity.isDead())
            existingSwordNPCs.putIfAbsent(entityUUID, initializeNPC((LivingEntity) entity));
    }

    /**
     * Removes the {@link SwordEntity} associated with the specified UUID from registration.
     * <p>
     * This removes player SwordEntities from online storage or NPC SwordEntities from the NPC map.
     * </p>
     *
     * @param uuid UUID of the entity to remove
     */
    public static void remove(UUID uuid) {
        if (onlineSwordPlayers.remove(uuid) == null) existingSwordNPCs.remove(uuid);
    }

    /**
     * Gets the {@link SwordEntity} associated with the specified UUID.
     * <p>
     * Prefers returning online player SwordEntities over NPCs.
     * </p>
     *
     * @param uuid UUID of the entity to retrieve
     * @return the SwordEntity corresponding to the UUID, or null if none found
     */
    public static SwordEntity get(UUID uuid) {
        return onlineSwordPlayers.getOrDefault(uuid, existingSwordNPCs.get(uuid));
    }

    /**
     * Gets the {@link SwordEntity} for the specified UUID,
     * registering and initializing it if it does not already exist.
     *
     * @param uuid UUID of the entity
     * @return the registered SwordEntity corresponding to the UUID
     */
    public static SwordEntity getOrAdd(UUID uuid) {
        SwordEntity swordEntity = get(uuid);
        if (swordEntity != null) return swordEntity;

        LivingEntity bukkitEntity = (LivingEntity) Bukkit.getEntity(uuid);
        assert bukkitEntity != null;
        register(bukkitEntity);

        return get(uuid);
    }

    /**
     * Creates and initializes an NPC {@link SwordEntity} wrapper for a given {@link LivingEntity}.
     * <p>
     * Chooses subclass type based on the entity type, such as hostile mobs or passive entities.
     * </p>
     *
     * @param entity the Bukkit living entity to wrap
     * @return a new SwordEntity instance wrapping the given entity, of appropriate subclass
     */
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

    public static void removeAllDisplays() {
        for (SwordEntity entity : existingSwordNPCs.values()) {
            entity.endStatusDisplay();
        }

        for (SwordEntity player : onlineSwordPlayers.values()) {
            ((SwordPlayer) player).endSheathedWeapon();
            player.endStatusDisplay();
        }
    }
}
