package btm.sword.system.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import btm.sword.Sword;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Dummy;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.system.entity.impl.Passive;
import btm.sword.system.entity.impl.RigHostile;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.mob.MobTypeDefinition;
import btm.sword.system.entity.mob.MobTypeRegistry;
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
public final class SwordEntityArbiter {

    private SwordEntityArbiter() {}

    private static final HashMap<UUID, SwordEntity> EXISTING_SWORD_NPCS = new HashMap<>();
    private static final HashMap<UUID, SwordEntity> ONLINE_SWORD_PLAYERS = new HashMap<>();

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
            if (ONLINE_SWORD_PLAYERS.get(entityUUID) == null) {
                ONLINE_SWORD_PLAYERS.put(entityUUID, new SwordPlayer(player, PlayerDataManager.getPlayerData(entityUUID)));
            }

            if (Sword.getInstance().isEnabled()) {
                SwordScheduler.runBukkitTaskLater(
                    () -> {
                        SwordEntity sp = ONLINE_SWORD_PLAYERS.getOrDefault(entityUUID, null);
                        if (sp != null) sp.onRegister();
                    },
                    200, TimeUnit.MILLISECONDS
                );
            }
        }
        else if (!entity.isDead()) {
            SwordEntity swordEntity = initializeNPC(entity);
            if (swordEntity == null) return;
            EXISTING_SWORD_NPCS.putIfAbsent(entityUUID, swordEntity);

            if (Sword.getInstance().isEnabled()) {
                SwordScheduler.runBukkitTaskLater(
                    () -> {
                        SwordEntity entityToRegister = EXISTING_SWORD_NPCS.get(entityUUID);
                        if (entityToRegister == null) return;
                        entityToRegister.onRegister();
                    }, 200, TimeUnit.MILLISECONDS
                );
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
        if (ONLINE_SWORD_PLAYERS.remove(entity.getUniqueId()) == null) EXISTING_SWORD_NPCS.remove(entity.getUniqueId());
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
        return ONLINE_SWORD_PLAYERS.getOrDefault(entity.getUniqueId(), EXISTING_SWORD_NPCS.get(entity.getUniqueId()));
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
            case ZOMBIE, SKELETON, WITHER_SKELETON, ENDERMAN, WARDEN, RAVAGER, CAVE_SPIDER, PILLAGER, ZOMBIFIED_PIGLIN,
                 HOGLIN, HUSK, SHULKER, SILVERFISH, SLIME, SPIDER, ENDER_DRAGON, EVOKER, ELDER_GUARDIAN, ENDERMITE,
                 BLAZE, MAGMA_CUBE, PHANTOM, WITCH, ILLUSIONER -> {
                CombatProfile profile = new CombatProfile();
                MobTypeDefinition mobType = MobTypeRegistry.getByEntityType(entity.getType());
                if (mobType != null) mobType.applyTo(profile);
                // Use RigHostile for mobs whose visuals are driven by a DEU display rig.
                if (mobType != null && mobType.displayGroup() != null) {
                    return new RigHostile(entity, profile);
                }
                return new Hostile(entity, profile);
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
                 DRAGON_FIREBALL, SMALL_FIREBALL, LEASH_KNOT, LLAMA_SPIT, SHULKER_BULLET, WITHER_SKULL, LINGERING_POTION, LIGHTNING_BOLT,
                 MARKER -> { return null; }
            default -> {
                return new Passive(entity, new CombatProfile());
            }
        }
    }

    /**
     * Converts a collection of Bukkit {@link LivingEntity} instances into their {@link SwordEntity} wrappers,
     * registering any that have not been registered yet.
     *
     * @param entities the entities to convert
     * @return an immutable list of the corresponding {@link SwordEntity} wrappers
     */
    public static Collection<SwordEntity> convertAllToSwordEntities(Collection<LivingEntity> entities) {
        return entities.stream().map(SwordEntityArbiter::getOrAdd).toList();
    }

    /**
     * Removes all visual display entities (umbral blades, status displays, indicator displays)
     * from every currently online {@link SwordPlayer}.
     * Called during plugin shutdown to clean up orphaned display entities.
     */
    public static void removeAllDisplays() {
        for (SwordEntity entity : ONLINE_SWORD_PLAYERS.values()) {
            SwordPlayer sp = (SwordPlayer) entity;
            if (sp.getActiveCameraController() != null) {
                sp.getActiveCameraController().stop();
            }
            sp.endUmbralBlade();
            sp.endStatusDisplay();
            sp.endIndicatorDisplay();
        }
    }

    /**
     * Schedules a deferred scan (2 ticks) of all loaded world entities and registers each
     * {@link LivingEntity} that is not yet tracked. Called on plugin startup so entities
     * that existed before the plugin loaded are picked up.
     */
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

    /**
     * Applies one or more {@link Consumer} actions to every registered NPC and online player.
     * Logs how many entities and players were affected.
     *
     * @param actions varargs of consumers to run on each {@link SwordEntity}
     */
    @SafeVarargs
    public static void applyToAllRegisteredEntities(Consumer<SwordEntity>... actions) {
        int entitiesAffected = 0;
        int playersAffected = 0;
        for (SwordEntity entity : EXISTING_SWORD_NPCS.values()) {
            entitiesAffected++;
            for (Consumer<SwordEntity> action : actions) {
                action.accept(entity);
            }
        }
        for (SwordEntity entity : ONLINE_SWORD_PLAYERS.values()) {
            playersAffected++;
            for (Consumer<SwordEntity> action : actions) {
                action.accept(entity);
            }
        }
        Sword.print(entitiesAffected + " Entities Affected, " + playersAffected + " Players Affected");
    }
}
