package btm.sword.system.attack;

import org.bukkit.entity.ItemDisplay;

import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;

public class UmbralBladeAttack extends ItemDisplayAttack {
    private UmbralBlade blade;

    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackType type, boolean orientWithPitch, boolean displayOnly, int tpDuration, int displaySteps, int attackStepsPerDisplayStep, int attackMilliseconds, double attackStartValue, double attackEndValue) {
        super(weaponDisplay, type, orientWithPitch, displayOnly, tpDuration, displaySteps, attackStepsPerDisplayStep, attackMilliseconds, attackStartValue, attackEndValue);
    }

    public UmbralBladeAttack(ItemDisplay weaponDisplay, AttackType type, boolean orientWithPitch, boolean displayOnly, int tpDuration) {
        super(weaponDisplay, type, orientWithPitch, displayOnly, tpDuration);
    }


    public UmbralBladeAttack setBlade(UmbralBlade blade) {
        this.blade = blade;
        return this;
    }

    @Override
    protected void drawAttackEffects() {
        super.drawAttackEffects();

        blade.getDisplay().setTransformation(blade.getStateDisplayTransformation(AttackingQuickState.class));
    }
}
