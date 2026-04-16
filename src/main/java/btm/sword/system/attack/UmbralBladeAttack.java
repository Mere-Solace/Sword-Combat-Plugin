package btm.sword.system.attack;

import java.util.HashSet;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;

import btm.sword.config.Config;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.utility.entity.HitboxUtil;


/** An {@link ItemDisplayAttack} that moves the UmbralBlade display and uses secant-based hit detection. */
public class UmbralBladeAttack extends ItemDisplayAttack {
    protected UmbralBlade blade;

    /** Full-parameter constructor forwarded to {@link ItemDisplayAttack}. */
    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile,
                             boolean orientWithPitch, boolean displayOnly,
                             int tpDuration, int displaySteps,
                             int attackStepsPerDisplayStep, int attackMilliseconds,
                             double attackStartValue, double attackEndValue) {

        super(weaponDisplay, profile,
            orientWithPitch, displayOnly,
            tpDuration, displaySteps,
            attackStepsPerDisplayStep, attackMilliseconds,
            attackStartValue, attackEndValue);
    }

    /** Convenience constructor using default display step count from config. */
    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration) {
        super(weaponDisplay, profile, orientWithPitch, displayOnly, tpDuration);
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

        double secantRadius = Config.Combat.HITBOXES_SECANT_RADIUS;
        double rangeSquared = Config.Combat.UMBRAL_BLADE_ATTACK_RANGE_SQUARED;
        return HitboxUtil.secant(origin, attackLocation, secantRadius,
            entity -> filter.test(entity) && entity.getLocation().distanceSquared(attackLocation) < rangeSquared);
    }
}
