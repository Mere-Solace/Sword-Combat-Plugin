package btm.sword.system.entity.ai.state;


import org.bukkit.Particle;

import btm.sword.config.Config;
import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;
import btm.sword.utility.Prefab;

/**
 * Pre-attack (wind-up) AI state for Hostile entities.
 * <p>
 * Stops the mob in place, plays ominous visual and audio cues, then after the
 * configured wind-up period fires the {@link AttackState} transition. While winding up,
 * the mob continuously faces its target.
 * </p>
 */
public class PreAttackState extends HostileAIFacade {

    @Override
    public String name() {
        return "PRE_ATTACK";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setPreAttackTimer(Config.Hostile.PRE_ATTACK_TICKS);
        h.getPathfinder().stopPathfinding();

        h.self().getWorld().spawnParticle(
            Particle.TRIAL_SPAWNER_DETECTION_OMINOUS,
            h.self().getEyeLocation(),
            25, 0.4, 0.4, 0.4, 0
        );

        h.broadcastMessage(10, "Gonna Getcha!");

        Prefab.Sounds.PRE_ATTACK.playForAllInRadius(h.self());

        faceTarget(h);
    }

    @Override
    public void onTick(Hostile h) {
        h.setPreAttackTimer(h.getPreAttackTimer() - 1);
        faceTarget(h);
    }

    @Override
    public void onExit(Hostile h) {
        h.setPreAttackTimer(0);
    }

    private void faceTarget(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        h.getMob().lookAt(h.getCurrentTarget().self(), 100f, 100f);
    }
}
