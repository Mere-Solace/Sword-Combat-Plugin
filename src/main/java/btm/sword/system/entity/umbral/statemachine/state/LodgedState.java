package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.system.statemachine.State;

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
        blade.endIdleMovement();
        // TODO: Attach blade display to target entity/block
        // TODO: Apply impalement effects (bleeding, damage over time)
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // TODO: Detach from target
        // TODO: Remove impalement effects
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor target health/validity
        // Apply damage over time if lodged in entity
        // Transition to WAITING if target is destroyed
    }
}
