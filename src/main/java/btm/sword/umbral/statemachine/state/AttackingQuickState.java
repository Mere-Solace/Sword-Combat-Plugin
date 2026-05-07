package btm.sword.umbral.statemachine.state;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import btm.sword.action.movement.DashDirection;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.UmbralBladeAttack;
import btm.sword.combat.style.AttackType;
import btm.sword.entity.base.SwordEntity;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.statemachine.UmbralStateFacade;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.prefab.Prefab;

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

    /** Creates an {@code AttackingQuickState}. */
    public AttackingQuickState() {}

    @Override
    public String name() {
        return "ATTACKING_QUICK";
    }

    @Override
    public void onEnter(UmbralBlade blade) {
        // handling it here cuz special case, doesn't work with tree well
        // TODO: is consuming soulfire at this location good?
        // TODO: simply make a different input action with different prereqs instead?
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

        attack(blade, 5.0);

    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setAttackCompleted(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {
    }

    private static void attack(UmbralBlade blade, double range) {
        if (blade.isDashing()) {
            dashAttack(blade, blade.getDashDirection());
            return;
        }
        SwordEntity target = blade.getThrower().getTargetedEntity(range);
        Attack attack;
        Location attackOrigin;

        if (target == null || target.isInvalid()) {
            attackOrigin = blade.getThrower().getChestLocation().clone()
                .add(blade.getThrower().dir().multiply(range));
        } else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), blade.getDisplay().getLocation(), target.getChestLocation(), 0.5);
            Vector to = target.getChestLocation().toVector()
                .subtract(blade.getDisplay().getLocation().toVector());
            attackOrigin = target.getChestLocation().clone()
                .subtract(to.normalize()).setDirection(to.normalize());
        }

        attack = blade.getBasicAttacks()[
            Math.max(0, Math.min(blade.getCurrentComboStep() - 1, blade.getBasicAttacks().length - 1))
        ].apply(blade.getThrower());

        attack.setOriginOfAll(attackOrigin).execute(blade.getThrower());
    }

    private static void dashAttack(UmbralBlade blade, DashDirection direction) {
        AttackType type;
        switch (direction) {
            case FORWARD -> type = AttackType.F_DASH_ATTACK;
            case BACKWARD -> type = AttackType.B_DASH_ATTACK;
            default -> type = AttackType.WIDE_UMBRAL_SLASH3;
        }
        new UmbralBladeAttack(blade.getBladeDisplay(), type,
            direction.equals(DashDirection.FORWARD), false, 1,
            5, 10, 200,
            0, 1)
            .setBlade(blade)
            .setInitialMovementTicks(1)
            .setCallback(blade.getAttackEndCallback(), 200)
            .execute(blade.getThrower());
    }
}
