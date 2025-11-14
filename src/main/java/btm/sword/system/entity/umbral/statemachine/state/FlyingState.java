package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is flying through the air.
 * <p>
 * In this state, the blade has been thrown or launched and is traveling
 * through the air following a physics trajectory. It will collide with
 * entities or blocks, transitioning to LODGED or WAITING states.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop idle movement</li>
 *   <li>Set display transformation for flight</li>
 *   <li>Initialize physics trajectory</li>
 *   <li>Enable particle trail</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Stop flight physics</li>
 *   <li>Disable particle trail</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>FLYING → LODGED (blade embeds in entity or block)</li>
 *   <li>FLYING → WAITING (blade lands on ground)</li>
 *   <li>FLYING → RECALLING (wielder recalls mid-flight)</li>
 * </ul>
 * </p>
 *
 */
public class FlyingState extends UmbralStateFacade {
    @Override
    public String name() {
        return "FLYING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        // Flying physics handled by ThrownItem parent class
        // Particle trail and collision detection in tick
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Stop flight physics if needed
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor flight trajectory
        // Check for collisions with entities or blocks
        // Update particle trail
    }
}
