package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.config.Config;
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
 *   <li>Clean c1 attack state</li>
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
        blade.performTargetedAttack(10);
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.FEROCIOUS_SWEEP);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setAttackCompleted(false);
        blade.getDisplay().setGlowing(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }
}
