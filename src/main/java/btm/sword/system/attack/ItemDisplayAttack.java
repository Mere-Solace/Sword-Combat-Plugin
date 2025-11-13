package btm.sword.system.attack;

import org.bukkit.entity.ItemDisplay;

import btm.sword.util.display.DisplayUtil;
import lombok.Setter;

public class ItemDisplayAttack extends Attack {
    @Setter
    private ItemDisplay weaponDisplay;
    private final boolean displayOnly;

    private final int displaySteps;
    private final int attackStepsPerDisplayStep; // number display steps gets multiplied by
    private final int tpDuration;

    // Takes in an already created weapon display and changes it's position around.
    // once the attack is done, the display should either be removed or control of
    // its movement should be handed back to previous controller.
    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackType type, boolean orientWithPitch, Runnable callback,
                             boolean displayOnly, int tpDuration) {
        super(type, orientWithPitch, callback);
        this.weaponDisplay = weaponDisplay;
        this.displayOnly = displayOnly;
        this.displaySteps = 10; //TODO config pls
        this.attackStepsPerDisplayStep = attackIterations / displaySteps;
        this.tpDuration = tpDuration;
    }

    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackType type, boolean orientWithPitch, Runnable callback,
                             boolean displayOnly, int tpDuration, int displaySteps, int attackStepsPerDisplayStep,
                             int attackMilliseconds, double attackStartValue, double attackEndValue) {
        super(type, orientWithPitch, callback, attackMilliseconds, displaySteps * attackStepsPerDisplayStep, attackStartValue, attackEndValue);
        this.weaponDisplay = weaponDisplay;
        this.displayOnly = displayOnly;
        this.displaySteps = displaySteps;
        this.attackStepsPerDisplayStep = attackStepsPerDisplayStep;
        this.tpDuration = tpDuration;
    }

    @Override
    protected void hit() {
        if (displayOnly) return;
        super.hit();
    }

    @Override
    protected void drawAttackEffects() {
        super.drawAttackEffects();
        if (curIteration % displaySteps == 0) {
            DisplayUtil.smoothTeleport(weaponDisplay, tpDuration);
            weaponDisplay.teleport(attackLocation);
        }
    }
}
