package btm.sword.system.attack;

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

import btm.sword.config.ConfigManager;
import btm.sword.config.section.CombatConfig;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.Prefab;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;

public class Attack extends SwordAction implements Runnable {
    protected final CombatConfig.AttacksConfig attacksConfig;
    protected final CombatConfig.AttackClassConfig attackConfig;

    protected Combatant attacker;
    protected LivingEntity attackingEntity;
    protected final AttackType attackType;
    protected final boolean orientWithPitch;

    protected final List<Vector> controlVectors;
    protected Function<Double, Vector> weaponPathFunction;

    protected Vector curRight;
    protected Vector curUp; // Reserved for future vertical knockback calculations
    protected Vector curForward; // Reserved for future forward knockback calculations

    @Setter // origin can be set for stationary attacks
    protected Location origin;
    protected Location attackLocation; // current bezier vec + origin
    protected Vector cur;
    protected Vector prev;

    protected final HashSet<LivingEntity> hitDuringAttack;
    protected Predicate<LivingEntity> filter;

    protected int curIteration;

    protected int attackMilliseconds;
    protected int attackIterations;
    protected double attackStartValue;
    protected double attackEndValue;
    protected int ticks;
    protected int tickPeriod;

    protected final double rangeMultiplier;

    protected Runnable callback;
    protected int msBeforeCallbackSchedule;

    @Getter
    protected Attack nextAttack;
    protected int millisecondDelayBeforeNextAttack;

    public Attack (AttackType type, boolean orientWithPitch) {
        controlVectors = type.controlVectors();
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

    public void calcTickValues() {
        int numOfTicks = attackMilliseconds/50;
        this.ticks = numOfTicks <= 0 ? 1 : numOfTicks + 1;
        int msPerIteration = attackMilliseconds/attackIterations;
        int ticksPerIteration = msPerIteration/50;
        this.tickPeriod = ticksPerIteration <= 0 ? 1 : ticksPerIteration;
    }

    public Attack setNextAttack(Attack nextAttack, int millisecondDelayBeforeNextAttack) {
        this.nextAttack = nextAttack;
        this.millisecondDelayBeforeNextAttack = millisecondDelayBeforeNextAttack;
        return this; // for initialization chaining
    }

    public Attack setCallback(Runnable callback, int msBeforeCallbackSchedule) {
        this.callback = callback;
        this.msBeforeCallbackSchedule = msBeforeCallbackSchedule;
        return this;
    }

    public boolean hasNextAttack() {
        return nextAttack != null;
    }

    public void execute(Combatant attacker) {
        this.attacker = attacker;

        this.attackingEntity = attacker.entity();
        this.filter = livingEntity -> livingEntity != attackingEntity &&
                livingEntity.getUniqueId() != attacker.getUniqueId() &&
                livingEntity.isValid();

        cast(attacker, 5, this);
    }

    // TODO change cast time and duration of last attack
    private void onRun() {
        attacker.setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) attacker.calcValueReductive(AspectType.FINESSE,
            attacksConfig.getCastTimingMinDuration(),
            attacksConfig.getCastTimingMaxDuration(),
            attacksConfig.getCastTimingReductionRate());
        attacker.setDurationOfLastAttack(cooldown * attacksConfig.getDurationMultiplier());
        startAttack();
    }

    @Override
    public void run() {
        onRun();
    }

    void playSwingSoundEffects() {
        Prefab.Sounds.ATTACK.play(attacker.entity());
    }

    void applyConsistentEffects() {
    }

    void applySelfAttackEffects() {
    }

    protected void startAttack() {
        applySelfAttackEffects();
        playSwingSoundEffects();

        double attackRange = attackEndValue - attackStartValue;
        double step = attackRange / attackIterations;
        int msPerIteration = attackMilliseconds / attackIterations;

        generateBezierFunction();

        determineOrigin();

        prev = weaponPathFunction.apply(attackStartValue - step);

        calcTickValues();

        curIteration = 0;
        for (int i = 0; i <= attackIterations; i++) {
            final int idx = i;
            SwordScheduler.runBukkitTaskLater(
                new BukkitRunnable() {
                @Override
                public void run() {
                    applyConsistentEffects();

                    cur = weaponPathFunction.apply(attackStartValue + (step * idx));
                    attackLocation = origin.clone().add(cur);

                    drawAttackEffects();
                    hit();
                    swingTest();

                    // allows for chaining of attack logic
                    if (idx == attackIterations) {
                        handleCallback();
                        if (nextAttack != null) {
                            SwordScheduler.runBukkitTaskLater(
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        prepareForNextUse();
                                        nextAttack.execute(attacker);
                                    }
                                }, millisecondDelayBeforeNextAttack, TimeUnit.MILLISECONDS
                            );
                        }
                    }
                    prev = cur;
                    curIteration++;
                }
            }, curIteration * msPerIteration, TimeUnit.MILLISECONDS);
        }
    }

    public void handleCallback() {
        if (callback != null) {
            SwordScheduler.runBukkitTaskLater(callback, msBeforeCallbackSchedule, TimeUnit.MILLISECONDS);
        }
    }

    // TODO: Either remove this, or make attack a consumable class and never re-use. it gets a little messy for some reason.
    private void prepareForNextUse() {
        hitDuringAttack.clear();
        origin = null;
    }

    void determineOrigin() {
        if (origin == null)
            origin = attackingEntity.getLocation().add(attacker.getChestVector());
    }

    public void setOriginOfAll(Location origin) {
        this.origin = origin;
        Attack cur = getNextAttack();
        while (cur != null) {
            cur.setOrigin(origin);
            cur = cur.getNextAttack();
        }
    }

    // TODO: Make Particle Effects more dynamic. Low prio.
    protected void drawAttackEffects() {
        Prefab.Particles.TEST_SWING.display(attackLocation);
    }

    protected void hit() {
        applyHitEffects(collectHitEntities());
    }

    protected void applyHitEffects(HashSet<LivingEntity> targets) {
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

    protected HashSet<LivingEntity> collectHitEntities() {
        double secantRadius = ConfigManager.getInstance().getCombat().getHitboxes().getSecantRadius();
        return HitboxUtil.secant(origin, attackLocation, secantRadius, filter);
    }

    void swingTest() {
        // check if attack entered the ground
        // enter ground and interpolation function
        Vector direction = cur.clone().subtract(prev);
        if (direction.isZero()) return;
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
    void generateBezierFunction() {
        ArrayList<Vector> basis = orientWithPitch ?
                VectorUtil.getBasis(attackingEntity.getEyeLocation(), attackingEntity.getEyeLocation().getDirection()) :
                VectorUtil.getBasisWithoutPitch(attackingEntity);
        curRight = basis.getFirst();
        curUp = basis.get(1);
        curForward = basis.getLast();

        List<Vector> adjusted = BezierUtil.adjustCtrlToBasis(basis, controlVectors, rangeMultiplier);
        weaponPathFunction = BezierUtil.cubicBezier3D(adjusted.get(0), adjusted.get(1), adjusted.get(2), adjusted.get(3));
    }

    private @NotNull Vector getKnockBackVector(AttackType attackType, LivingEntity target) {
        var attackVelocity = ConfigManager.getInstance().getPhysics().getAttackVelocity();
        Vector base =  Prefab.Direction.UP.clone().multiply(attackVelocity.getKnockbackVerticalBase());
        Vector r = curRight.clone().multiply(attackVelocity.getKnockbackHorizontalModifier());

        return switch (attackType) {
            case SLASH1 -> base.add(r);
            case SLASH2 -> base.add(r.multiply(-1));
            case SLASH3 -> target.getLocation().toVector()
                    .subtract(origin.toVector()).normalize()
                    .subtract(new Vector(0, attackVelocity.getKnockbackVerticalBase() * 2, 0));
            case D_AIR -> base.multiply(-2);
            case N_AIR -> target.getLocation().toVector()
                    .subtract(origin.toVector()).normalize().multiply(attackVelocity.getKnockbackNormalMultiplier());

            default -> cur.clone().normalize().multiply(attackVelocity.getKnockbackNormalMultiplier());
        };
    }
}
