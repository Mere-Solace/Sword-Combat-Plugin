package btm.sword.system.attack;

import btm.sword.config.Config;
import btm.sword.system.attack.style.AttackProfile;

import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.utility.SwordTimeUnit;

import btm.sword.utility.math.VectorUtil;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SweepAttack extends Attack{
    private ItemDisplay sweepTail;
    private ItemDisplay sweepBody;
    private ItemDisplay sweepFront;
    private World world;
    private int tpDuration;

    private Vector tail;
    private double currentTailValue;
    private Vector front;
    private double currentFrontValue;

    private ArrayList<ItemDisplay> sweepDisplays;
    private int iterationsBetweenDisplaySpawn;
    private float xScale;
    private float yScale;
    private float zScale;

    private boolean dynamicNormal;
    private float displayRollRotation;


    public SweepAttack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch) {
        super(itemUsedInAttack, profile, orientWithPitch);
    }

    public SweepAttack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch, int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue) {
        super(itemUsedInAttack, profile, orientWithPitch, attackMilliseconds, attackIterations, attackStartValue, attackEndValue);
    }

    @Override
    protected void startupLogic() {
//        xScale = Config.Combat.SWEEP_ATTACK_X_SCALE;
//        yScale = Config.Combat.SWEEP_ATTACK_Y_SCALE;
//        zScale = Config.Combat.SWEEP_ATTACK_Z_SCALE;
//
//        iterationsBetweenDisplaySpawn = 10;
//
//        dynamicNormal = attackProfile.normalVector() == null;
//        if (!dynamicNormal) {
//            displayRollRotation = (float) VectorUtil.getAngleBetweenTwoVectors(attackProfile.normalVector(), Config.Direction.UP());
//        }
//
//        sweepDisplays = new ArrayList<>(attackIterations);
//
//        tpDuration = Math.max(1, (int) (SwordTimeUnit.millisToTicks(msPerIteration) * 3 / TimeArbiter.getGLOBAL_TIME_SCALE()));
//        world = attacker.world();
//
//        currentTailValue = attackStartValue - (interpolationStep * 3);
//        tail = weaponPathFunction.apply(currentTailValue);
//        sweepTail = (ItemDisplay) world.spawnEntity(origin.clone().add(prev).setDirection(prev), EntityType.ITEM_DISPLAY);
//        basicSlashDisplaySetup(sweepTail);
//
//        Vector body = weaponPathFunction.apply(attackStartValue);
//        sweepBody = (ItemDisplay) world.spawnEntity(origin.clone().add(body).setDirection(body), EntityType.ITEM_DISPLAY);
//        basicSlashDisplaySetup(sweepBody);
//
//        currentFrontValue = attackStartValue + (interpolationStep * 3);
//        front = weaponPathFunction.apply(currentFrontValue);
//        sweepFront = (ItemDisplay) world.spawnEntity(origin.clone().add(front).setDirection(front), EntityType.ITEM_DISPLAY);
//        basicSlashDisplaySetup(sweepFront);

    }

    protected float calcDisplayRotation() {
        if (dynamicNormal) {
            Vector body = cur == null ? weaponPathFunction.apply(attackStartValue) : cur;
            return (float) VectorUtil.getAngleBetweenTwoVectors(
                to == null ? body.subtract(prev) : to,
                Config.Direction.UP()
            );
        }
        return displayRollRotation;
    }

    protected void basicSlashDisplaySetup(ItemDisplay display) {
        displayRollRotation = calcDisplayRotation();

        Transformation transformation = new Transformation(
            new Vector3f(xScale/2, -zScale/2, -zScale/2), // to center the block at the location
            new Quaternionf().rotateZ(displayRollRotation),
            new Vector3f(xScale, yScale, zScale),
            new Quaternionf()
        );
        display.setItemStack(ItemStack.of(Material.REDSTONE_BLOCK));
        display.setTransformation(transformation);
        display.setTeleportDuration(tpDuration);
    }

    @Override
    protected void drawAttackEffects() {
//        super.drawAttackEffects();
//        tail = weaponPathFunction.apply(currentTailValue + interpolationStep);
//        front = weaponPathFunction.apply(currentFrontValue + interpolationStep);
//
//        TimeArbiter.teleportDisplay(sweepTail, origin.clone().add(prev), prev, tpDuration);
//        TimeArbiter.teleportDisplay(sweepBody, attackLocation, cur, tpDuration);
//        TimeArbiter.teleportDisplay(sweepFront, origin.clone().add(front), front, tpDuration);
//
//
//        if (curIteration.get() % iterationsBetweenDisplaySpawn == 0) {
//            ItemDisplay newDisplay = (ItemDisplay) world.spawnEntity(attackLocation.setDirection(cur), EntityType.ITEM_DISPLAY);
//            basicSlashDisplaySetup(newDisplay);
//            sweepDisplays.add(newDisplay);
//        }
    }

    @Override
    protected void endingLogic() {
//        SwordScheduler.runBukkitTaskLater(
//            () -> sweepDisplays.forEach(Entity::remove),
//            500, TimeUnit.MILLISECONDS
//        );
//
//        sweepTail.remove();
//        sweepBody.remove();
//        sweepFront.remove();

        super.endingLogic();
    }
}
