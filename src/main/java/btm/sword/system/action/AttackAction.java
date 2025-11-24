package btm.sword.system.action;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.AttackType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.util.Prefab;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;


/**
 * Provides attack-related actions for {@link Combatant} entities.
 * <p>
 * Supports basic melee attacks, including grounded and aerial variations.
 * Handles attack execution, hit detection, damage application, particle effects,
 * knockback, and associated cooldowns.
 */
public class AttackAction extends SwordAction {
    /** Mapping from item suffixes to corresponding attack handlers. */
    private static final Map<String, TriConsumer<Combatant, AttackType, Boolean>> attackMap = Map.of(
            "_SWORD", AttackAction::basicSlash,
            "_SHOVEL", AttackAction::basicSlash,
            "_AXE", AttackAction::basicSlash,
            "SHIELD", AttackAction::basicSlash
    );

    /**
     * Executes a basic attack for the given {@link Combatant} and {@link AttackType}.
     * <p>
     * Selects the correct attack variant based on the item in hand and whether the
     * executor is grounded or airborne. Aerial attacks reset the executor's combo tree.
     *
     * @param executor The combatant performing the attack.
     * @param type The type of attack being performed.
     */
    public static void basicAttack(Combatant executor, AttackType type, boolean orientWithPitch) {
        ItemStack itemStack = executor.getItemStackInHand(true);
        Material itemType = itemStack.getType();

        executor.message("> Basic Attack Method Call.");

        if (executor.getItemStackInHand(true).isEmpty()) {
            throwPunch(executor, -1);
            return;
        }

        if (executor.holdingSoulLink()) {
            // TODO: make attack go in different directions randomly. Implement in Quick attack class.
            // If they have enough SF and in the Standby State, do a quick slash attack
            if (executor.getAspects().soulfireCur() >= 10f &&
                executor.getUmbralBlade().inState(StandbyState.class)) {

                executor.getUmbralBlade().request(BladeRequest.ATTACK_QUICK);

                if (executor instanceof SwordPlayer swordPlayer) {
                    swordPlayer.resetTree();
                }
                return;
            }

            throwPunch(executor, -1);
            return;
        }

        double dot = executor.entity().getEyeLocation().getDirection().dot(Config.Direction.UP());

        if (executor.isGrounded()) {
            for (var entry : attackMap.entrySet()) {
                if (itemType.name().endsWith(entry.getKey())) {
                    entry.getValue().accept(executor, type, orientWithPitch);
                    return;
                }
            }
        }
        else {
            ((SwordPlayer) executor).resetTree(); // can't combo aerials

            AttackType attackType = AttackType.N_AIR;
            double downAirThreshold = Config.Combat.ATTACKS_DOWN_AIR_THRESHOLD;
            if (dot < downAirThreshold) {
                executor.message("Down Air!"); // TODO: remove after testing.
                attackType = AttackType.D_AIR;
            }

            for (var entry : attackMap.entrySet()) {
                if (itemType.name().endsWith(entry.getKey())) {
                                                                    // with pitch?
                    entry.getValue().accept(executor, attackType, attackType != AttackType.D_AIR);
                    return;
                }
            }
        }
    }

    public static void basicSlash(Combatant executor, AttackType type, Boolean orientWithPitch) {
        new Attack(type, orientWithPitch, 40, 60, 0.1, 0.9).execute(executor);
    }

    public static void punch(Combatant executor, boolean right, double distance) {
        double dist = distance < 0 ? 2.5 : distance;
        double spacing = 0.33;

        Prefab.Sounds.PUNCH_ATTEMPT.play(executor.entity());

        Basis basis = VectorUtil.getBasis(executor.entity().getEyeLocation(), executor.getEyeDirection());

        Location originLoc = executor.getChestLocation()
            .add(right ? basis.right().multiply(spacing) : basis.right().multiply(-spacing));

        Prefab.Particles.PUNCH.display(originLoc.clone().add(basis.forward().multiply(dist)));

        Entity hit = HitboxUtil.ray(originLoc, executor.getEyeDirection(), dist, 1.2,
            entity -> entity instanceof LivingEntity l &&
                l.getUniqueId() != executor.getUniqueId() &&
                SwordEntityArbiter.getOrAdd(l.getUniqueId()) != null);

        if (hit != null) {
            SwordEntity swordHit = SwordEntityArbiter.getOrAdd(hit.getUniqueId());
            if (swordHit != null) {
                Prefab.Sounds.PUNCH_CONNECT.play(executor.entity());
                swordHit.hit(executor, Prefab.Attacks.punch, executor.getEyeDirection());
            }
        }
    }

    public static void throwPunch(Combatant executor, double distance) {
        executor.setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) executor.calcValueReductive(AspectType.FINESSE,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);
        executor.setDurationOfLastAttack(cooldown * Config.Combat.ATTACKS_DURATION_MULTIPLIER);

        cast(executor, cooldown,
            () -> punch(executor, ThreadLocalRandom.current().nextBoolean(), distance)
        );
    }
}
