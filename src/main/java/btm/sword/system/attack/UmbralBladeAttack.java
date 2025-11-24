package btm.sword.system.attack;

import java.util.HashSet;
import java.util.List;

import btm.sword.config.Config;
import btm.sword.util.Prefab;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.entity.HitboxUtil;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;

import btm.sword.system.entity.umbral.UmbralBlade;

import org.bukkit.util.Vector;

public class UmbralBladeAttack extends ItemDisplayAttack {
    protected UmbralBlade blade;

    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration, int displaySteps, int attackStepsPerDisplayStep, int attackMilliseconds, double attackStartValue, double attackEndValue) {
        super(weaponDisplay, profile, orientWithPitch, displayOnly, tpDuration, displaySteps, attackStepsPerDisplayStep, attackMilliseconds, attackStartValue, attackEndValue);
    }

    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration) {
        super(weaponDisplay, profile, orientWithPitch, displayOnly, tpDuration);
    }


    public UmbralBladeAttack setBlade(UmbralBlade blade) {
        this.blade = blade;
        return this;
    }

    @Override
    protected void drawAttackEffects() {
        super.drawAttackEffects();
    }

    @Override
    protected HashSet<LivingEntity> collectHitEntities() {
        if (origin == null || origin.toVector().isZero() || !origin.isFinite() ||
            weaponDisplay == null || !weaponDisplay.isValid()) {
            return new HashSet<>();
        }

        double secantRadius = Config.Combat.HITBOXES_SECANT_RADIUS;
        return HitboxUtil.secant(origin, attackLocation, secantRadius,
            entity -> filter.test(entity) && entity.getLocation().distanceSquared(attackLocation) < 20);
    }
}
