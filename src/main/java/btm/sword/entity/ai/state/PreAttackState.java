package btm.sword.entity.ai.state;

import org.bukkit.Particle;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.config.section.HostileConfig;
import btm.sword.entity.ai.HostileAIFacade;
import btm.sword.entity.ai.MobGoalArbiter;
import btm.sword.entity.ai.ability.AbilityCategory;
import btm.sword.entity.ai.goal.ApproachGoal;
import btm.sword.entity.ai.goal.LookAtTargetGoal;
import btm.sword.entity.ai.goal.PreAttackRetreatGoal;
import btm.sword.entity.mob.Hostile;
import btm.sword.util.prefab.Prefab;

/**
 * Pre-attack (wind-up) AI state for Hostile entities.
 *
 * <p>Selects a {@link btm.sword.entity.ai.ability.MobAbility} and then moves the mob
 * during the telegraph:
 * <ul>
 *   <li>{@link AbilityCategory#MELEE} — mob approaches the target at 110% speed via
 *       {@link ApproachGoal}.</li>
 *   <li>{@link AbilityCategory#RANGED} — mob retreats from the target via
 *       {@link PreAttackRetreatGoal}.</li>
 *   <li>No ability available — mob stops pathfinding.</li>
 * </ul>
 * After the configured wind-up ticks, transitions to {@link AttackState} which fires the ability
 * immediately — no proximity gate.
 */
public class PreAttackState extends HostileAIFacade {

    @Override
    public String name() {
        return "PRE_ATTACK";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setPreAttackTimer(HostileConfig.PRE_ATTACK_TICKS);
        h.selectAbility();

        h.getMob().setAware(true);

        h.self().getWorld().spawnParticle(
            Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
            h.self().getEyeLocation(),
            25, 0.4, 0.4, 0.4, 0
        );

        h.broadcastMessage(10, "Gonna Getcha!");

        Prefab.Sounds.PRE_ATTACK.playForAllInRadius(h.self());

        if (h.getPendingAbility() == null) {
            h.getPathfinder().stopPathfinding();
        } else if (h.getPendingAbility().category() == AbilityCategory.MELEE) {
            MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new ApproachGoal(h.mob(), h));
            MobGoalArbiter.GOALS.addGoal(h.mob(), 2, new LookAtTargetGoal(h.mob(), h));
        } else {
            MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new PreAttackRetreatGoal(h.mob(), h));
            MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new LookAtTargetGoal(h.mob(), h));
        }
    }

    @Override
    public void onTick(Hostile h) {
        h.setPreAttackTimer(h.getPreAttackTimer() - 1);
    }

    @Override
    public void onExit(Hostile h) {
        h.setPreAttackTimer(0);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
