package btm.sword.system.entity.ai.state;

import btm.sword.system.entity.ai.HostileAIFacade;
import btm.sword.system.entity.impl.Hostile;

/**
 * Attack AI state for Hostile entities.
 * <p>
 * Charges toward the target at full speed. Once within melee distance (2.5 blocks),
 * executes one of the mob's available attacks via {@link Hostile#randomAttack()}.
 * After the attack fires, the mob transitions to {@link RetreatState}.
 * </p>
 */
public class AttackState extends HostileAIFacade {

    /** Melee reach threshold in blocks squared (2.5 blocks). */
    private static final double MELEE_REACH_SQ = 6.25;

    @Override
    public String name() {
        return "ATTACK";
    }

    @Override
    public void onEnter(Hostile h) {
        h.setAttackDone(false);
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        h.getPathfinder().moveTo(h.getCurrentTarget().self(), 1.6);
    }

    @Override
    public void onTick(Hostile h) {
        if (h.isAttackDone()) return;
        if (h.getCurrentTarget() == null || !h.getCurrentTarget().self().isValid()) return;
        if (h.getPossibleAttacks().isEmpty()) return;

        double distSq = h.self().getLocation().distanceSquared(h.getCurrentTarget().self().getLocation());
        if (distSq <= MELEE_REACH_SQ) {
            h.randomAttack();
            h.setAttackDone(true);
        }
    }

    @Override
    public void onExit(Hostile h) {
        h.getPathfinder().stopPathfinding();
    }
}
