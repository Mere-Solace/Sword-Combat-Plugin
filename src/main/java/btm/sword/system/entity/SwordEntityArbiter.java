package btm.sword.system.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Dummy;
import btm.sword.system.entity.types.Hostile;
import btm.sword.system.entity.types.Passive;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.playerdata.PlayerDataManager;

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
    public static void register(LivingEntity entity) {

        UUID entityUUID = entity.getUniqueId();
        if (entity instanceof Player player) {
            PlayerDataManager.register(player);
            if (onlineSwordPlayers.get(entityUUID) == null) {
                onlineSwordPlayers.put(entityUUID, new SwordPlayer(player, PlayerDataManager.getPlayerData(entityUUID)));
            }

            if (Sword.getInstance().isEnabled()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        onlineSwordPlayers.get(entityUUID).onRegister();
                    }
                }.runTaskLater(Sword.getInstance(), 2L);
            }
        }
        else if (!entity.isDead()) {
            SwordEntity swordEntity = initializeNPC((LivingEntity) entity);
            if (swordEntity == null) return;
            existingSwordNPCs.putIfAbsent(entityUUID, swordEntity);
            if (Sword.getInstance().isEnabled()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        SwordEntity swordEntity = existingSwordNPCs.get(entityUUID);
                        if (swordEntity == null) return;
                        swordEntity.onRegister();
                    }
                }.runTaskLater(Sword.getInstance(), 2L);
            }
        }
    }

    /**
     * Removes the {@link SwordEntity} associated with the specified UUID from registration.
     * <p>
     * This removes player SwordEntities from online storage or NPC SwordEntities from the NPC map.
     * </p>
     *
     * @param entity entity to remove
     */
    public static void remove(LivingEntity entity) {
        if (onlineSwordPlayers.remove(entity.getUniqueId()) == null) existingSwordNPCs.remove(entity.getUniqueId());
    }

    /**
     * Gets the {@link SwordEntity} associated with the specified UUID.
     * <p>
     * Prefers returning online player SwordEntities over NPCs.
     * </p>
     *
     * @param entity Living Entity from which the UUID will be used to check
     * @return the SwordEntity corresponding to the UUID, or null if none found
     */
    public static SwordEntity get(LivingEntity entity) {
        return onlineSwordPlayers.getOrDefault(entity.getUniqueId(), existingSwordNPCs.get(entity.getUniqueId()));
    }

    /**
     * Gets the {@link SwordEntity} for the specified UUID,
     * registering and initializing it if it does not already exist.
     *
     * @param entity Living Entity from which the UUID will be used to check
     * @return the registered SwordEntity corresponding to the UUID
     */
    public static SwordEntity getOrAdd(LivingEntity entity) {
        SwordEntity swordEntity = get(entity);
        if (swordEntity != null) return swordEntity;

        register(entity);
        return get(entity);
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
            case ARMOR_STAND -> {
                if (entity instanceof ArmorStand stand) {
                    if (stand.isMarker() || !stand.isVisible() || !stand.getScoreboardTags().contains("dummy")) return null;
                    return new Dummy(stand, new CombatProfile());
                }
                return null;
            }
            case ITEM_DISPLAY, ITEM_FRAME, GLOW_ITEM_FRAME, TEXT_DISPLAY, BLOCK_DISPLAY, FALLING_BLOCK, MINECART,
                 CHEST_MINECART, FURNACE_MINECART, HOPPER_MINECART, PAINTING, OMINOUS_ITEM_SPAWNER, ITEM, PALE_OAK_BOAT,
                 ACACIA_BOAT, BIRCH_BOAT, BIRCH_CHEST_BOAT, ACACIA_CHEST_BOAT, CHERRY_BOAT, CHERRY_CHEST_BOAT, JUNGLE_CHEST_BOAT,
                 DARK_OAK_BOAT, DARK_OAK_CHEST_BOAT, JUNGLE_BOAT, MANGROVE_BOAT, MANGROVE_CHEST_BOAT, OAK_BOAT, OAK_CHEST_BOAT,
                 PALE_OAK_CHEST_BOAT, SPRUCE_BOAT, SPRUCE_CHEST_BOAT, EXPERIENCE_ORB, EYE_OF_ENDER, UNKNOWN, AREA_EFFECT_CLOUD,
                 EGG, END_CRYSTAL, ENDER_PEARL, EXPERIENCE_BOTTLE, TRIDENT, EVOKER_FANGS, WIND_CHARGE, ARROW, BREEZE_WIND_CHARGE,
                 BAMBOO_CHEST_RAFT, BAMBOO_RAFT, FISHING_BOBBER, TNT, TNT_MINECART, COMMAND_BLOCK_MINECART, FIREBALL, FIREWORK_ROCKET,
                 DRAGON_FIREBALL, SMALL_FIREBALL, LEASH_KNOT, LLAMA_SPIT, SHULKER_BULLET, WITHER_SKULL, LINGERING_POTION, LIGHTNING_BOLT ,
                 MARKER -> { return null; }
            default -> {
                return new Passive(entity, new CombatProfile());
            }
        }
    }

    public static Collection<SwordEntity> convertAllToSwordEntities(Collection<LivingEntity> entities) {
        return entities.stream().map(SwordEntityArbiter::getOrAdd).toList();
    }

    public static void removeAllDisplays() {
        for (SwordEntity player : onlineSwordPlayers.values()) {
            ((SwordPlayer) player).endUmbralBlade();
            player.endStatusDisplay();
            ((SwordPlayer) player).endIndicatorDisplay();
        }
    }

    public static void registerAllExistingEntities() {
        Bukkit.getScheduler().runTaskLater(Sword.getInstance(),
            bukkitTask -> {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof LivingEntity livingEntity)
                            register(livingEntity);
                    }
                }
            }, 2L
        );
    }
}
