package btm.sword.system.attack;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.SwordAction;
import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.entity.HitboxUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.BezierUtil;
import btm.sword.utility.math.ControlVectors;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.misc.ConsumerToConsumePair;
import lombok.Getter;

public class Attack extends SwordAction implements Runnable {

    protected Combatant attacker;
    protected ItemStack itemUsedInAttack;
    protected LivingEntity attackingEntity;
    protected final AttackProfile attackProfile;
    protected final boolean orientWithPitch;

    protected final ControlVectors controlVectors;
    protected Function<Double, Vector> weaponPathFunction;

    protected Vector curRight;
    protected Vector curUp; // Reserved for future vertical knockback calculations
    protected Vector curForward; // Reserved for future forward knockback calculations

    protected Location origin;
    protected Location attackLocation; // current bezier vec + origin

    protected Vector cur;
    protected Vector prev;
    protected Vector to; // the vector from the previous vector TO the current bezier vector

    protected final HashSet<LivingEntity> hitDuringAttack;
    protected Predicate<Entity> filter;
    @Getter
    protected SwordEntity currentTarget;

    protected AtomicInteger curIteration;

    protected int attackMilliseconds;
    protected int attackIterations;
    protected double attackStartValue;
    protected double attackEndValue;

    protected double interpolationValueRange;
    protected double interpolationStep;
    protected int msPerIteration;

    protected final double rangeMultiplier;

    protected Runnable callback;
    protected int msBeforeCallbackSchedule;

    protected TimeArbiter.TaskHandle attackIterationTask;

    protected ConsumerToConsumePair<?>[] onAttackConnectInstructions = {};
    protected Consumer<SwordEntity> onEntityHitInstructions;

    @Getter
    protected Attack nextAttack;
    protected int millisecondDelayBeforeNextAttack;

    public Attack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch) {
        this.itemUsedInAttack = itemUsedInAttack;
        this.controlVectors = profile.controlVectors();
        this.attackProfile = profile;
        this.orientWithPitch = orientWithPitch;

        hitDuringAttack = new HashSet<>();

        curIteration = new AtomicInteger(0);

        this.attackMilliseconds = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_DURATION;
        this.attackIterations = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_ITERATIONS;
        this.attackStartValue = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_START_VALUE;
        this.attackEndValue = Config.Combat.ATTACK_CLASS_TIMING_ATTACK_END_VALUE;

        this.rangeMultiplier = Config.Combat.ATTACK_CLASS_MODIFIERS_RANGE_MULTIPLIER;
    }

    public Attack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch,
                  int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue) {
        this(itemUsedInAttack, profile, orientWithPitch);
        this.attackMilliseconds = attackMilliseconds;
        this.attackIterations = attackIterations;
        this.attackStartValue = attackStartValue;
        this.attackEndValue = attackEndValue;
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

    public Attack setAttackConnectInstructions(ConsumerToConsumePair<?>... onAttackConnectInstructions) {
        this.onAttackConnectInstructions = onAttackConnectInstructions;
        return this;
    }

    public Attack setOnEntityHitInstructions(Consumer<SwordEntity> onEntityHitInstructions) {
        this.onEntityHitInstructions = onEntityHitInstructions;
        return this;
    }

    public boolean nextAttackExists() {
        return nextAttack != null;
    }

    public void execute(Combatant attacker) {
        this.attacker = attacker;

        this.attackingEntity = attacker.self();
        this.filter = entity ->
            entity instanceof LivingEntity livingEntity &&
            livingEntity != attackingEntity &&
            !attacker.isSelf(livingEntity) &&
            livingEntity.isValid();

        cast();
    }

    // TODO: #139 make usage dynamic
    protected void cast() {
        // Trigger attack logic immediately; input-level casting will be handled by the InputAction's castDuration
        onRun();
    }

    private void onRun() {
        attacker.setTimeOfLastAttack(System.currentTimeMillis());
        int cooldown = (int) attacker.calcValueReductive(AspectType.FINESSE,
            Config.Combat.ATTACKS_CAST_TIMING_MIN_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_MAX_DURATION,
            Config.Combat.ATTACKS_CAST_TIMING_REDUCTION_RATE);
        attacker.setDurationOfLastAttack(cooldown);
        startAttack();
    }

    @Override
    public void run() {
        onRun();
    }

    void playSwingSoundEffects() {
        Prefab.Sounds.ATTACK.playForAllInRadius(attacker.self());
    }

    void applyConsistentEffects() {

    }

    void applySelfAttackEffects() {

    }

    protected void startAttack() {
        applySelfAttackEffects();
        playSwingSoundEffects();

        interpolationValueRange = attackEndValue - attackStartValue;
        interpolationStep = interpolationValueRange / attackIterations;
        int msPerIteration = Math.max(1, attackMilliseconds / attackIterations);

        generateBezierFunction();
        determineOrigin();
        prev = weaponPathFunction.apply(attackStartValue - interpolationStep);
        startupLogic();

        curIteration.set(0);

        attackIterationTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                applyConsistentEffects();
                // curIteration is incremented HERE----------------------------------------\/
                cur = weaponPathFunction.apply(attackStartValue + (interpolationStep * curIteration.getAndIncrement()));
                to = cur.clone().subtract(prev);
                attackLocation = origin.clone().add(cur);

                drawAttackEffects();
                performHitLogic();
                swingTest();
            },
            () -> prev = cur,
            null,
            0, msPerIteration,
            Attack.class, "startAttack",
            new PredicateRunnablePair(
                this::shouldEndAttack,
                () -> {
                    handleCallback();
                    endingLogic();
                    if (nextAttackExists()) {
                        SwordScheduler.runBukkitTaskLater(() -> {
                            nextAttack.execute(attacker);
                            }, millisecondDelayBeforeNextAttack, TimeUnit.MILLISECONDS
                        );
                    }
                }
            )
        );
    }

    protected boolean shouldEndAttack() {
        return curIteration.get() >= attackIterations;
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

    public Attack setOriginOfAll(Location origin) {
        this.origin = origin;
        Attack cur = getNextAttack();
        while (cur != null) {
            cur.setOrigin(origin);
            cur = cur.getNextAttack();
        }
        return this;
    }

    void determineOrigin() {
        if (origin == null)
            origin = attackingEntity.getLocation().add(attacker.getChestVector());
    }

    public Attack setOrigin(@Nullable Location origin) {
        this.origin = origin;
        return this;
    }

    // TODO: #128 - Make Particle Effects more dynamic. Low prio.
    protected void drawAttackEffects() {
        Prefab.Particles.TEST_SWING.display(attackLocation);
    }

    protected void performHitLogic() {
        applyHitEffects(collectHitEntities());
    }

    protected void applyHitEffects(HashSet<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            if (!hitDuringAttack.contains(target)) {
                SwordEntity sTarget = SwordEntityArbiter.getOrAdd(target);

                if (sTarget == null || sTarget.isDead())
                    continue;

                currentTarget = sTarget;

                if (!currentTarget.self().isDead()) {
                    hit();
                    if (onEntityHitInstructions != null) onEntityHitInstructions.accept(currentTarget);
                    for (ConsumerToConsumePair<?> connectInstruction : onAttackConnectInstructions) {
                        connectInstruction.accept();
                    }
                }
            }
        }
        hitDuringAttack.addAll(targets);
    }

    protected void hit() {
        currentTarget.hit(attacker, Prefab.Attacks.basicAttack,
            attackProfile.knockbackFunction().apply(this));

        Prefab.Particles.TEST_HIT.display(currentTarget.getChestLocation());
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
                VectorUtil.getBasis(
                    origin == null ? attackingEntity.getEyeLocation() : origin,
                    origin == null ? attackingEntity.getEyeLocation().getDirection() : origin.getDirection()) :
                VectorUtil.getBasisWithoutPitch(origin == null ? attackingEntity.getEyeLocation() : origin);
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
