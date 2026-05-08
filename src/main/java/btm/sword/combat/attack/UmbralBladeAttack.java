package btm.sword.combat.attack;

import java.util.HashSet;

import org.bukkit.entity.LivingEntity;

import btm.sword.combat.style.AttackProfile;
import btm.sword.config.section.CombatConfig;
import btm.sword.umbral.UmbralBlade;
import btm.sword.umbral.presentation.BladeDisplay;
import btm.sword.util.entity.HitboxUtil;


/** An {@link ItemDisplayAttack} that moves the UmbralBlade display and uses secant-based hit detection. */
public class UmbralBladeAttack extends ItemDisplayAttack {
    protected UmbralBlade blade;

    /** Full-parameter constructor forwarded to {@link ItemDisplayAttack}. */
    public UmbralBladeAttack(BladeDisplay weaponDisplay, AttackProfile profile,
                             boolean orientWithPitch, boolean displayOnly,
                             int tpDuration, int displaySteps,
                             int attackStepsPerDisplayStep, int attackMilliseconds,
                             double attackStartValue, double attackEndValue) {

        super(weaponDisplay.handle(), profile,
            orientWithPitch, displayOnly,
            tpDuration, displaySteps,
            attackStepsPerDisplayStep, attackMilliseconds,
            attackStartValue, attackEndValue);
    }

    /** Convenience constructor using default display step count from config. */
    public UmbralBladeAttack(BladeDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration) {
        super(weaponDisplay.handle(), profile, orientWithPitch, displayOnly, tpDuration);
    }


    /** Associates an {@link UmbralBlade} with this attack for state callbacks. */
    public UmbralBladeAttack setBlade(UmbralBlade blade) {
        this.blade = blade;
        return this;
    }

    @Override
    protected HashSet<LivingEntity> collectHitEntities() {
        if (origin == null || origin.toVector().isZero() || !origin.isFinite() ||
            weaponDisplay == null || !weaponDisplay.isValid()) {
            return new HashSet<>();
        }

        double secantRadius = CombatConfig.HITBOXES_SECANT_RADIUS;
        double rangeSquared = CombatConfig.UMBRAL_BLADE_ATTACK_RANGE_SQUARED;
        return HitboxUtil.secant(origin, attackLocation, secantRadius,
            entity -> filter.test(entity) && entity.getLocation().distanceSquared(attackLocation) < rangeSquared);
    }
}
