package btm.sword.system.attack;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.Prefab;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.Basis;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;
import btm.sword.util.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;

public class Attack extends SwordAction implements Runnable {

    protected Combatant attacker;
    protected LivingEntity attackingEntity;
    protected final AttackProfile attackProfile;
    protected final boolean orientWithPitch;

    protected final ControlVectors controlVectors;
    protected Function<Double, Vector> weaponPathFunction;

    protected Vector curRight;
    protected Vector curUp; // Reserved for future vertical knockback calculations
    protected Vector curForward; // Reserved for future c2 knockback calculations

    @Setter // origin can be set for stationary attacks
    protected Location origin;
    protected Location attackLocation; // current bezier vec + origin

    protected Vector cur;
    protected Vector prev;
    protected Vector to; // the vector from the previous vector TO the current bezier vector

    protected final HashSet<LivingEntity> hitDuringAttack;
    protected Predicate<Entity> filter;
    @Getter
    protected SwordEntity currentTarget;

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
    protected boolean finishedOrCanceled = false;

    protected Consumer<SwordEntity> onHitInstructions;

    @Getter
    protected Attack nextAttack;
    protected int millisecondDelayBeforeNextAttack;

    public Attack(AttackProfile profile, boolean orientWithPitch) {
        controlVectors = profile.controlVectors();
        this.attackProfile = profile;
        this.orientWithPitch = orientWithPitch;

        hitDuringAttack = new HashSet<>();

        this.attackMilliseconds = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_DURATION;
        this.attackIterations = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_ITERATIONS;
        this.attackStartValue = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_START_VALUE;
        this.attackEndValue = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_END_VALUE;

        this.rangeMultiplier = Config.Combat.ATTACK_CLASS_MODIFIERS_RANGE_MULTIPLIER;
    }

    public Attack(AttackProfile profile, boolean orientWithPitch,
                  int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue) {
        this(profile, orientWithPitch);
        this.attackMilliseconds = attackMilliseconds;
        this.attackIterations = attackIterations;
        this.attackStartValue = attackStartValue;
        this.attackEndValue = attackEndValue;
    }

    public void calcTickValues() {
        int numOfTicks = attackMilliseconds/Prefab.Value.MILLISECONDS_PER_TICK;
        this.ticks = numOfTicks <= 0 ? 1 : numOfTicks + 1;
        int msPerIteration = attackMilliseconds/attackIterations;
        int ticksPerIteration = msPerIteration/Prefab.Value.MILLISECONDS_PER_TICK;
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

    public Attack setHitInstructions(Consumer<SwordEntity> onHitInstructions) {
        this.onHitInstructions = onHitInstructions;
        return this;
    }

    public boolean hasNextAttack() {
        return nextAttack != null;
    }

    public void execute(Combatant attacker) {
        this.attacker = attacker;

        this.attackingEntity = attacker.entity();
        this.filter = entity ->
            entity instanceof LivingEntity livingEntity &&
            livingEntity != attackingEntity &&
            livingEntity.getUniqueId() != attacker.getUniqueId() &&
            livingEntity.isValid();

        cast(attacker, 200, this);
    }

    private void onRun() {
        attacker.setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) attacker.calcValueReductive(AspectType.FINESSE,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);
        attacker.setDurationOfLastAttack(cooldown * Config.Combat.ATTACKS_DURATION_MULTIPLIER);
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
        startupLogic();
        calcTickValues();

        curIteration = 0;
        for (int i = 0; i <= attackIterations; i++) { // TODO: #120 - Research a better way than scheduling all at once with delays
            final int idx = i;
            SwordScheduler.runBukkitTaskLater(
                new BukkitRunnable() {
                @Override
                public void run() {
                    if (finishedOrCanceled) {
                        cancel();
                        return;
                    }

                    applyConsistentEffects();

                    cur = weaponPathFunction.apply(attackStartValue + (step * idx));
                    to = cur.clone().subtract(prev);
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
                                        endingLogic();
                                        nextAttack.execute(attacker);
                                    }
                                }, millisecondDelayBeforeNextAttack, TimeUnit.MILLISECONDS
                            );
                        }
                    }
                    prev = cur;
                    curIteration++;
                }
            }, calcIterationStartDelay(i, msPerIteration), TimeUnit.MILLISECONDS);
        }
    }

    protected void startupLogic() {

    }

    protected void endingLogic() {

    }

    protected int calcIterationStartDelay(int i, int msPerIteration) {
        return i * msPerIteration;
    }

    public void handleCallback() {
        if (callback != null) {
            SwordScheduler.runBukkitTaskLater(callback, msBeforeCallbackSchedule, TimeUnit.MILLISECONDS);
        }
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

    // TODO: #128 - Make Particle Effects more dynamic. Low prio.
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

                if (sTarget == null || sTarget.isDead())
                    continue;

                currentTarget = sTarget;

                if (!currentTarget.entity().isDead()) {
                    currentTarget.hit(attacker, Prefab.Attacks.basicAttack,
                            attackProfile.knockbackFunction().apply(this));

                    Prefab.Particles.TEST_HIT.display(currentTarget.getChestLocation());

                    if (onHitInstructions != null) onHitInstructions.accept(currentTarget);
                }
            }
        }
        hitDuringAttack.addAll(targets);
    }

    protected HashSet<LivingEntity> collectHitEntities() {
        if (origin == null || origin.toVector().isZero() || !origin.isFinite()) {
            return new HashSet<>();
        }
        double secantRadius = Config.Combat.HITBOXES_SECANT_RADIUS;
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
            new ParticleWrapper(Particle.BLOCK, 5, 0.5, 0.5, 0.5, //TODO: config or naw
                    Objects.requireNonNull(result.getHitBlock()).getBlockData()).display(attackLocation);
            Prefab.Particles.COLLIDE.display(attackLocation);

            // potential reduction of damage formula
        }
        else if (direction.lengthSquared() > (double) 2 / (attackIterations*attackIterations)) {
            // interpolated particle, same as normal particle
            // 0.5 - half the length to the particle
            Prefab.Particles.TEST_SWING.display(attackLocation.clone().add(direction.multiply(0.5)));
        }
    }

    // static function oriented with the players current basis to be used when the attack is executed.
    void generateBezierFunction() {
        Basis basis = orientWithPitch ?
                VectorUtil.getBasis(attackingEntity.getEyeLocation(), attackingEntity.getEyeLocation().getDirection()) :
                VectorUtil.getBasisWithoutPitch(attackingEntity);
        curRight = basis.right();
        curUp = basis.up();
        curForward = basis.forward();

        ControlVectors adjusted = attackProfile instanceof GeneratedAttackProfile ?
            controlVectors :
            controlVectors.adjustToBasis(basis, rangeMultiplier);
        weaponPathFunction = BezierUtil.cubicBezier3D(adjusted);
    }

    public Vector getCur() {
        return cur.clone();
    }

    public Vector getPrev() {
        return prev.clone();
    }

    public Vector getTo() {
        return to.clone();
    }

    public Vector getRightVector() {
        return curRight.clone();
    }

    public Vector getForwardVector() {
        return curForward.clone();
    }

    public Vector getUpVector() {
        return curUp.clone();
    }
}
