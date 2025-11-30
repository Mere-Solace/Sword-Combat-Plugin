package btm.sword.system.attack;


import org.bukkit.entity.ItemDisplay;

import btm.sword.system.attack.style.AttackProfile;
import btm.sword.util.Prefab;
import btm.sword.util.display.DisplayUtil;
import lombok.Setter;

public class ItemDisplayAttack extends Attack {
    @Setter
    protected ItemDisplay weaponDisplay;
    private final boolean displayOnly;

    private final int displaySteps;
    @SuppressWarnings("unused") // Calculated for symmetry; used in overloaded constructor
    private final int attackStepsPerDisplayStep; // number display steps gets multiplied by
    private final int tpDuration;

    private int ticksSpentMovingToInitialLocation = 0;
    private boolean drawParticles = true;

    // Takes in an already created weapon display and changes it's position around.
    // once the attack is done, the display should either be removed or control of
    // its movement should be handed back to previous controller.
    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch,
                             boolean displayOnly, int tpDuration) {
        super(profile, orientWithPitch);
        this.weaponDisplay = weaponDisplay;
        this.displayOnly = displayOnly;
        this.displaySteps = 10; //TODO config pls
        this.attackStepsPerDisplayStep = attackIterations / displaySteps; // Stored for potential future use
        this.tpDuration = tpDuration;
    }

    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackProfile profile, boolean orientWithPitch,
                             boolean displayOnly, int tpDuration, int displaySteps, int attackStepsPerDisplayStep,
                             int attackMilliseconds, double attackStartValue, double attackEndValue) {
        super(profile, orientWithPitch, attackMilliseconds, displaySteps * attackStepsPerDisplayStep, attackStartValue, attackEndValue);
        this.weaponDisplay = weaponDisplay;
        this.displayOnly = displayOnly;
        this.displaySteps = displaySteps;
        this.attackStepsPerDisplayStep = attackStepsPerDisplayStep;
        this.tpDuration = tpDuration;
    }

    @Override
    protected void performHitLogic() {
        if (displayOnly) return;
        super.performHitLogic();
    }

    @Override
    void swingTest() {
        if (displayOnly) return;
        super.swingTest();
    }

    @Override
    protected void drawAttackEffects() {
        if (drawParticles) super.drawAttackEffects();
        if (curIteration % displaySteps == 0) {
            DisplayUtil.smoothTeleport(weaponDisplay, tpDuration);
            weaponDisplay.teleport(attackLocation.setDirection(cur));
        }
    }

    @Override
    protected void startupLogic() {


        if (ticksSpentMovingToInitialLocation != 0) {
            DisplayUtil.smoothTeleport(weaponDisplay, ticksSpentMovingToInitialLocation * 2);
            weaponDisplay.teleport(origin.clone().add(prev));
        }
    }

    @Override
    protected void endingLogic() {
        origin = null;
    }

    @Override
    protected int calcIterationStartDelay(int i, int msPerIteration) {
        return ticksSpentMovingToInitialLocation + (i * msPerIteration);
    }

    public ItemDisplayAttack setInitialMovementTicks(int ticksSpentMovingToInitialLocation) {
        this.ticksSpentMovingToInitialLocation = ticksSpentMovingToInitialLocation;
        return this; // for builder pattern, pretty cool GOF
    }

    public ItemDisplayAttack setDrawParticles(boolean drawParticles) {
        this.drawParticles = drawParticles;
        return this;
    }

    // TODO: pass in an instance of HitValues
    @Override
    protected void hit() {
        currentTarget.hit(attacker, Prefab.Attacks.umbralItemDisplayAttack,
            attackProfile.knockbackFunction().apply(this));

        Prefab.Particles.TEST_HIT.display(currentTarget.getChestLocation());
    }
}
