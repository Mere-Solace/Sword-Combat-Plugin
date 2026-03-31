package btm.sword.system.entity.ai.ability;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.system.entity.impl.ThrowPhase;

/**
 * A ranged throwing ability for hostile mobs.
 *
 * <p>Launches the mob's main-hand item as a projectile with a parabolic arc toward the
 * current target. The arc height and cooldown are controlled via {@link Config.Hostile}.
 *
 * <p>If the mob has nothing in its main hand, it performs a dirt-scoop animation:
 * looks down, punches the ground, picks up a dirt block, and throws it instead.
 *
 * <p>When the thrown item lands, the mob's {@code lodgedThrowItem} is set so the
 * {@link btm.sword.system.entity.ai.state.RetrieveWeaponState} can drive retrieval.
 *
 * <p>Ability category: {@link AbilityCategory#RANGED}.
 */
public class MobThrowAbility implements MobAbility {

    @Override
    public String name() {
        return "mob_throw";
    }

    @Override
    public AbilityCategory category() {
        return AbilityCategory.RANGED;
    }

    /**
     * Returns {@code true} if the ability is off cooldown and passes the
     * {@link Config.Hostile#MOB_THROW_WEIGHT} probability roll, making throws rarer
     * than melee when both abilities are available.
     *
     * @param h the hostile mob
     * @return {@code true} if the throw can be selected this pre-attack phase
     */
    @Override
    public boolean canUse(Hostile h) {
        if (h.getAbilityCooldown(name()) > 0) return false;
        return ThreadLocalRandom.current().nextDouble() < Config.Hostile.MOB_THROW_WEIGHT;
    }

    @Override
    public void execute(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        // Guard: combo re-entry (roll=2) re-calls execute() before the first throw's delayed
        // throwItem() fires. Without this check, a second ThrownItem is created and the first
        // one's display + landing marker are orphaned and never cleaned up.
        if (h.getThrowPhase() == ThrowPhase.THROWING) return;

        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new LookAtTargetGoal(h.mob(), h));

        Vector toTarget = h.getCurrentTarget().self().getLocation()
            .subtract(h.self().getLocation())
            .toVector();

        double distance = toTarget.length();
        if (distance < 0.001) return;
        toTarget.normalize();

        ItemStack projectile = h.getThrowableItem();
        if (projectile == null || projectile.isEmpty()) {
            executeDirtThrow(h, toTarget);
            return;
        }

        ThrowAction.throwReady(h, projectile);
        h.onWeaponThrown();

        if (h.getThrownItem() != null) {
            h.getThrownItem().setLaunchDirection(toTarget);
            // #224: when the weapon lands, flag it for retrieval
            h.getThrownItem().setOnGroundCallback(() -> h.setLodgedThrowItem(h.getThrownItem()));
        }

        SwordScheduler.runBukkitTaskLater(
            () -> ThrowAction.throwItem(h),
            400,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Fallback throw sequence for when the mob has no item in its main hand.
     *
     * <p>The mob looks down, punches the ground (block-break particles), then after a short
     * delay stands up, takes a dirt block, and throws it at the current target.
     *
     * @param h        the hostile mob performing the sequence
     * @param toTarget pre-normalized direction vector toward the current target
     */
    private void executeDirtThrow(Hostile h, Vector toTarget) {
        // Prevent re-entry during the animation window
        h.setThrowPhase(ThrowPhase.THROWING);

        float yaw = h.mob().getLocation().getYaw();
        h.mob().setRotation(yaw, 70f);

        h.self().getWorld().spawnParticle(
            Particle.BLOCK,
            h.self().getLocation().add(0, 0.2, 0),
            30, 0.3, 0.1, 0.3, 0,
            Material.DIRT.createBlockData()
        );

        SwordScheduler.runBukkitTaskLater(() -> {
            if (!h.self().isValid() || h.getCurrentTarget() == null
                    || !h.getCurrentTarget().self().isValid()) {
                h.setThrowPhase(ThrowPhase.IDLE);
                return;
            }

            h.mob().setRotation(yaw, 0f);
            ItemStack dirt = ItemStack.of(Material.DIRT);
            h.setItemStackInHand(dirt, true);

            ThrowAction.throwReady(h, dirt);

            if (h.getThrownItem() != null) {
                h.getThrownItem().setLaunchDirection(toTarget);
            }

            SwordScheduler.runBukkitTaskLater(
                () -> ThrowAction.throwItem(h),
                400,
                TimeUnit.MILLISECONDS
            );
        }, 300, TimeUnit.MILLISECONDS);
    }

    @Override
    public int cooldownTicks() {
        return Config.Hostile.MOB_THROW_COOLDOWN_TICKS;
    }
}
