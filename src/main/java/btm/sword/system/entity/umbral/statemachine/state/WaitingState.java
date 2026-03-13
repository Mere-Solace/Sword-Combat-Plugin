package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.config.Config;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade hovers at a fixed world position after being parked.
 * <p>
 * In this state, the blade bobs in place with idle animations and is registered as
 * an interactable item. If left idle too long or the wielder moves too far, it
 * automatically requests a recall.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Restart idle movement so the blade bobs at its current world position</li>
 *   <li>Register blade as interactable item</li>
 *   <li>Record entry time for timeout tracking</li>
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
 *   <li>WAITING → RECALLING (auto-return when idle too long or player too far)</li>
 *   <li>WAITING → STANDBY (wielder dash-grabs the blade)</li>
 * </ul>
 * </p>
 *
 */
public class WaitingState extends UmbralStateFacade {
    private long entryTime;

    @Override
    public String name() {
        return "WAITING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.startIdleMovement();
        blade.registerAsInteractableItem();
        entryTime = System.currentTimeMillis();
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.endIdleMovement();
        blade.unregisterAsInteractableItem();
    }

    @Override
    public void onTick(UmbralBlade blade) {
        long elapsed = System.currentTimeMillis() - entryTime;
        if (elapsed > Config.UmbralBlade.WAITING_TIMEOUT_MS) {
            blade.request(BladeRequest.RECALL);
            return;
        }

        if (blade.getDisplay() == null || !blade.getDisplay().isValid()) return;

        double maxDist = Config.UmbralBlade.WAITING_MAX_DISTANCE;
        double distSq = blade.getThrower().self().getLocation()
            .distanceSquared(blade.getDisplay().getLocation());
        if (distSq > maxDist * maxDist) {
            blade.request(BladeRequest.RECALL);
        }
    }
}
