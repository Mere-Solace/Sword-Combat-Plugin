package btm.sword.system.attack;

import btm.sword.config.ConfigManager;
import btm.sword.config.section.CombatConfig;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.SwordAction;
import btm.sword.system.action.type.AttackType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.display.Prefab;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.VectorUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Attack extends SwordAction implements Runnable {
    private final CombatConfig.AttacksConfig attacksConfig;
    private final CombatConfig.AttackClassConfig attackConfig;

    private Combatant attacker;
    private LivingEntity attackingEntity;
    private final AttackType attackType;

    private final boolean orientWithPitch;

    private final List<Vector> controlVectors;
    private Function<Double, Vector> weaponPathFunction;

    private Vector curRight;
    private Vector curUp; // Reserved for future vertical knockback calculations
    private Vector curForward; // Reserved for future forward knockback calculations

    private Location origin;
    private Location attackLocation;
    private Vector cur;
    private Vector prev;

    private final HashSet<LivingEntity> hitDuringAttack;
    private Predicate<LivingEntity> filter;

    private int attackMilliseconds;
    private int attackIterations;
    private double attackStartValue;
    private double attackEndValue;

    private final double rangeMultiplier;

    public Attack (AttackType type, boolean orientWithPitch) {
        controlVectors = getAttackVectors(type);
        this.attackType = type;
        this.orientWithPitch = orientWithPitch;
        attacksConfig = ConfigManager.getInstance().getCombat().getAttacks();
        attackConfig = ConfigManager.getInstance().getCombat().getAttackClass();

        hitDuringAttack = new HashSet<>();

        this.attackMilliseconds = attackConfig.getTiming().getAttackDuration();
        this.attackIterations = attackConfig.getTiming().getAttackIterations();
        this.attackStartValue = attackConfig.getTiming().getAttackStartValue();
        this.attackEndValue = attackConfig.getTiming().getAttackEndValue();

        this.rangeMultiplier = attackConfig.getModifiers().getRangeMultiplier();
    }

    public Attack(AttackType type, boolean orientWithPitch,
                  int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue) {
        this(type, orientWithPitch);
        this.attackMilliseconds = attackMilliseconds;
        this.attackIterations = attackIterations;
        this.attackStartValue = attackStartValue;
        this.attackEndValue = attackEndValue;
    }

    public void execute(Combatant attacker) {
        this.attacker = attacker;
        this.attackingEntity = attacker.entity();
        this.filter = livingEntity -> livingEntity != attackingEntity &&
                livingEntity.getUniqueId() != attacker.getUniqueId() &&
                livingEntity.isValid();
        attackMilliseconds = (int) attacker.calcValueReductive(AspectType.FINESSE,
                attacksConfig.getCastTimingMinDuration(),
                attacksConfig.getCastTimingMaxDuration(),
                attacksConfig.getCastTimingReductionRate());
        cast(attacker, attackMilliseconds, this);
    }

    @Override
    public void run() {
        onRun();
    }

    private void onRun() {
        attacker.setTimeOfLastAttack(System.currentTimeMillis());
        attacker.setDurationOfLastAttack(attackMilliseconds * attacksConfig.getDurationMultiplier());
        startAttack();
    }

    private void playSwingSoundEffects() {
        Prefab.Sounds.ATTACK.play(attacker.entity());
    }

    private void applyConsistentEffects() {
    }

    private void applySelfAttackEffects() {
    }

    private void startAttack() {
        applySelfAttackEffects();
        playSwingSoundEffects();

        double attackRange = attackEndValue - attackStartValue;
        double step = attackRange / attackIterations;
        int msPerIteration = attackMilliseconds / attackIterations;

        generateBezierFunction();

        origin = attackingEntity.getLocation().add(attacker.getChestVector());
        prev = weaponPathFunction.apply(attackStartValue - step);

        for (int i = 0; i <= attackIterations; i++) {
            final int idx = i;

            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    applyConsistentEffects();

                    cur = weaponPathFunction.apply(attackStartValue + (step * idx));
                    attackLocation = origin.clone().add(cur);

                    drawAttackEffects(idx);
                    applyHitEffects(collectHitEntities());
                    swingTest();

                    prev = cur;
                }
            }, idx * msPerIteration, TimeUnit.MILLISECONDS);
        }
    }

    private void drawAttackEffects(int step) {
        Prefab.Particles.TEST_SWING.display(attackLocation);
    }

    private void applyHitEffects(HashSet<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (!hitDuringAttack.contains(target)) {
                SwordEntity sTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());

                if (sTarget == null)
                    continue;

                if (!sTarget.entity().isDead()) {
                    sTarget.hit(attacker,
                            5, 1, 15, 6,
                            getKnockBackVector(attackType, target));

                    Prefab.Particles.TEST_HIT.display(sTarget.getChestLocation());
                } else {
                    attacker.message("Target: " + target + " caused an NPE");
                }
            }
        }
        hitDuringAttack.addAll(targets);
    }

    private HashSet<LivingEntity> collectHitEntities() {
        double secantRadius = ConfigManager.getInstance().getCombat().getHitboxes().getSecantRadius();
        return HitboxUtil.secant( attackingEntity, origin, attackLocation, secantRadius,
                true, filter);
    }

    private void swingTest() {
        // check if attack entered the ground
        // enter ground and interpolation function
        Vector direction = cur.clone().subtract(prev);
        RayTraceResult result = attackingEntity.getWorld().rayTraceBlocks(attackLocation, direction, 0.3);
        if (result != null) {
            // enter ground particles
            new ParticleWrapper(Particle.BLOCK, 10, 0.5, 0.5, 0.5,
                    Objects.requireNonNull(result.getHitBlock()).getBlockData()).display(attackLocation);
            Prefab.Particles.COLLIDE.display(attackLocation);
            // potential reduction of damage formula
        }
        else if (direction.lengthSquared() > (double) 2 / (attackIterations*attackIterations)) {
            // interpolated particle, same as normal particle
            Prefab.Particles.TEST_SWING.display(attackLocation.clone().add(direction.multiply(0.5)));
        }
    }

    // static function oriented with the players current basis to be used when the attack is executed.
    private void generateBezierFunction() {
        ArrayList<Vector> basis = orientWithPitch ?
                VectorUtil.getBasis(attackingEntity.getEyeLocation(), attackingEntity.getEyeLocation().getDirection()) :
                VectorUtil.getBasisWithoutPitch(attackingEntity);
        curRight = basis.getFirst();
        curUp = basis.get(1);
        curForward = basis.getLast();

        List<Vector> adjusted = BezierUtil.adjustCtrlToBasis(basis, controlVectors, rangeMultiplier);
        weaponPathFunction = BezierUtil.cubicBezier3D(adjusted.get(0), adjusted.get(1), adjusted.get(2), adjusted.get(3));
    }

    private static @NotNull List<Vector> getAttackVectors(AttackType attackType) {
        List<Vector> ctrlVectors;
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

    private @NotNull Vector getKnockBackVector(AttackType attackType, LivingEntity target) {
        var attackVelocity = ConfigManager.getInstance().getPhysics().getAttackVelocity();
        Vector base =  Prefab.Direction.UP.clone().multiply(attackVelocity.getKnockbackVerticalBase());
        Vector r = curRight.clone().multiply(attackVelocity.getKnockbackHorizontalModifier());

        Vector knockback;
        switch (attackType) {
            case BASIC_1 -> knockback = base.add(r);
            case BASIC_2 -> knockback = base.add(r.multiply(-1));
            case BASIC_3 -> knockback = target.getLocation().toVector()
                    .subtract(origin.toVector()).normalize()
                    .subtract(new Vector(0, attackVelocity.getKnockbackVerticalBase() * 2, 0));
            case HEAVY_1 -> knockback = base.multiply(2);
            case D_AIR -> knockback = base.multiply(-2);
            case N_AIR -> knockback = target.getLocation().toVector()
                    .subtract(origin.toVector()).normalize().multiply(attackVelocity.getKnockbackNormalMultiplier());

            default -> knockback = cur.clone().normalize().multiply(attackVelocity.getKnockbackNormalMultiplier());
        }
        return knockback;
    }
}
