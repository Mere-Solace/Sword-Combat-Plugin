package btm.sword.system.attack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.VectorUtil;
import lombok.Setter;

public class ItemDisplayAttack extends Attack {
    @Setter
    private ItemDisplay weaponDisplay;
    private final boolean displayOnly;

    private final int displaySteps;
    private final int attackStepsPerDisplayStep; // number display steps gets multiplied by
    private final int tpDuration;

    private int ticksSpentMovingToInitialLocation = 0;
    private boolean drawParticles = true;

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
    void swingTest() {
        if (displayOnly) return;
        super.swingTest();
    }

    @Override
    protected void drawAttackEffects() {
        if (drawParticles) super.drawAttackEffects();
        if (curIteration % displaySteps == 0) {
            DisplayUtil.smoothTeleport(weaponDisplay, tpDuration);
            weaponDisplay.teleport(attackLocation);
        }
    }

    @Override
    protected void startAttack() {
        applySelfAttackEffects();
        playSwingSoundEffects();

        double attackRange = attackEndValue - attackStartValue;
        double step = attackRange / attackIterations;
        int msPerIteration = attackMilliseconds / attackIterations;

        generateBezierFunction();

        determineOrigin();

        prev = weaponPathFunction.apply(attackStartValue - step);

        if (ticksSpentMovingToInitialLocation != 0) {
            DisplayUtil.smoothTeleport(weaponDisplay, ticksSpentMovingToInitialLocation*2);
            weaponDisplay.teleport(origin.clone().add(prev));
        }

        curIteration = 0;
        for (int i = 0; i <= attackIterations; i++) {
            final int idx = i;
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    applyConsistentEffects();

                    cur = weaponPathFunction.apply(attackStartValue + (step * idx));
                    attackLocation = origin.clone().add(cur).setDirection(cur);

                    drawAttackEffects();
                    hit();
                    swingTest();

                    // allows for chaining of attack logic
                    if (idx == attackIterations) {
                        if (callback != null) {
                            Bukkit.getScheduler().runTask(Sword.getInstance(), callback);
                        }
                        if (nextAttack != null) {
                            SwordScheduler.runBukkitTaskLater(
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        origin = null;  // important so that consecutive uses of the same origin don't
                                        // all start with the same origin.
                                        nextAttack.execute(attacker);
                                    }
                                }, millisecondDelayBeforeNextAttack, TimeUnit.MILLISECONDS
                            );
                        }
                    }
                    prev = cur;
                    curIteration++;
                }
            }, ticksSpentMovingToInitialLocation + curIteration * msPerIteration,
                TimeUnit.MILLISECONDS);
        }
    }

    @Override
    void generateBezierFunction() {
        ArrayList<Vector> basis = orientWithPitch ?
            VectorUtil.getBasis(origin, origin.getDirection()) :
            VectorUtil.getBasisWithoutPitch(origin);
        curRight = basis.getFirst();
        curUp = basis.get(1);
        curForward = basis.getLast();

        List<Vector> adjusted = BezierUtil.adjustCtrlToBasis(basis, controlVectors, rangeMultiplier);
        weaponPathFunction = BezierUtil.cubicBezier3D(adjusted.get(0), adjusted.get(1), adjusted.get(2), adjusted.get(3));
    }

    public ItemDisplayAttack setInitialMovementTicks(int ticksSpentMovingToInitialLocation) {
        this.ticksSpentMovingToInitialLocation = ticksSpentMovingToInitialLocation;
        return this; // for builder pattern, pretty cool GOF
    }

    public ItemDisplayAttack setDrawParticles(boolean drawParticles) {
        this.drawParticles = drawParticles;
        return this;
    }
}
