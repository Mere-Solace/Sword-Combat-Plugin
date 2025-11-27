package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is waiting after an attack completes.
 * <p>
 * In this state, the blade hovers in place with idle animations, registered
 * as an interactable item that can be picked up or commanded. If left idle
 * too long or the wielder moves too far, it will automatically return.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Start idle movement animations</li>
 *   <li>Register blade as interactable item</li>
 *   <li>Set display transformation</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Stop idle movement</li>
 *   <li>Unregister from interactable items</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>WAITING → STANDBY (wielder picks it up)</li>
 *   <li>WAITING → RETURNING (auto-return triggered)</li>
 * </ul>
 * </p>
 *
 */
public class WaitingState extends UmbralStateFacade {
    @Override
    public String name() {
        return "WAITING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.registerAsInteractableItem();
        blade.startIdleMovement();
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.unregisterAsInteractableItem();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor distance to wielder and time idle
        // Trigger RETURNING transition if too far or too long
    }
}
