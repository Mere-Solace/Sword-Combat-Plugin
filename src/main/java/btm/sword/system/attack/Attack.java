package btm.sword.system.attack;

import btm.sword.config.ConfigManager;
import btm.sword.config.section.CombatConfig;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.SwordAction;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.ColorUtil;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.Prefab;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.VectorUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Attack extends SwordAction implements Runnable {
    private Combatant attacker;
    private LivingEntity a;
    private final CombatConfig.AttackClassConfig attackConfig;

    private final boolean orientWithPitch;

    private ItemDisplay weaponDisplay;

    private final List<Vector> controlVectors;
    private Function<Double, Vector> weaponPathFunction;

    private Vector curRight;
    private Vector curUp;
    private Vector curForward;

    private Vector cur;

    private Predicate<LivingEntity> filter;

    private int windupMilliseconds;
    private int windupIterations;
    private double windupStartValue;
    private double windupEndValue;

    private int attackMilliseconds;
    private int attackIterations;
    private double attackStartValue;
    private double attackEndValue;

    int recoverMilliseconds;

    private final float displayOffsetX;
    private final float displayOffsetY;
    private final float displayOffsetZ;
    private final float displayScaleX;
    private final float displayScaleY;
    private final float displayScaleZ;
    private final float displayRotationY;
    private final float displayRotationZ;
    private final float displayRotationX;

    private final Color displayGlowColor;
    private final Color attackGlowColor;

    private final int displaySmoothSteps;

    private final int windupSlownessAmplifier;
    private final int attackSlownessAmplifier;

    private final double rangeMultiplier;

    private boolean cancelled;
    private boolean finished;

    public Attack (AttackType type, boolean orientWithPitch, Predicate<LivingEntity> filter) {
        controlVectors = getAttackVectors(type);
        this.orientWithPitch = orientWithPitch;
        this.filter = filter;
        attackConfig = ConfigManager.getInstance().getCombat().getAttackClass();

        this.windupMilliseconds = attackConfig.getTiming().getWindupDuration();
        this.windupIterations = attackConfig.getTiming().getWindupIterations();
        this.windupStartValue = attackConfig.getTiming().getWindupStartValue();
        this.windupEndValue = attackConfig.getTiming().getWindupEndValue();

        this.attackMilliseconds = attackConfig.getTiming().getAttackDuration();
        this.attackIterations = attackConfig.getTiming().getAttackIterations();
        this.attackStartValue = attackConfig.getTiming().getAttackStartValue();
        this.attackEndValue = attackConfig.getTiming().getAttackEndValue();

        this.recoverMilliseconds = attackConfig.getTiming().getRecoveryDuration();

        this.displayOffsetX = attackConfig.getDisplay().getOffsetX();
        this.displayOffsetY = attackConfig.getDisplay().getOffsetY();
        this.displayOffsetZ = attackConfig.getDisplay().getOffsetZ();
        this.displayScaleX = attackConfig.getDisplay().getScaleX();
        this.displayScaleY = attackConfig.getDisplay().getScaleY();
        this.displayScaleZ = attackConfig.getDisplay().getScaleZ();
        this.displayRotationY = attackConfig.getDisplay().getRotationY();
        this.displayRotationZ = attackConfig.getDisplay().getRotationZ();
        this.displayRotationX = attackConfig.getDisplay().getRotationX();

        this.displayGlowColor = ColorUtil.fromHex(attackConfig.getDisplay().getGlowColor());
        this.attackGlowColor = ColorUtil.fromHex(attackConfig.getDisplay().getAttackGlowColor());

        this.displaySmoothSteps = attackConfig.getMotion().getDisplaySmoothSteps();

        this.windupSlownessAmplifier = attackConfig.getEffects().getWindupSlownessAmplifier();
        this.attackSlownessAmplifier = attackConfig.getEffects().getAttackSlownessAmplifier();

        this.rangeMultiplier = attackConfig.getModifiers().getRangeMultiplier();

        cancelled = false;
        finished = false;
    }

    public Attack(AttackType type, boolean orientWithPitch, Predicate<LivingEntity> filter,
                  int windupMilliseconds, int windupIterations, double windupStartValue, double windupEndValue,
                  int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue,
                  int recoverMilliseconds) {
        this(type, orientWithPitch, filter);
        this.windupMilliseconds = windupMilliseconds;
        this.windupIterations = windupIterations;
        this.windupStartValue = windupStartValue;
        this.windupEndValue = windupEndValue;
        this.attackMilliseconds = attackMilliseconds;
        this.attackIterations = attackIterations;
        this.attackStartValue = attackStartValue;
        this.attackEndValue = attackEndValue;
        this.recoverMilliseconds = recoverMilliseconds;
    }

    public void execute(Combatant attacker) {
        this.attacker = attacker;
        a = attacker.entity();

        int castDuration = windupMilliseconds + attackMilliseconds + recoverMilliseconds;

        weaponDisplay = (ItemDisplay) a.getWorld().spawnEntity(a.getLocation(), EntityType.ITEM_DISPLAY);
        weaponDisplay.setInvisible(true);
        weaponDisplay.setItemStack(attacker.getItemStackInHand(true));
        weaponDisplay.setGlowing(true);
        weaponDisplay.setGlowColorOverride(displayGlowColor);
        weaponDisplay.setTransformation(
                new Transformation(
                        new Vector3f(displayOffsetX, displayOffsetY, displayOffsetZ),
                        new Quaternionf()
                                .rotateY(displayRotationY)
                                .rotateZ(displayRotationZ)
                                .rotateX(displayRotationX),
                        new Vector3f(displayScaleX, displayScaleY, displayScaleZ),
                        new Quaternionf()
                )
        );

        cast(attacker, castDuration, this);
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        startWindup();
    }

    public void applyConsistentEffects() {
        // stay put and drop
        a.setVelocity(new Vector(a.getVelocity().getX(), Math.min(0, a.getVelocity().getY()), a.getVelocity().getZ()));
    }

    public void applySelfWindupEffects() {
        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS,
                windupMilliseconds/Prefab.Value.MILLISECONDS_PER_TICK,
                windupSlownessAmplifier);
        attacker.entity().addPotionEffect(slowness);
    }

    // While winding up, can change direction, so basis must be recalculated often
    public void startWindup() {
//        applySelfWindupEffects();

        double windupRange = windupEndValue - windupStartValue;
        double step = windupRange/windupIterations;
        int msPerIteration = windupMilliseconds/windupIterations;

        for (int i = 0; i < windupIterations; i++) {
            final int idx = i;
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled) {
                        cancel();
                        return;
                    }
                    applyConsistentEffects();

                    generateBezierFunction();
                    cur = weaponPathFunction.apply(windupStartValue + (step * idx));

                    DisplayUtil.smoothTeleport(weaponDisplay,
                            Math.max(displaySmoothSteps, msPerIteration/Prefab.Value.MILLISECONDS_PER_TICK));
                    weaponDisplay.teleport(a.getEyeLocation().add(cur).setDirection(cur));
                    weaponDisplay.setInvisible(false);

                    if (idx == windupIterations - 1) {
                        startAttack();
                    }
                }
            }, idx * msPerIteration, TimeUnit.MILLISECONDS);
        }
    }

    private void applySelfAttackEffects() {
        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS,
                attackMilliseconds/Prefab.Value.MILLISECONDS_PER_TICK,
                attackSlownessAmplifier);
        attacker.entity().addPotionEffect(slowness);
    }

    public void startAttack() {
//        applySelfAttackEffects();

        double attackRange = attackEndValue - attackStartValue;
        double step = attackRange / attackIterations;
        int msPerIteration = attackMilliseconds / attackIterations;

        generateBezierFunction();

        final Vector[] prev = { weaponPathFunction.apply(attackStartValue - step) };

        for (int i = 0; i <= attackIterations; i++) {
            final int idx = i;
            final Location origin = a.getEyeLocation();
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled) {
                        cancel();
                        return;
                    }
                    if (idx == 1) weaponDisplay.setGlowColorOverride(attackGlowColor);

                    applyConsistentEffects();

                    cur = weaponPathFunction.apply(attackStartValue + (step * idx));
                    DisplayUtil.smoothTeleport(weaponDisplay,
                            Math.max(displaySmoothSteps, msPerIteration/Prefab.Value.MILLISECONDS_PER_TICK));
                    weaponDisplay.teleport(origin.add(cur).setDirection(cur));

                    applyHitEffects(collectHitEntities(cur, prev[0]));

                    prev[0] = cur;
                    if (idx == attackIterations) {
                        startRecovery();
                    }
                }
            }, idx * msPerIteration, TimeUnit.MILLISECONDS);
        }
    }

    public void applyHitEffects(HashSet<LivingEntity> targets) {

    }

    public HashSet<LivingEntity> collectHitEntities(Vector cur, Vector prev) {
        HashSet<LivingEntity> targets = new HashSet<>();

        // use filter predicate



        return targets;
    }

    public void applySelfRecoveryEffects() {
        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS,
                recoverMilliseconds/Prefab.Value.MILLISECONDS_PER_TICK,
                attackSlownessAmplifier);
        attacker.entity().addPotionEffect(slowness);
    }

    public void startRecovery() {
//        applySelfRecoveryEffects();

        SwordScheduler.runBukkitTaskLater(
                () -> { finished = true; if (weaponDisplay.isValid()) weaponDisplay.remove(); },
                recoverMilliseconds, TimeUnit.MILLISECONDS);
    }

    // static function oriented with the players current basis to be used when the attack is executed.
    public void generateBezierFunction() {
        ArrayList<Vector> basis = orientWithPitch ?
                VectorUtil.getBasis(a.getEyeLocation(), a.getEyeLocation().getDirection()) :
                VectorUtil.getBasisWithoutPitch(a.getEyeLocation());
        curRight = basis.getFirst();
        curUp = basis.get(1);
        curForward = basis.getLast();

        List<Vector> adjusted = BezierUtil.adjustCtrlToBasis(basis, controlVectors, rangeMultiplier);
        weaponPathFunction = BezierUtil.cubicBezier3D(adjusted.get(0), adjusted.get(1), adjusted.get(2), adjusted.get(3));
    }

    private static @NotNull List<Vector> getAttackVectors(AttackType attackType) {
        List<Vector> ctrlVectors = null;
        switch (attackType) {
            case BASIC_1 -> ctrlVectors = Prefab.ControlVectors.SLASH1;
            case BASIC_2 -> ctrlVectors = Prefab.ControlVectors.SLASH2;
            case BASIC_3 -> ctrlVectors = Prefab.ControlVectors.SLASH3;
            case HEAVY_1 -> ctrlVectors = Prefab.ControlVectors.UP_SMASH;
            case D_AIR -> ctrlVectors = Prefab.ControlVectors.D_AIR_SLASH;
            case N_AIR -> ctrlVectors = Prefab.ControlVectors.N_AIR_SLASH;
            default -> ctrlVectors = List.of(
                    Prefab.Direction.UP,
                    Prefab.Direction.DOWN,
                    Prefab.Direction.OUT_UP,
                    Prefab.Direction.OUT_DOWN);
        }
        return ctrlVectors;
    }
}
