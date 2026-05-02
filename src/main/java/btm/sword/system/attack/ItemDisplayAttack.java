package btm.sword.system.attack;


import org.bukkit.entity.ItemDisplay;

import btm.sword.config.Config;
import btm.sword.control.TimeArbiter;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.utility.Prefab;
import lombok.Setter;

/** An attack that moves an existing {@link ItemDisplay} along the Bezier path rather than using hit particles alone. */
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

    /**
     * Constructs an attack that animates the given display entity along the attack profile path.
     * When complete, the caller is responsible for removing or re-taking control of the display.
     */
    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackProfile profile,
                             boolean orientWithPitch,
                             boolean displayOnly, int tpDuration) {

        super(weaponDisplay.getItemStack(), profile, orientWithPitch);
        this.weaponDisplay = weaponDisplay;
        this.displayOnly = displayOnly;
        this.displaySteps = Config.Display.ATTACK_DISPLAY_STEPS;
        this.attackStepsPerDisplayStep = attackIterations / displaySteps; // Stored for potential future use
        this.tpDuration = tpDuration;
    }

    /** Full-parameter constructor allowing explicit control over display steps, iteration count, and timing. */
    public ItemDisplayAttack(ItemDisplay weaponDisplay, AttackProfile profile,
                             boolean orientWithPitch, boolean displayOnly,
                             int tpDuration, int displaySteps,
                             int attackStepsPerDisplayStep, int attackMilliseconds,
                             double attackStartValue, double attackEndValue) {

        super(weaponDisplay.getItemStack(), profile,
            orientWithPitch, attackMilliseconds,
            displaySteps * attackStepsPerDisplayStep,
            attackStartValue, attackEndValue);

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
        if (curIteration.get() % displaySteps == 0) {
            TimeArbiter.teleportDisplay(weaponDisplay, attackLocation, cur, tpDuration, ItemDisplayAttack.class, 73);
        }
    }

    @Override
    protected void startupLogic() {
        if (ticksSpentMovingToInitialLocation != 0) {
            TimeArbiter.teleportDisplay(
                weaponDisplay,
                origin.clone().add(prev), cur,
                ticksSpentMovingToInitialLocation * 2,
                ItemDisplayAttack.class, 86);
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

    /** Sets how many ticks the display will spend moving to its initial position before the attack begins. */
    public ItemDisplayAttack setInitialMovementTicks(int ticksSpentMovingToInitialLocation) {
        this.ticksSpentMovingToInitialLocation = ticksSpentMovingToInitialLocation;
        return this;
    }

    /** Controls whether hit particles are drawn during the attack sweep. */
    public ItemDisplayAttack setDrawParticles(boolean drawParticles) {
        this.drawParticles = drawParticles;
        return this;
    }

    // TODO: pass in an instance of HitValues
    @Override
    protected void hit() {
        currentTarget.hit(attacker, Prefab.Attacks.UMBRAL_ITEM_DISPLAY_ATTACK,
            attackProfile.knockbackFunction().apply(currentTarget).apply(this));

        Prefab.Particles.TEST_HIT.display(currentTarget.getChestLocation());
    }
}
