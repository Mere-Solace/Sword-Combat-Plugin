package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is automatically returning to sheath.
 * <p>
 * Similar to RECALLING but triggered automatically when the blade has been
 * idle for too long or the wielder moves too far away.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Set display transformation for return animation</li>
 *   <li>Stop idle movement</li>
 *   <li>Begin automatic return to sheath</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>RETURNING → SHEATHED (when return completes)</li>
 * </ul>
 * </p>
 *
 */
public class ReturningState extends UmbralStateFacade {
    @Override
    public String name() {
        return "RETURNING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.returnToWielderAndRequestState(BladeRequest.STANDBY);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Return animation cleanup if needed
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor return progress
    }
}
