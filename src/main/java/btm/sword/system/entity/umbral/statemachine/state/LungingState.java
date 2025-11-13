package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.statemachine.State;

/**
 * State where the UmbralBlade is lunging toward a target.
 * <p>
 * In this state, the blade rapidly travels toward a targeted entity,
 * typically to impale or strike them. This is a high-mobility attack
 * that closes distance quickly.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop idle movement</li>
 *   <li>Set display transformation for lunge</li>
 *   <li>Begin rapid movement toward target</li>
 *   <li>Enable smooth teleport for visual trail</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Stop lunge movement</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>LUNGING → LODGED (blade hits and embeds in target)</li>
 *   <li>LUNGING → WAITING (lunge completes without lodging)</li>
 *   <li>LUNGING → FLYING (lunge misses, continues flying)</li>
 * </ul>
 * </p>
 *
 */
public class LungingState extends State<UmbralBlade> {
    @Override
    public String name() {
        return "LUNGING";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.setDisplayTransformation(UmbralState.LUNGING);
        blade.endIdleMovement();
        // TODO: Implement lungeToTarget logic when target system is ready
        // blade.lungeToTarget(blade.getLastTargetLocation());
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Stop lunge movement
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor lunge progress
        // Check for collision with target or obstacles
    }
}
