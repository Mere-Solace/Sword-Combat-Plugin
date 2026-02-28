package btm.sword.system.entity.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.Pathfinder;

import btm.sword.system.action.throwing.types.DroppedItem;
import btm.sword.system.entity.ai.HostileStateMachine;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.WanderProfile;
import btm.sword.system.entity.ai.state.IdleState;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.item.prefab.ItemLibrary;
import btm.sword.utility.Prefab;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a hostile (enemy) entity in the Sword combat system.
 * <p>
 * Extends {@link Combatant} with a finite state machine (FSM) driven AI that governs
 * patrol, aggro, approach, surround, attack, retreat, and flee behaviours.
 * The mob's UmbralBlade is kept permanently in {@code InactiveState} as a foundation
 * for future mob blade interactions.
 * </p>
 */
@Getter
@Setter
public class Hostile extends Combatant {
    private final Mob mob;
    private final Pathfinder pathfinder;
    private Location origin;
    private final List<Consumer<Combatant>> possibleAttacks;

    // AI state machine
    private HostileStateMachine aiStateMachine;
    private WanderProfile wanderProfile = WanderProfile.ROAMER;
    private SwordEntity currentTarget;
    private SwordEntity nearestScannedTarget;

    // AI timers (all count upward and reset at their cadence threshold)
    private int aggroScanTimer;
    private int allyScanTimer;
    private int idleWanderTimer;
    private int fleeScanTimer;

    // Attack / retreat timers (count downward)
    private int preAttackTimer;
    private int retreatTimer;

    // Surround arc state
    private int arcSlotIndex;
    private boolean frontSlot;
    private int nearbyAlliesCount;

    // Attack result flag
    private boolean attackDone;

    ItemStack itemInLeftHand = new ItemStack(Material.SHIELD);
    ItemStack itemInRightHand = new ItemStack(ItemLibrary.sword);

    /**
     * Constructs a new Hostile wrapping the given {@link LivingEntity}.
     *
     * @param associatedEntity the Bukkit living entity to wrap
     * @param combatProfile the combat profile defining stats and settings
     */
    public Hostile(LivingEntity associatedEntity, CombatProfile combatProfile) {
        super(associatedEntity, combatProfile);
        mob = (Mob) self;
        pathfinder = mob.getPathfinder();
        pathfinder.setCanFloat(false);
        pathfinder.setCanOpenDoors(true);
//        pathfinder.

        origin = mob.getLocation();
        possibleAttacks = new ArrayList<>();

        // Register the basic melee attack
        possibleAttacks.add(c -> {
            Hostile h = (Hostile) c;
            SwordEntity target = h.getCurrentTarget();
            if (target == null || !target.self().isValid()) return;
            Vector knockback = target.self().getLocation()
                .subtract(h.self().getLocation())
                .toVector();
            if (knockback.lengthSquared() > 0.001) knockback.normalize();
            knockback.multiply(0.5);
            target.hit(h, Prefab.Attacks.defaultMobHit, knockback);
        });

        EntityEquipment equipment = associatedEntity.getEquipment();
        if (equipment != null) {
            equipment.setItemInMainHand(itemInLeftHand);
            equipment.setItemInOffHand(itemInRightHand);
            equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        }
    }

    /** Returns the underlying Bukkit {@link Mob}. */
    public Mob mob() {
        return mob;
    }

    @Override
    public void onTick() {
        super.onTick();
        if (aiStateMachine != null) {
            aiStateMachine.tick();
        }
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        ((Resource) aspects.getAspect(AspectType.SHARDS)).stopRegenTask(); // prevent regen of shards
        MobGoalArbiter.GOALS.removeAllGoals(mob); // clear all vanilla goals before starting custom AI
        aiStateMachine = new HostileStateMachine(this, new IdleState());
    }

    @Override
    public void onDeath() {
        aiStateMachine = null;
        super.onDeath();
    }

    @Override
    public void onZeroHealth() {
        if (!dead) {
            ItemStack offHand = getItemStackInHand(false);
            if (!offHand.isEmpty()) {
                Vector dropVel = new Vector(
                    Math.random() - 0.5,
                    Math.random() + 0.5,
                    Math.random() - 0.5
                ).multiply(0.5);

                DroppedItem stuck = new DroppedItem(getChestLocation(), dropVel, offHand);
                stuck.register();
            }
        }
        super.onZeroHealth();
    }

    public void broadcastMessage(double radius, String message) {
        for (Entity entity : self().getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player) {
                player.sendMessage("[" + self().getName() + "] " + message);
            }
        }
    }

    /**
     * Sets up the Hostile's UmbralBlade and immediately requests deactivation,
     * keeping the blade permanently in {@code InactiveState}. The display entity
     * is removed as a passenger so no visual sword appears on the mob.
     */
    @Override
    public void setupUmbralBlade() {
//        super.setupUmbralBlade();
//        // Schedule slightly after the 200 ms blade creation to ensure the blade exists
//        SwordScheduler.runBukkitTaskLater(() -> {
//            UmbralBlade blade = getUmbralBlade();
//            if (blade == null) return;
//            blade.request(BladeRequest.DEACTIVATE);
//            // Suppress the visual display entity so no sword appears on the mob
//            if (blade.getDisplay() != null && blade.getDisplay().isValid()) {
//                self().removePassenger(blade.getDisplay());
//                blade.getDisplay().setItemStack(new ItemStack(Material.AIR));
//            }
//        }, 250, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes a random attack from {@link #possibleAttacks}.
     * No-op if the attack list is empty.
     */
    public void randomAttack() {
        if (possibleAttacks.isEmpty()) return;
        Random random = new Random();
        possibleAttacks.get(random.nextInt(possibleAttacks.size())).accept(this);
    }

    public Location getOrigin() {
        return this.origin.clone();
    }
}
