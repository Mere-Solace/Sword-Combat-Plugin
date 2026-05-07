package btm.sword.umbral.statemachine.state;


import org.bukkit.entity.LivingEntity;

import btm.sword.entity.base.SwordEntity;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is lodged in an entity or block.
 *
 * <p>Entry installs the {@link btm.sword.umbral.motion.drivers.ImpalementFollowDriver} on the
 * blade's motion subsystem via {@code blade.impale(...)} when an impaled entity is present.
 * Knockback damage is applied by the FSM transition action that reaches this state — this
 * {@code onEnter} only sets up the follow behavior.</p>
 *
 * <p>Exit stops the motion driver. Glow / inventory / display cleanup is handled by the FSM
 * recovery conditions and outgoing transition actions.</p>
 */
public class LodgedState extends UmbralStateFacade {

    @Override
    public String name() {
        return "LODGED";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        SwordEntity target = blade.getHitEntity();
        if (target != null && target.self() instanceof LivingEntity le) {
            blade.impale(le);
        }
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getBladeMotion().stop();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Impalement follow runs from the installed motion driver; nothing to do per-tick here.
    }
}
