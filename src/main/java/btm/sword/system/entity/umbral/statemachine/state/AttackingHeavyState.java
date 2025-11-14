package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is performing a heavy attack.
 * <p>
 * In this state, the blade executes a powerful, charged attack with
 * higher damage and impact but longer windup and recovery time.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop idle movement</li>
 *   <li>Execute heavy attack animation</li>
 *   <li>Set display transformation for attack</li>
 * </ul>
 * </p>
 * <p>
 * <b>Exit Actions:</b>
 * <ul>
 *   <li>Clean up attack state</li>
 * </ul>
 * </p>
 * <p>
 * <b>Typical Transitions:</b>
 * <ul>
 *   <li>ATTACKING_HEAVY → WAITING (attack completes)</li>
 *   <li>ATTACKING_HEAVY → STANDBY (attack cancelled)</li>
 * </ul>
 * </p>
 *
 */
public class AttackingHeavyState extends UmbralStateFacade {
    @Override
    public String name() {
        return "ATTACKING_HEAVY";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        blade.endIdleMovement();
        // Heavy attack execution
        blade.performAttack(5.0, true); // range=5, heavy=true
    }

    @Override
    public void onExit(UmbralBlade blade) {
        // Attack cleanup if needed
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor heavy attack animation progress
        // Transition to WAITING when attack completes (handled by callback)
    }
}
