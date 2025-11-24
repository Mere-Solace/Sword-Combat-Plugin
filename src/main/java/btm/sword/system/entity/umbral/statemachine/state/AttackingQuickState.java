package btm.sword.system.entity.umbral.statemachine.state;

import btm.sword.config.Config;
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
 *   <li>Clean c1 attack state</li>
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
        // handling it here cuz special case, doesn't work with tree well
        blade.getThrower().consumeSoulfire(10f);

        // Attack execution is handled by performAttack method
        blade.performAttack(5.0, false);

        // TODO: #121 - Potentially add per state glow changes or just a method for this
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.UMBRAL_GLOW);
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
