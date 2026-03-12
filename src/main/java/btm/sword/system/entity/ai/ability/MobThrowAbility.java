package btm.sword.system.entity.ai.ability;

import java.util.concurrent.TimeUnit;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.impl.Hostile;

/**
 * A ranged throwing ability for hostile mobs.
 *
 * <p>Launches the mob's off-hand item as a projectile with a parabolic arc toward the
 * current target. The arc height and cooldown are controlled via {@link Config.Hostile}.
 * Ability category: {@link AbilityCategory#RANGED}.
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

    @Override
    public boolean canUse(Hostile h) {
        Integer cooldown = h.getAbilityCooldowns().get(name());
        return cooldown == null || cooldown <= 0;
    }

    @Override
    public void execute(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        // Guard: combo re-entry (roll=2) re-calls execute() before the first throw's delayed
        // throwItem() fires. Without this check, a second ThrownItem is created and the first
        // one's display + landing marker are orphaned and never cleaned up.
        if (h.isAttemptingThrow()) return;

        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new LookAtTargetGoal(h.mob(), h));

        // Bug 1 fix: use the explicit item so ThrowAction uses it instead of inferring from hand
        ItemStack projectile = h.getItemStackInHand(true);
        if (projectile == null || projectile.isEmpty()) return;

        Vector toTarget = h.getCurrentTarget().self().getLocation()
            .subtract(h.self().getLocation())
            .toVector();

        double distance = toTarget.length();
        if (distance < 0.001) return;

        toTarget.normalize();
//        // Add vertical arc so the projectile curves up and lands on the target
//        toTarget.setY(toTarget.getY() + Config.Hostile.MOB_THROW_ARC_HEIGHT * distance / 8.0);

        // Bug 1 fix: pass the explicit off-hand item so ThrowAction uses it instead of main-hand
        ThrowAction.throwReady(h, projectile);

        // Bug 2 fix: apply the arc direction to the ThrownItem before it is released
        if (h.getThrownItem() != null) {
            h.getThrownItem().setLaunchDirection(toTarget);
        }

        SwordScheduler.runBukkitTaskLater(
            () -> ThrowAction.throwItem(h),
            400,
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public int cooldownTicks() {
        return Config.Hostile.MOB_THROW_COOLDOWN_TICKS;
    }
}
