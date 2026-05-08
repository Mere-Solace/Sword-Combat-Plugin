package btm.sword.entity.ai.state;

import org.bukkit.Particle;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.section.HostileConfig;
import btm.sword.entity.ai.HostileAIFacade;
import btm.sword.entity.ai.MobGoalArbiter;
import btm.sword.entity.ai.goal.LookAtTargetGoal;
import btm.sword.entity.ai.goal.OnGuardBackoffGoal;
import btm.sword.entity.mob.Hostile;

/**
 * OnGuard post-attack AI state for Hostile entities.
 *
 * <p>One of three post-attack branches selected by a random roll in {@link AttackState}.
 * The mob backs off to a safe orbit distance, strafes laterally around the player via
 * {@link OnGuardBackoffGoal}, and faces the player throughout via {@link LookAtTargetGoal}.
 * After {@code ON_GUARD_TICKS} the mob advances back into {@link PreAttackState}
 * (target still present) or returns to {@link IdleState} (target gone).
 */
public class OnGuardState extends HostileAIFacade {

    @Override
    public String name() {
        return "ON_GUARD";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setOnGuardTimer(HostileConfig.ON_GUARD_TICKS);
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;

        h.self().getWorld().spawnParticle(
            Particle.SMOKE,
            h.self().getEyeLocation(),
            12, 0.3, 0.3, 0.3, 0.02
        );

        MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new OnGuardBackoffGoal(h.mob(), h));
        MobGoalArbiter.GOALS.addGoal(h.mob(), 3, new LookAtTargetGoal(h.mob(), h));
    }

    @Override
    public void onTick(Hostile h) {
        h.setOnGuardTimer(h.getOnGuardTimer() - 1);
    }

    @Override
    public void onExit(Hostile h) {
        h.setOnGuardTimer(0);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
