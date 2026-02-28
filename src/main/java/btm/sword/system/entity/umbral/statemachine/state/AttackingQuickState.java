package btm.sword.system.entity.umbral.statemachine.state;

import org.bukkit.util.Transformation;

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
        // handling it here cuz special case, doesn't work with tree well
        // TODO: is consuming soulfire at this location good?
        blade.getThrower().consumeSoulfire(blade.getCurrentComboStep() * 2.5f);

        if (blade.getCurrentComboStep() == 3) {
            Transformation curTr = blade.getDisplay().getTransformation();

            blade.getDisplay().setTransformation(
                new Transformation(
                    curTr.getTranslation(),
                    curTr.getLeftRotation().rotateY((float) Math.PI / 2),
                    curTr.getScale(),
                    curTr.getRightRotation()
                )
            );
        }

        // Attack execution is handled by performAttack method
        blade.performSimpleAttack(5.0); // second param doesn't matter here

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
