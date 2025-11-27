package btm.sword.system.entity.umbral.statemachine.state;


import btm.sword.config.Config;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is lodged in an entity or block.
 * <p>
 * In this state, the blade is stuck in a target (enemy entity or solid block)
 * after a throw, lunge, or attack. It remains attached until recalled by the
 * wielder or until the target dies/is destroyed.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop all movement</li>
 *   <li>Set display transformation for lodged position</li>
 *   <li>Attach display to target entity/block</li>
 *   <li>Apply impalement effects (damage over time, if applicable)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Detach from target</li>
 *   <li>Remove impalement effects</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>LODGED → RECALLING (wielder recalls the blade)</li>
 *   <li>LODGED → WAITING (target dies or block destroyed)</li>
 * </ul>
 * </p>
 *
 */
public class LodgedState extends UmbralStateFacade {
    @Override
    public String name() {
        return "LODGED";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.UMBRAL_GLOW);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.getDisplay().setGlowing(false);
        blade.cleanupBeforeNewThrow();
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
