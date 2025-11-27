package btm.sword.system.attack;

import java.util.HashSet;
import java.util.List;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.util.entity.HitboxUtil;

// will Pierce through all enemies and then stick out of the last one. Must Forward Calculate, then... Maybe just stick out of the first one hit
// Can have an attack that just rips through everything at some point.
public class ImpalingUmbralBladeAttack extends UmbralBladeAttack {

    // TODO: #122 - Might just handle this with the throwing mechanics of the Thrown Item class that Umbral Blade inherits from

    public ImpalingUmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration, int displaySteps, int attackStepsPerDisplayStep, int attackMilliseconds, double attackStartValue, double attackEndValue) {
        super(weaponDisplay, profile, orientWithPitch, displayOnly, tpDuration, displaySteps, attackStepsPerDisplayStep, attackMilliseconds, attackStartValue, attackEndValue);
    }

    public ImpalingUmbralBladeAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch, boolean displayOnly, int tpDuration) {
        super(weaponDisplay, profile, orientWithPitch, displayOnly, tpDuration);
    }

    @Override
    protected HashSet<LivingEntity> collectHitEntities() {
        Vector checktor = to.clone().normalize(); // check + vector = checktor
        if (checktor.isZero()) {
            return new HashSet<>();
        }

        LivingEntity hit = (LivingEntity) HitboxUtil.ray(attackLocation.clone(), checktor, 2, 0.75, filter);

        if (hit == null) return new HashSet<>();

        return new HashSet<>(List.of(hit));
    }

    @Override
    protected void applyHitEffects(HashSet<LivingEntity> targets) {
        if (!hitDuringAttack.isEmpty()) {
            finishedOrCanceled = true;
            return;
        }

        LivingEntity hit = targets.stream().iterator().next();
        SwordEntity impaled = SwordEntityArbiter.getOrAdd(hit.getUniqueId());

        blade.setHitEntity(impaled);
        blade.request(BladeRequest.IMPALE);
    }
}
