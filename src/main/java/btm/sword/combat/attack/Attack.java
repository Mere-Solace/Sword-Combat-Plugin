package btm.sword.combat.attack;

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

import btm.sword.action.core.SwordAction;
import btm.sword.combat.style.AttackProfile;
import btm.sword.config.Config;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.entity.HitboxUtil;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.misc.ConsumerToConsumePair;
import btm.sword.util.misc.Debug;
import btm.sword.util.prefab.Prefab;
import lombok.Getter;

/**
 * Base class for all weapon sweep attacks.
 * <p>
 * An {@code Attack} drives a time-sliced iteration loop that steps a parametric
 * {@link Function}&lt;Double, Vector&gt; (the weapon path function) from
 * {@code attackStartValue} to {@code attackEndValue} across {@code attackIterations}
 * steps spaced {@code attackMilliseconds / attackIterations} ms apart.  Each step
 * computes the current Bezier point, performs hit detection via
 * {@link btm.sword.util.entity.HitboxUtil#secant}, and draws particle effects.
 * </p>
 * <p>
 * The path function is generated once in {@link #generatePathFunction()} by capturing
 * the attacker's {@link Basis} at the moment {@link #startAttack()} is called.
 * Mid-swing rotation of the attacker does not affect the curve.
 * </p>
 * <p>
 * Attacks can be chained with {@link #setNextAttack(Attack, int)}.  The origin can be
 * fixed across a chain with {@link #setOriginOfAll(org.bukkit.Location)}.
 * </p>
 *
 * @see btm.sword.combat.style.AttackProfile
 * @see btm.sword.combat.hit.HitValuePacket
 * @see btm.sword.util.entity.HitboxUtil
 */
public class Attack extends SwordAction implements Runnable {
    @Getter
    protected Combatant attacker;
    protected ItemStack itemUsedInAttack;
    protected LivingEntity attackingEntity;
    protected final AttackProfile attackProfile;
    protected final boolean orientWithPitch;

    protected Function<Double, Vector> weaponPathFunction;

    protected Vector curRight;
    protected Vector curUp; // Reserved for future vertical knockback calculations
    protected Vector curForward; // Reserved for future forward knockback calculations

    protected Location origin;
    protected Location attackLocation; // current path vec + origin

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

    protected final double rangeMultiplier;

    protected Function<Combatant, Integer> attackDurationResolver;

    protected Runnable callback;
    protected int msBeforeCallbackSchedule;

    protected TimeArbiter.TaskHandle attackIterationTask;

    protected ConsumerToConsumePair<?>[] onAttackConnectInstructions = {};
    protected Consumer<SwordEntity> onEntityHitInstructions;

    @Getter
    protected Attack nextAttack;
    protected int millisecondDelayBeforeNextAttack;

    // TODO: maybe: have a caller-defined consumer that takes in a time-step and current vector (or just attack)
    //  and does display/sound stuff? a 'DisplayConsumer' that runs per iteration??

    /**
     * Constructs an {@code Attack} with timing values taken from the current config.
     *
     * @param itemUsedInAttack the item held during the attack (used for display and future item queries)
     * @param profile          the {@link AttackProfile} defining the sweep curve and knockback
     * @param orientWithPitch  if {@code true}, the attack basis includes the attacker's pitch;
     *                         if {@code false}, only yaw is used (horizontal sweep)
     */
    public Attack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch) {
        this.itemUsedInAttack = itemUsedInAttack;
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

    /**
     * Constructs an {@code Attack} with explicit timing overrides.
     *
     * @param itemUsedInAttack  the item held during the attack
     * @param profile           the {@link AttackProfile} defining the sweep curve and knockback
     * @param orientWithPitch   if {@code true}, basis includes pitch; if {@code false}, yaw only
     * @param attackMilliseconds total duration of the attack animation in milliseconds
     * @param attackIterations  number of discrete hit-detection steps across the sweep
     * @param attackStartValue  parametric start value fed into the path function (typically 0.0)
     * @param attackEndValue    parametric end value fed into the path function (typically 1.0)
     */
    public Attack(ItemStack itemUsedInAttack, AttackProfile profile, boolean orientWithPitch,
                  int attackMilliseconds, int attackIterations, double attackStartValue, double attackEndValue) {
        this(itemUsedInAttack, profile, orientWithPitch);
        this.attackMilliseconds = attackMilliseconds;
        this.attackIterations = attackIterations;
        this.attackStartValue = attackStartValue;
        this.attackEndValue = attackEndValue;
    }

    /**
     * Chains another {@link Attack} to execute automatically after this one completes.
     *
     * @param nextAttack                      the attack to execute after this one
     * @param millisecondDelayBeforeNextAttack delay in milliseconds between this attack ending and the next starting
     * @return this attack, for initialization chaining
     */
    public Attack setNextAttack(Attack nextAttack, int millisecondDelayBeforeNextAttack) {
        this.nextAttack = nextAttack;
        this.millisecondDelayBeforeNextAttack = millisecondDelayBeforeNextAttack;
        return this; // for initialization chaining
    }

    /**
     * Registers a callback to run after the attack completes, with an optional delay.
     *
     * @param callback                 the runnable to invoke after completion
     * @param msBeforeCallbackSchedule delay in milliseconds before the callback is scheduled
     * @return this attack, for chaining
     */
    public Attack setCallback(Runnable callback, int msBeforeCallbackSchedule) {
        this.callback = callback;
        this.msBeforeCallbackSchedule = msBeforeCallbackSchedule;
        return this;
    }

    /**
     * Sets instructions to run on every entity hit during this attack.
     * Each {@link btm.sword.util.misc.ConsumerToConsumePair} encapsulates a paired
     * consume action that fires when the sweep connects.
     *
     * @param onAttackConnectInstructions varargs of connect-time instructions
     * @return this attack, for chaining
     */
    public Attack setAttackConnectInstructions(ConsumerToConsumePair<?>... onAttackConnectInstructions) {
        this.onAttackConnectInstructions = onAttackConnectInstructions;
        return this;
    }

    /**
     * Sets a per-entity callback invoked each time a distinct entity is struck during the sweep.
     * Called after the standard {@link #hit()} logic for that entity.
     *
     * @param onEntityHitInstructions consumer receiving the struck {@link SwordEntity}
     * @return this attack, for chaining
     */
    public Attack setOnEntityHitInstructions(Consumer<SwordEntity> onEntityHitInstructions) {
        this.onEntityHitInstructions = onEntityHitInstructions;
        return this;
    }

    /**
     * Sets a dynamic resolver for the attack animation duration in milliseconds.
     * Evaluated at execution time with the attacking {@link Combatant}, allowing
     * the duration to scale with player stats, weapon type, or combo position.
     *
     * @param resolver function that computes animation duration from the attacker
     * @return this attack for chaining
     */
    public Attack setAttackDuration(Function<Combatant, Integer> resolver) {
        this.attackDurationResolver = resolver;
        return this;
    }

    /**
     * Returns {@code true} if a chained {@link Attack} will execute after this one completes.
     *
     * @return {@code true} if a next attack is configured
     */
    public boolean nextAttackExists() {
        return nextAttack != null;
    }

    /**
     * Begins execution of this attack for the given {@link Combatant}.
     * Sets up the attacker reference and entity filter, then calls {@link #cast()}.
     *
     * @param attacker the combatant performing the attack
     */
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

    protected void cast() {
        attacker.applyAttackCooldown();
        if (attackDurationResolver != null) {
            this.attackMilliseconds = attackDurationResolver.apply(attacker);
        }
        startAttack();
    }

    @Override
    public void run() {
        cast();
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
        Debug.attack("attackMilliseconds=" + attackMilliseconds);
        Debug.attack("msPerIteration=" + msPerIteration);

        generatePathFunction();
        determineOrigin();
        prev = weaponPathFunction.apply(attackStartValue - interpolationStep);
        startupLogic();

        curIteration.set(0);

        final long attackStartMs = System.currentTimeMillis();
        final long[] lastIterationMs = {attackStartMs};
        final int finalMsPerIteration = msPerIteration;

        attackIterationTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                long iterNow = System.currentTimeMillis();
                long actualDeltaMs = iterNow - lastIterationMs[0];
                long elapsedMs = iterNow - attackStartMs;
                int iter = curIteration.get();
                long expectedElapsedMs = (long) iter * finalMsPerIteration;
                Debug.attack("ITER " + iter
                    + " | actualDelta=" + actualDeltaMs + "ms (expected " + finalMsPerIteration + "ms)"
                    + " | elapsed=" + elapsedMs + "ms (expected " + expectedElapsedMs + "ms)"
                    + " | lag=" + (elapsedMs - expectedElapsedMs) + "ms");
                lastIterationMs[0] = iterNow;

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
                        SwordScheduler.runBukkitTaskLater(() -> nextAttack.execute(attacker),
                            millisecondDelayBeforeNextAttack, TimeUnit.MILLISECONDS
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

    /** Schedules the post-attack callback after the configured delay, if one is set. */
    public void handleCallback() {
        if (callback != null) {
            SwordScheduler.runBukkitTaskLater(callback, msBeforeCallbackSchedule, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Fixes the attack origin for this attack and all chained attacks to the given location.
     * Useful for anchoring a combo to the position where it started.
     *
     * @param origin the fixed world-space origin to propagate through the chain
     * @return this attack, for chaining
     */
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

    /**
     * Overrides the origin for this attack only.
     * If {@code null}, the origin is determined from the attacker's chest position at swing start.
     *
     * @param origin the world-space origin to use, or {@code null} to use the attacker's position
     * @return this attack, for chaining
     */
    public Attack setOrigin(@Nullable Location origin) {
        this.origin = origin;
        return this;
    }

    // TODO: #128 - Make Particle Effects more dynamic. Low prio.
    //  Maybe we just put particles to display in the AttackType or Attack profile?
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
        currentTarget.hit(attacker, Prefab.Attacks.BASIC_ATTACK,
            attackProfile.knockbackFunction().apply(currentTarget).apply(this));

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
            new ParticleWrapper(() -> Particle.BLOCK, () -> Config.Combat.GROUND_HIT_PARTICLE_COUNT,
                    () -> Config.Combat.GROUND_HIT_PARTICLE_OFFSET, () -> Config.Combat.GROUND_HIT_PARTICLE_OFFSET,
                    () -> Config.Combat.GROUND_HIT_PARTICLE_OFFSET)
                    .withBlockData(() -> Objects.requireNonNull(result.getHitBlock()).getBlockData())
                    .display(attackLocation);
            Prefab.Particles.COLLIDE.display(attackLocation);

            // potential reduction of damage formula
        }
        else if (direction.lengthSquared() > (double) 2 / (attackIterations * attackIterations)) {
            // interpolated particle, same as normal particle
            // 0.5 - half the length to the particle
            Prefab.Particles.TEST_SWING.display(attackLocation.clone().add(direction.multiply(0.5)));
        }
    }

    /**
     * Captures the attacker's current {@link Basis} and resolves the profile's
     * {@link btm.sword.combat.style.AttackShape} into the weapon path function.
     * Called once at attack start before the iteration loop begins.
     */
    void generatePathFunction() {
        Basis basis = orientWithPitch ?
                VectorUtil.getBasis(
                    origin == null ? attackingEntity.getEyeLocation() : origin,
                    origin == null ? attackingEntity.getEyeLocation().getDirection() : origin.getDirection()) :
                VectorUtil.getBasisWithoutPitch(origin == null ? attackingEntity.getEyeLocation() : origin);
        curRight = basis.right();
        curUp = basis.up();
        curForward = basis.forward();

        weaponPathFunction = attackProfile.shape().resolve(basis, rangeMultiplier);
    }

    /** Returns a clone of the current weapon tip position in world space. */
    public Vector getCur() {
        return cur.clone();
    }

    /** Returns a clone of the previous weapon tip position in world space. */
    public Vector getPrev() {
        return prev.clone();
    }

    /** Returns a clone of the target weapon tip position for the current iteration. */
    public Vector getTo() {
        return to.clone();
    }

    /** Returns a clone of the current right basis vector in world space. */
    public Vector getRightVector() {
        return curRight.clone();
    }

    /** Returns a clone of the current forward basis vector in world space. */
    public Vector getForwardVector() {
        return curForward.clone();
    }

    /** Returns a clone of the current up basis vector in world space. */
    public Vector getUpVector() {
        return curUp.clone();
    }
}
