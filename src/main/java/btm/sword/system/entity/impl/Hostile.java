package btm.sword.system.entity.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.GameMode;
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
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.system.action.throwing.types.DroppedItem;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.ai.HostileStateMachine;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.WanderProfile;
import btm.sword.system.entity.ai.ability.MobAbility;
import btm.sword.system.entity.ai.ability.MobSlashAbility;
import btm.sword.system.entity.ai.ability.MobThrowAbility;
import btm.sword.system.entity.ai.state.IdleState;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.base.CombatProfile;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.item.prefab.ItemLibrary;
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

    /** Ordered list of abilities this mob can select from during pre-attack. */
    private final List<MobAbility> possibleAbilities;

    /** The ability selected at the start of PreAttackState; executed on AttackState entry. */
    private MobAbility pendingAbility;

    /** Per-ability cooldown counters (ticks remaining). Decremented each tick; removed at 0. */
    private final Map<String, Integer> abilityCooldowns;

    /** {@code true} while this mob is grabbed — suppresses AI movement and attack execution. */
    private boolean incapacitated;

    // AI state machine
    private HostileStateMachine aiStateMachine;
    private WanderProfile wanderProfile = WanderProfile.ROAMER;
    private SwordEntity currentTarget;
    private SwordEntity nearestScannedTarget;

    /**
     * Persistent aggro target — not cleared when the mob returns to idle.
     * Allows the mob to immediately re-engage the same player when they re-enter range
     * after a brief retreat. Only cleared when the target dies or enters an invulnerable game mode.
     */
    private SwordEntity aggroTarget;

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

    // Attack result flag and post-attack branching
    private boolean attackDone;

    /** Random 0–2 roll set atomically with {@code attackDone = true}; selects the post-attack branch. */
    private int attackPostRoll;

    /** {@code true} when this {@code AttackState} entry is a combo follow-up. */
    private boolean combo;

    // Post-attack state timers (count downward)
    private int onGuardTimer;
    private int attackReadyTimer;

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

        origin = mob.getLocation();
        possibleAbilities = new ArrayList<>();
        abilityCooldowns = new HashMap<>();

        possibleAbilities.add(new MobSlashAbility());
        possibleAbilities.add(new MobThrowAbility());

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
        tickAbilityCooldowns();
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

    @Override
    public void onGrabbed() {
        incapacitated = true;
        mob.setAware(false);
        MobGoalArbiter.GOALS.removeAllGoals(mob, GoalType.MOVE);
    }

    @Override
    public void onReleased() {
        incapacitated = false;
        mob.setAware(true);
    }

    public void broadcastMessage(double radius, String message) {
        for (Entity entity : self().getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player player) {
                SwordEntityArbiter.getOrAdd(player).combatInfo("[" + self().getName() + "] " + message);
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
     * Returns {@code true} if the given target is a player in creative or spectator mode
     * and should therefore be excluded from aggro scanning and combat targeting.
     *
     * @param target the {@link SwordEntity} to test
     * @return {@code true} if the target is a {@link Player} in creative or spectator mode
     */
    public static boolean isInvulnerableGameMode(SwordEntity target) {
        if (!(target.self() instanceof Player player)) return false;
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    /**
     * Selects a random available ability from {@link #possibleAbilities} and stores it in
     * {@link #pendingAbility}. If no ability passes {@link MobAbility#canUse(Hostile)},
     * {@code pendingAbility} is set to {@code null}.
     */
    public void selectAbility() {
        List<MobAbility> available = new ArrayList<>();
        for (MobAbility ability : possibleAbilities) {
            if (ability.canUse(this)) {
                available.add(ability);
            }
        }
        if (available.isEmpty()) {
            pendingAbility = null;
            return;
        }
        pendingAbility = available.get(new Random().nextInt(available.size()));
    }

    /**
     * Sets the cooldown for the named ability.
     *
     * @param name  the ability name (from {@link MobAbility#name()})
     * @param ticks the cooldown duration in ticks
     */
    public void setAbilityCooldown(String name, int ticks) {
        abilityCooldowns.put(name, ticks);
    }

    /** Decrements all per-ability cooldown counters and removes entries that reach zero. */
    private void tickAbilityCooldowns() {
        Iterator<Map.Entry<String, Integer>> it = abilityCooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public Location getOrigin() {
        return this.origin.clone();
    }
}
