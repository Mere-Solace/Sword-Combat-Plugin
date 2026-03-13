package btm.sword.system.entity.umbral.statemachine.state;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.GeneratedAttackProfile;
import btm.sword.system.attack.UmbralBladeAttack;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.UmbralBlade.ReclaimType;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.ControlVectors;

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
        if (blade.getReclaimType() == ReclaimType.AERIAL_SPIKE) {
            reclaimAttack(blade, AttackType.AERIAL_SPIKE);
        } else {
            attack(blade, 8);
        }
        blade.getDisplay().setGlowing(true);
        blade.getDisplay().setGlowColorOverride(Config.SwordColor.FEROCIOUS_SWEEP);
    }

    @Override
    public void onExit(UmbralBlade blade) {
        blade.setAttackCompleted(false);
        blade.setReclaimType(ReclaimType.NONE);
        blade.getDisplay().setGlowing(false);
    }

    @Override
    public void onTick(UmbralBlade blade) {

    }

    private static void reclaimAttack(UmbralBlade blade, AttackType type) {
        new UmbralBladeAttack(blade.getDisplay(), type,
            false, false, 0,
            20, 15, 400,
            0, 1)
            .setBlade(blade)
            .setInitialMovementTicks(5)
            .setOnEntityHitInstructions(target -> {
                blade.getThrower().getAspects().soulfire().add(8f);
                Prefab.Particles.BLEED.display(target.getChestLocation());
            })
            .setCallback(blade.getAttackEndCallback(), 200)
            .execute(blade.getThrower());
    }

    private static void attack(UmbralBlade blade, double range) {
        SwordEntity target = blade.getThrower().getTargetedEntity(range);
        Location attackOrigin = blade.getDisplay().getLocation();
        Location targetedLocation;
        Vector toTarget;
        Vector start;
        Vector end;
        Vector c1;
        Vector c2;
        Basis attackFrame;
        double dist;

        double minSweepDistance = 5.0;
        int direction;

        if (target == null || target.isInvalid()) {
            targetedLocation = blade.getThrower().getChestLocation().add(blade.getThrower().dir().multiply(range));
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), attackOrigin, targetedLocation, 0.25);
        } else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), blade.getDisplay().getLocation(), target.getChestLocation(), 0.5);
            targetedLocation = target.getChestLocation();
        }

        toTarget = targetedLocation.toVector().subtract(blade.getDisplay().getLocation().toVector());
        direction = blade.getThrower().rightBasisVector(false).dot(toTarget) > 0 ? -1 : 1;

        attackOrigin.setDirection(toTarget);
        attackFrame = new Basis(attackOrigin, true);

        double calculatedDistance = targetedLocation.toVector().subtract(attackOrigin.toVector()).length();
        double diffBetweenCalculatedDistAndMinDist = calculatedDistance - minSweepDistance;
        if (diffBetweenCalculatedDistAndMinDist < 0) {
            attackOrigin.add(toTarget.normalize().multiply(diffBetweenCalculatedDistAndMinDist));
        }
        dist = Math.max(calculatedDistance, minSweepDistance);

        start = attackFrame.forward().multiply(-dist / 10)
            .add(attackFrame.right().multiply(direction * dist / 5));
        c1 = attackOrigin.clone()
            .add(attackFrame.forward().multiply(dist / 5))
            .add(attackFrame.right().multiply(direction * dist / 3))
            .toVector()
            .subtract(attackOrigin.toVector());
        c2 = targetedLocation.clone()
            .add(attackFrame.forward().multiply(-dist / 5))
            .add(attackFrame.right().multiply(direction * dist / 3))
            .toVector()
            .subtract(attackOrigin.toVector());
        end = targetedLocation.clone()
            .add(attackFrame.forward().multiply(dist / 2))
            .add(attackFrame.right().multiply(-direction * dist / 3))
            .toVector()
            .subtract(attackOrigin.toVector());

        ControlVectors ctrl = new ControlVectors(start, end, c1, c2);
        GeneratedAttackProfile profile = new GeneratedAttackProfile(ctrl, Attack::getTo);

        int duration = 20 * (int) Math.log(Math.max(1, dist * dist));

        Attack attackObj = new UmbralBladeAttack(blade.getDisplay(), profile,
            true, true, 1,
            30, 1, (int) (duration * 1.75),
            0.2, -0.1)
            .setBlade(blade)
            .setInitialMovementTicks(5)
            .setDrawParticles(false)
            .setNextAttack(
                new UmbralBladeAttack(blade.getDisplay(), profile,
                    true, false, 0,
                    (int) (dist * 2), 10, duration / 2,
                    0, 1)
                    .setBlade(blade)
                    .setOnEntityHitInstructions(
                        swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                    .setCallback(blade.getAttackEndCallback(), 200),
                100);

        attackObj.setOriginOfAll(attackOrigin).execute(blade.getThrower());
    }
}
