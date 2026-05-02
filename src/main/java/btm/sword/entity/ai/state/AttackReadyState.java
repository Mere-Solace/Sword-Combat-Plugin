package btm.sword.entity.ai.state;

import org.bukkit.Particle;

import btm.sword.config.Config;
import btm.sword.entity.ai.HostileAIFacade;
import btm.sword.entity.mob.Hostile;

/**
 * AttackReady post-attack AI state for Hostile entities.
 *
 * <p>One of three post-attack branches selected by a random roll in {@link AttackState}.
 * The mob pauses in place, telegraphs a follow-up strike with a {@link Particle#CRIT} burst,
 * faces the target continuously, then transitions directly into a new {@code AttackState}
 * without a full pre-attack wind-up — creating a visible combo-pause effect.
 */
public class AttackReadyState extends HostileAIFacade {

    /** Creates an {@code AttackReadyState}. */
    public AttackReadyState() {}

    @Override
    public String name() {
        return "ATTACK_READY";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setAttackReadyTimer(Config.Hostile.ATTACK_READY_TICKS);
        h.getPathfinder().stopPathfinding();
        h.getMob().setAware(true);

        h.self().getWorld().spawnParticle(
            Particle.CRIT,
            h.self().getEyeLocation(),
            10, 0.3, 0.3, 0.3, 0.15
        );

        faceTarget(h);
    }

    @Override
    public void onTick(Hostile h) {
        h.setAttackReadyTimer(h.getAttackReadyTimer() - 1);
        faceTarget(h);
    }

    @Override
    public void onExit(Hostile h) {
        h.setAttackReadyTimer(0);
    }

    private void faceTarget(Hostile h) {
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        h.getMob().lookAt(h.getCurrentTarget().self(), 100f, 100f);
    }
}
