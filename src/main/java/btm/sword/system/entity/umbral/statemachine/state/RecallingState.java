package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is being recalled to the wielder.
 * <p>
 * In this state, the blade is traveling back to the player, typically after
 * being thrown, lodged in an enemy, or left in a waiting state.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Set display transformation for returning animation</li>
 *   <li>Stop idle movement</li>
 *   <li>Begin lerp movement back to wielder</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>RECALLING → SHEATHED (when blade arrives)</li>
 * </ul>
 * </p>
 *
 */
public class RecallingState extends UmbralStateFacade {
    @Override
    public String name() {
        return "RECALLING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.returnToWielderAndRequestState(BladeRequest.STANDBY);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Recall animation cleanup if needed
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor recall progress
    }
}
