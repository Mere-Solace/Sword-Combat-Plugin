package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;

/**
 * State where the UmbralBlade is performing a quick attack.
 * <p>
 * In this state, the blade executes a fast, light attack animation with
 * lower damage but shorter recovery time. The attack is typically triggered
 * when the wielder performs a basic attack input.
 * </p>
 * <p>
 * <b>Entry Actions:</b>
 * <ul>
 *   <li>Stop idle movement</li>
 *   <li>Execute quick attack animation</li>
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
 *   <li>ATTACKING_QUICK → WAITING (attack completes)</li>
 *   <li>ATTACKING_QUICK → STANDBY (attack cancelled)</li>
 * </ul>
 * </p>
 *
 */
public class AttackingQuickState extends UmbralStateFacade {
    @Override
    public String name() {
        return "ATTACKING_QUICK";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        // Attack execution is handled by performAttack method
        blade.performAttack(5.0, false);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setAttackCompleted(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {
        // Monitor attack animation progress
        // Transition to WAITING when attack completes (handled by callback)
    }
}
