package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.utility.GrabAction;
import btm.sword.system.playerdata.CombatProfile;
import btm.sword.util.VectorUtil;
import com.destroystokyo.paper.entity.Pathfinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a hostile NPC entity with AI capabilities.
 * <p>
 * Hostile entities extend {@link Combatant} with pathfinding, targeting, and AI behavior.
 * The class provides methods for various AI behaviors such as patrolling, approaching,
 * charging, retreating, and fleeing.
 * </p>
 *
 * <p><b>Note:</b> Many AI methods are currently placeholder implementations and will be
 * fully developed as the AI system evolves. See Issue #37.</p>
 *
 * @see Combatant
 * @see SwordEntity
 * @see Pathfinder
 */
@Getter
@Setter
public class Hostile extends Combatant {
    /** The Bukkit Mob instance for pathfinding control. */
    private final Mob mob;

    /** Pathfinder for controlling mob movement. */
    private final Pathfinder pathfinder;

    /** Currently running pathfinding task. */
    private BukkitTask currentPathfindingTask;

    /** The origin point for patrol behavior. */
    private Location origin;

    /** List of possible attacks this hostile can perform. */
    private final List<Consumer<Combatant>> possibleAttacks;

    /** Item equipped in the left hand. */
    ItemStack itemInLeftHand = new ItemStack(Material.IRON_AXE);

    /** Item equipped in the right hand. */
    ItemStack itemInRightHand = new ItemStack(Material.ENCHANTED_BOOK);

    /**
     * Constructs a Hostile entity with the specified Bukkit entity and combat profile.
     * <p>
     * Initializes pathfinding capabilities, sets equipment, and prepares the AI system.
     * </p>
     *
     * @param associatedEntity the Bukkit LivingEntity to wrap (must be a Mob)
     * @param combatProfile the combat profile with stats and resources
     */
    public Hostile(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
        mob = (Mob) self;
        pathfinder = mob.getPathfinder();
        pathfinder.setCanFloat(false);
        pathfinder.setCanOpenDoors(true);

        origin = mob.getLocation();

        possibleAttacks = new ArrayList<>();

        EntityEquipment equipment = associatedEntity.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(itemInLeftHand);
            equipment.setItemInOffHand(itemInRightHand);

//			equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        }
    }

    /**
     * Gets the underlying Bukkit Mob instance.
     *
     * @return the mob
     */
    public Mob mob() {
        return mob;
    }

    /**
     * Called every tick for AI updates.
     * <p>
     * Currently placeholder - will contain AI logic in future implementation.
     * </p>
     */
    @Override
    public void onTick() {
        super.onTick();


    }

    /**
     * Called when the entity spawns.
     * <p>
     * Currently placeholder - will contain spawn initialization in future implementation.
     * </p>
     */
    @Override
    public void onSpawn() {
        super.onSpawn();

    }

    /**
     * Called when the entity dies.
     * <p>
     * Currently placeholder - will contain death handling in future implementation.
     * </p>
     */
    @Override
    public void onDeath() {

    }

    /**
     * Makes the hostile patrol around a specified origin point.
     * <p>
     * Currently basic implementation - will be enhanced with more sophisticated patrol
     * patterns in future updates.
     * </p>
     *
     * @param origin the center point to patrol around
     */
    public void patrol(Location origin) {
        currentPathfindingTask = new BukkitRunnable() {
            @Override
            public void run() {
                Random random = new Random();
                random.nextFloat();
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 20L);
        pathfinder.moveTo(origin);
    }

    /**
     * Stops all current pathfinding and targeting.
     * <p>
     * Cancels active pathfinding tasks, clears targets, and disables mob awareness.
     * </p>
     */
    public void halt() {
        if (currentPathfindingTask != null && !currentPathfindingTask.isCancelled() && currentPathfindingTask.getTaskId() != -1)
            currentPathfindingTask.cancel();
        pathfinder.moveTo(mob.getLocation());
        mob.setTarget(null);
        mob.setAware(false);
    }

    /**
     * Coordinates with allies to surround target entities.
     * <p>
     * Placeholder - will implement formation positioning in future updates.
     * </p>
     *
     * @param targets the entities to surround
     * @param allies allied combatants to coordinate with
     */
    public void surround(List<SwordEntity> targets, List<Combatant> allies) {
        halt();

    }

    /**
     * Approaches a target entity.
     * <p>
     * Placeholder - will implement pathfinding to target in future updates.
     * </p>
     *
     * @param target the entity to approach
     */
    public void approach(SwordEntity target) {
        halt();

    }

    /**
     * Charges at a target entity with high speed.
     * <p>
     * Placeholder - will implement charging attack in future updates.
     * </p>
     *
     * @param target the entity to charge at
     */
    public void charge(SwordEntity target) {
        halt();

    }

    /**
     * Retreats from a target entity.
     * <p>
     * Placeholder - will implement tactical retreat in future updates.
     * </p>
     *
     * @param target the entity to retreat from
     */
    public void retreat(SwordEntity target) {
        halt();

    }

    /**
     * Flees from multiple threatening entities.
     * <p>
     * Placeholder - will implement escape behavior in future updates.
     * </p>
     *
     * @param targets the entities to flee from
     */
    public void flee(List<SwordEntity> targets) {
        halt();

    }

    /**
     * Executes a random attack from the list of possible attacks.
     */
    public void randomAttack() {
        Random random = new Random();
        possibleAttacks.get(random.nextInt(possibleAttacks.size())).accept(this);
    }

    /**
     * Attempts to grab a nearby entity.
     */
    public void grab() {
        GrabAction.grab(this);
    }

    /**
     * Makes the hostile jump upward.
     */
    public void jump() {
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= 1) cancel();
                mob.setVelocity(VectorUtil.UP.clone().multiply(1));
                i++;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }
}
