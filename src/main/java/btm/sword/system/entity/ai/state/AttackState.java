package btm.sword.system.entity.ai.state;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Particle;

import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.ai.MobGoalArbiter;
import btm.sword.system.entity.ai.goal.LookAtTargetGoal;
import btm.sword.system.entity.display.DisplayRig;
import btm.sword.system.entity.impl.Hostile;
import net.donnypz.displayentityutils.utils.DisplayEntities.machine.MachineState;

/**
 * Attack AI state for Hostile entities.
 *
 * <p>Fires the mob's {@link btm.sword.system.entity.ai.ability.MobAbility#execute pending ability}
 * immediately on entry — no charge, no distance gate. Rolls a random
 * {@code attackPostRoll} (0–2) to select the post-attack branch:
 * <ul>
 *   <li>0 → {@link OnGuardState}</li>
 *   <li>1 → {@link AttackReadyState}</li>
 *   <li>2 → {@link AttackState} (combo re-entry)</li>
 * </ul>
 * A {@link org.bukkit.Particle#CRIT} burst is played on entry when this is a combo re-entry.
 * Entry is a no-op if the mob is currently incapacitated (e.g., grabbed).
 */
public class AttackState extends HostileAIFacade {

    @Override
    public String name() {
        return "ATTACK";
    }

    @Override
    public void onEnter(Hostile h) {
        if (h.isCombo()) {
            h.self().getWorld().spawnParticle(
                Particle.CRIT,
                h.self().getEyeLocation(),
                8, 0.3, 0.3, 0.3, 0.2
            );
            h.setCombo(false);
        }

        if (h.isIncapacitated()) return;

        MobGoalArbiter.GOALS.addGoal(h.mob(), 1, new LookAtTargetGoal(h.mob(), h));

        if (h.getPendingAbility() != null) {
            h.getPendingAbility().execute(h);
            h.setAbilityCooldown(h.getPendingAbility().name(), h.getPendingAbility().cooldownTicks());
        }

        h.setAttackDone(true);
        h.setAttackPostRoll(ThreadLocalRandom.current().nextInt(3));

        DisplayRig rig = h.getDisplayRig();
        if (rig != null) {
            rig.setState(MachineState.StateType.MELEE);
        }
    }

    @Override
    public void onTick(Hostile h) {
        // Attack fires on entry; nothing to do per-tick.
    }

    @Override
    public void onExit(Hostile h) {
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.MOVE);
        MobGoalArbiter.GOALS.removeAllGoals(h.mob(), GoalType.LOOK);
    }
}
