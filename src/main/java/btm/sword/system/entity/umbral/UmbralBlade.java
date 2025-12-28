package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.config.Config;
import btm.sword.system.action.movement.DashDirection;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.GeneratedAttackProfile;
import btm.sword.system.attack.UmbralBladeAttack;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.input.InputBuffer;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateMachine;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.FinisherState;
import btm.sword.system.entity.umbral.statemachine.state.GrabImpaleState;
import btm.sword.system.entity.umbral.statemachine.state.InactiveState;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.LungingState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.RecoverState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.entity.umbral.statemachine.state.WaitingState;
import btm.sword.system.entity.umbral.statemachine.state.WieldState;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DisplayUtil;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.BezierUtil;
import btm.sword.utility.math.ControlVectors;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.statemachine.State;
import btm.sword.utility.statemachine.Transition;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit
@Getter
@Setter
public class UmbralBlade extends ThrownItem {
    private UmbralStateMachine bladeStateMachine;

    private Function<Combatant, Attack>[] basicAttacks;
    private int currentComboStep = -1;

    private ItemStack link;
    private ItemStack blade;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    protected static final int MAX_STATIONARY_ITERATIONS_WHILE_RETURNING = 4;
    protected static final double EPSILON_SQUARED = 0.004;

    private static final int idleMovementPeriod = 150;
    private static final float idleMovementAmplitude = 0.25f;
    private TimeArbiter.TaskHandle idleMovementTask;

    private final Predicate<UmbralBlade> endHoverPredicate;
    private final Runnable attackEndCallback;
    private boolean attackCompleted = false;

    private ControlVectors ctrlPointsForLunge;
    private boolean finishedLunging = false;

    boolean usingCustomFuncs;

    private boolean skillFinished;

    private DashDirection dashDirection = DashDirection.NONE;

    private final InputBuffer inputBuffer = new InputBuffer();

    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        super(thrower, display -> {
            display.setItemStack(weapon);
            display.setTransformation(new Transformation(
                    new Vector3f(0.28f, -1.35f, -0.5f),
                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                    new Vector3f(0.85f, 1.3f, 1f),
                    new Quaternionf()
            ));
            display.setPersistent(false);

            thrower.self().addPassenger(display);
            display.setBillboard(Display.Billboard.FIXED);
        }, 5);

        this.weapon = weapon;

        generateUmbralItems();

        this.attackEndCallback = () -> attackCompleted = true;

        loadBasicAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        initStateMachine();

        exitImpalementStatePredicate = blade -> !inState(LodgedState.class);

        endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
    }

    private void initStateMachine() {
        // =====================================================================
        // UNIVERSAL — wildcard transitions
        // =====================================================================

        // 1) Enter inactive from ANYTHING
        bladeStateMachine.addTransition(new Transition<>(
            UmbralStateFacade.class,
            InactiveState.class,
            blade -> (thrower.self() instanceof SwordPlayer sp &&
                sp.player().getGameMode().equals(GameMode.SPECTATOR)) ||
                isRequested(BladeRequest.DEACTIVATE),
            blade -> {}
        ));

        // 2) Enter recover from ANYTHING when display is invalid
        bladeStateMachine.addTransition(new Transition<>(
            UmbralStateFacade.class,
            RecoverState.class,
            blade -> blade.getDisplay() == null || blade.display.isDead() || !blade.display.isValid(),
            blade -> {}
        ));

        // 3) Reactivate to last state
        bladeStateMachine.addTransition(new Transition<>(
            InactiveState.class,
            StandbyState.class,
            blade -> isRequested(BladeRequest.ACTIVATE_TO_PREVIOUS),
            blade -> {}
        ));

        // 4) Recover and go back to last state
        bladeStateMachine.addTransition(new Transition<>(
            RecoverState.class,
            StandbyState.class,
            blade -> (display != null && !display.isDead() && display.isValid()) ||
                isRequested(BladeRequest.RESUME_FROM_REPAIR),
            blade -> { }
        ));

        // =====================================================================
        // SHEATHED
        // =====================================================================
        // TODO: add a charged slash skill while sheathed
        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            WieldState.class,
            blade -> isRequestedAndActive(BladeRequest.WIELD),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            AttackingQuickState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            AttackingHeavyState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            LungingState.class,
            blade -> isRequestedAndActive(BladeRequest.LUNGE),
            blade -> {}
        ));

        // =====================================================================
        // STANDBY
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            SheathedState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            WieldState.class,
            blade -> isRequestedAndActive(BladeRequest.WIELD),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            AttackingQuickState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            AttackingHeavyState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            LungingState.class,
            blade -> isRequestedAndActive(BladeRequest.LUNGE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            GrabImpaleState.class,
            blade -> isRequestedAndActive(BladeRequest.GRAB_IMPALE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            FinisherState.class,
            blade -> isRequestedAndActive(BladeRequest.FINISHER),
            blade -> {}
        ));


        // =====================================================================
        // FINISHER
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            FinisherState.class,
            RecallingState.class,
            blade -> isSkillFinished() || isRequestedAndActive(BladeRequest.STANDBY),
            blade -> setSkillFinished(false)
        ));

        // =====================================================================
        // WIELD
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            WieldState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));


        // =====================================================================
        // ATTACKING (Quick + Heavy)
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            AttackingQuickState.class,
            RecallingState.class,
            blade -> blade.attackCompleted,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            AttackingHeavyState.class,
            RecallingState.class,
            blade -> blade.attackCompleted,
            blade -> {}
        ));

        // =====================================================================
        // GRAB IMPALE
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            GrabImpaleState.class,
            LodgedState.class,
            UmbralBlade::hitTargetWithLunge,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            GrabImpaleState.class,
            RecallingState.class,
            blade -> finishedLunging,
            blade -> {}
        ));


        // =====================================================================
        // WAITING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            WaitingState.class,
            StandbyState.class,
            blade -> true, //!blade.shouldReturn(), // immediate fallback
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            WaitingState.class,
            RecallingState.class,
            UmbralBlade::isTooFarOrIdleTooLong,
            blade -> {}
        ));


        // =====================================================================
        // RECALLING / RETURNING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            SheathedState.class,
            blade -> isRequestedAndActive(BladeRequest.SHEATH),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.STANDBY),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            LungingState.class,
            blade -> isRequestedAndActive(BladeRequest.LUNGE), // TODO: #122 - Test this transition
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            AttackingQuickState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            AttackingHeavyState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            blade -> {}
        ));

        // =====================================================================
        // LODGED
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            RecallingState.class,
            blade ->
                hitEntity == null ||
                hitEntity.isInvalid() ||
                isRequestedAndActive(BladeRequest.RECALL),
            blade -> {
                Location bladeLoc = blade.getDisplay().getLocation();
                TimeArbiter.teleportDisplay(blade.getDisplay(),
                    bladeLoc.clone().subtract(
                    bladeLoc.getDirection().multiply(6)),
                    null, 10);

                if (hitEntity != null) {
                    hitEntity.setVelocity(blade.getDisplay().getLocation().getDirection().multiply(-0.75));
                }
            }
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            WaitingState.class,
            blade -> false,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            WieldState.class,
            blade -> isRequestedAndActive(BladeRequest.WIELD),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.STANDBY),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            AttackingHeavyState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            blade -> {}
        ));

        // =====================================================================
        // LUNGING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            LungingState.class,
            LodgedState.class,
            UmbralBlade::hitTargetWithLunge,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LungingState.class,
            RecallingState.class,
            blade -> finishedLunging,
            blade -> {}
        ));
    }

    private boolean hitTargetWithLunge() {
        return hitEntity != null;
    }

    public void request(BladeRequest request) {
        inputBuffer.push(request);
    }

    public boolean isRequested(BladeRequest request) {
        return inputBuffer.consumeIfPresent(request);
    }

    public boolean isRequestedAndActive(BladeRequest request) {
        return isRequested(request) && !inState(InactiveState.class);
    }

    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId().equals(thrower.getUniqueId());
    }

    public boolean inState(Class<? extends State<UmbralBlade>> clazz) {
        return bladeStateMachine.getState().getClass().equals(clazz);
    }

    public void onTick() {
        if (thrower.isInvalid()) {
//            thrower.message("Ending Umbral Blade");
            dispose();
        }

        if (bladeStateMachine != null)
            bladeStateMachine.tick();
    }

    // TODO: #121 - Make a method for calculating correct orientation of blade for edge to align with plane of swing on attack
    // TODO: #121 - Make transitions smooth and slow with arcs and such and interpolation
    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (display == null) return;

        SwordScheduler.runBukkitTaskLater(() -> {
            DisplayUtil.setInterpolationValues(display, 0, 2); // TODO: #119 - Make duration dynamic if needed
            display.setTransformation(getStateDisplayTransformation(state));
            }, 50, TimeUnit.MILLISECONDS
        );
    }

    public Transformation getStateDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (state == SheathedState.class) {
            return new Transformation(
                new Vector3f(0.28f, -1.35f, -0.42f),
                new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / 1.65f),
                scale,
                new Quaternionf());
        }
        else if (state == StandbyState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotationY(0).rotateZ((float) Math.PI),
                scale,
                new Quaternionf());
        }
        else if (state == RecallingState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) -Math.PI/2),
                scale,
                new Quaternionf());
        }
        else if (state == LungingState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) Math.PI/2),
                scale,
                new Quaternionf()
            );
        }
        else if (state == GrabImpaleState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) -Math.PI/2),
                scale,
                new Quaternionf()
            );
        }
        else if (state == LodgedState.class) {
            return display.getTransformation();
        }
        else if (state == AttackingQuickState.class || state == AttackingHeavyState.class) {
            return new Transformation(
                new Vector3f(0, 0, -1),
                new Quaternionf().rotateX((float) Math.PI/2),
                scale,
                new Quaternionf());
        }
        else {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI),
                scale,
                new Quaternionf());
        }
    }

    public TimeArbiter.TaskHandle hoverBehindWielder() {
        if (!inState(StandbyState.class)) return null;
        // Play unsheathing animation

        // follows player shoulder position smoothly
        return DisplayUtil.itemDisplayFollowLerp(thrower, display,
            new Vector(0.7, 0.7, -0.5),
            5, 150, false);
    }

    public void registerAsInteractableItem() {
        InteractiveItemArbiter.put(this);
    }

    public void unregisterAsInteractableItem() {
        InteractiveItemArbiter.remove(display, false);
    }

    public void updateSheathedPosition() {
        if (inState(WaitingState.class)) return;
        int x = 3;
        for (int i = 0; i < x; i++) {
            SwordScheduler.runBukkitTaskLater(() -> {
                    TimeArbiter.teleportDisplay(
                        display,
                        thrower.self().getLocation(), thrower.getFlatDir(),
                        2
                    );
                    thrower.self().addPassenger(display);
            }, 50/x, TimeUnit.MILLISECONDS);
        }
    }

    public void startIdleMovement() {
        if (idleMovementTask != null) {
            idleMovementTask.cancel();
        }
        final double[] step = {0};
        idleMovementTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                TimeArbiter.setDisplayTransformation(display,
                    new Transformation(
                        new Vector3f(0, (float) Math.cos(step[0]) * idleMovementAmplitude, 0),
                        display.getTransformation().getLeftRotation(),
                        scale,
                        new Quaternionf()
                    ),
                    idleMovementPeriod
                );

                step[0] += Math.PI/8;
            },
            null,
            null,
            0, idleMovementPeriod,
            UmbralBlade.class, "startIdleMovement"
        );
    }

    public void endIdleMovement() {
        if (idleMovementTask != null) {
            idleMovementTask.cancel();
        }
    }

    // TODO: #121 - Make item Display changes look less jerky

    public TimeArbiter.TaskHandle returnToWielderAndRequestState(BladeRequest request) {

        AtomicReference<Location> currentBladeLoc = new AtomicReference<>(display.getLocation());
        AtomicReference<Location> previousBladeLoc = new AtomicReference<>(display.getLocation());

        final int[] stationaryCount = {0};
        return DisplayUtil.displaySlerpToOffset(thrower, display,
            thrower.getChestVector(),
            1.5, 5, 100, 1.5,
            false,
            500, // give it 10 seconds to get back
            () -> {
                currentBladeLoc.set(display.getLocation());

                Vector difference = currentBladeLoc.get().toVector().subtract(previousBladeLoc.get().toVector());
                if (difference.lengthSquared() < EPSILON_SQUARED) {
                    stationaryCount[0]++;
                } else {
                    stationaryCount[0] = 0;
                }

                previousBladeLoc.set(currentBladeLoc.get());
                return stationaryCount[0] > MAX_STATIONARY_ITERATIONS_WHILE_RETURNING;
            },
            () -> request(request)
        );
    }

    @Override
    public void groundedCheck() {
        RayTraceResult hitBlock = display.getWorld()
            .rayTraceBlocks(prev, velocity, // detect from prev, checks go through later, which is what I need to happen.
                cur.toVector()
                    .subtract(prev.toVector())
                    .lengthSquared() * Config.Detection.THROW_GROUND_CHECK_MULTIPLIER / 10,
                FluidCollisionMode.NEVER, true);

        if (hitBlock == null) {
            return;
        }

        if (hitBlock.getHitBlock() == null || hitBlock.getHitBlock().getType().isAir())
            return;

        grounded = true;
        stuckBlock = hitBlock.getHitBlock();
        cur = hitBlock.getHitPosition().toLocation(display.getWorld());
    }

    public void performSimpleAttack(double range) {
        if (isDashing()) {
            umbralDashAttack(dashDirection);
            return;
        }
        SwordEntity target = thrower.getTargetedEntity(range);
        Attack attack;
        Location attackOrigin;

        if (target == null || target.isInvalid()) {
            attackOrigin = thrower.getChestLocation().clone()
                .add(thrower.dir().multiply(range));
        }
        else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), display.getLocation(), target.getChestLocation(), 0.5);
            // From the bladeDisplay TO the target
            Vector to = target.getChestLocation().toVector()
                .subtract(display.getLocation().toVector());

            attackOrigin = target.getChestLocation().clone()
                .subtract(to.normalize()).setDirection(to.normalize());
        }

        attack = basicAttacks[
            // use currentComboStep to get attack
            Math.max(0, Math.min(currentComboStep - 1, basicAttacks.length - 1))
            ].apply(thrower);

        attack.setOriginOfAll(attackOrigin).execute(thrower);
    }

    private void umbralDashAttack(DashDirection direction) {
        AttackType type;
        switch (direction) {
            case FORWARD -> type = AttackType.F_DASH_ATTACK;
            case BACKWARD -> type = AttackType.B_DASH_ATTACK;
            default -> type = AttackType.WIDE_UMBRAL_SLASH3;
        }
        new UmbralBladeAttack(display, type,
            direction.equals(DashDirection.FORWARD), false, 1,
            5, 10, 200,
            0, 1)
            .setBlade(this)
            .setInitialMovementTicks(1)
            .setCallback(attackEndCallback, 200)
            .execute(thrower);
    }

    public void performWideUmbralSweepAttack(double range) {
        SwordEntity target = thrower.getTargetedEntity(range);
        Location attackOrigin = display.getLocation();
        Location targetedLocation;
        Vector toTarget;
        Vector start;
        Vector end;
        Vector c1;
        Vector c2;
        Basis attackFrame;
        double dist;

        double MIN_SWEEP_DISTANCE = 5.0;

        int direction;

        if (target == null || target.isInvalid()) {
            targetedLocation = thrower.getChestLocation().add(thrower.dir().multiply(range));
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), attackOrigin, targetedLocation, 0.25);
        }
        else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), display.getLocation(), target.getChestLocation(), 0.5);
            targetedLocation = target.getChestLocation();
        }

        toTarget = targetedLocation.toVector().subtract(display.getLocation().toVector());
        direction = thrower.rightBasisVector(false).dot(toTarget) > 0 ? -1: 1;

        attackOrigin.setDirection(toTarget);
        attackFrame = new Basis(attackOrigin, true);

        double calculatedDistance = targetedLocation.toVector().subtract(attackOrigin.toVector()).length();
        double diffBetweenCalculatedDistAndMinDist = calculatedDistance - MIN_SWEEP_DISTANCE;
        if (diffBetweenCalculatedDistAndMinDist < 0) {
            attackOrigin.add(toTarget.normalize().multiply(diffBetweenCalculatedDistAndMinDist));
        }
        dist = Math.max(calculatedDistance, MIN_SWEEP_DISTANCE);

        start = attackFrame.forward().multiply(-dist/10)
            .add(attackFrame.right().multiply(direction * dist/5));
        c1 = attackOrigin.clone()
            .add(attackFrame.forward().multiply(dist/5))
            .add(attackFrame.right().multiply(direction * dist/3))
            .toVector()
            .subtract(attackOrigin.toVector());
        c2 = targetedLocation.clone()
            .add(attackFrame.forward().multiply(-dist/5))
            .add(attackFrame.right().multiply(direction * dist/3))
            .toVector()
            .subtract(attackOrigin.toVector());
        end = targetedLocation.clone()
            .add(attackFrame.forward().multiply(dist/2))
            .add(attackFrame.right().multiply(-direction * dist/3))
            .toVector()
            .subtract(attackOrigin.toVector());

        ControlVectors ctrl = new ControlVectors(start, end, c1, c2);
        GeneratedAttackProfile profile = new GeneratedAttackProfile(ctrl, Attack::getTo);

        int duration = 20 * (int) Math.log(Math.max(1, dist * dist));

        Attack attack = new UmbralBladeAttack(display, profile,
            true, true, 1,
            30, 1, (int) (duration * 1.75),
            0.2, -0.1)
            .setBlade(this)
            .setInitialMovementTicks(5)
            .setDrawParticles(false)
            .setNextAttack(
                new UmbralBladeAttack(display, profile,
                    true, false, 0,
                    (int) (dist * 2), 10, duration/2,
                    0, 1)
                    .setBlade(this)
                    .setOnEntityHitInstructions(
                        swordEntity ->
                            Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                    .setCallback(attackEndCallback, 200),
                100);

        attack.setOriginOfAll(attackOrigin).execute(thrower);
    }

    private boolean isTooFarOrIdleTooLong() {
        if (display == null) return false;
        double distance = thrower.self().getLocation().distance(display.getLocation());
        long timeSinceLastAction = System.currentTimeMillis() - lastActionTime;
        return distance > 20.0 || timeSinceLastAction > 30000;
    }

    @SuppressWarnings("unchecked")
    private void loadBasicAttacks() {
        basicAttacks = new Function[]{
            // TODO: #120 - Fix how display step and attack steps work, confusing and incorrect rn
            combatant -> new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH1_WINDUP,
                true, true, 1,
                10, 30, 500,
                0, 1)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH1,
                        true, false, 0,
                        20, 10, 100,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    100),

            combatant -> new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH2_WINDUP,
                true, true, 2,
                10, 30, 500,
                0, 1)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH2,
                        true, false, 0,
                        20, 10, 100,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    100),

            combatant -> new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH3_WINDUP,
                false, true, 3,
                10, 30, 500,
                0, 1.2)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(display, AttackType.WIDE_UMBRAL_SLASH3,
                        false, false, 0,
                        20, 10, 50,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    250)
        };
    }

    private void generateUmbralItems() {
        // item Stack used for determining umbral blade inputs
        this.link = new ItemStackBuilder(Material.HEAVY_CORE)
            .hideAll()
            .name(Component.text("~ ", Config.SwordColor.TEXT_ITEM_NAME)
                .append(Component.text(thrower.getDisplayName() + "'s Soul Link",
                    Config.SwordColor.TEXT_ITEM_NAME, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_ITEM_NAME)))
            .lore(Prefab.Text.SOUL_LINK_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, PersistentDataType.STRING, thrower.getUniqueId().toString())
            .tagSwordItem(SwordItemType.UMBRAL_LINK)
            .build();

        this.blade = new ItemStackBuilder(weapon.getType())
            .hideAll()
            .name(Component.text("~ ", Config.SwordColor.TEXT_COOL_DARK)
                .append(Component.text(thrower.getDisplayName() + "'s Blade",
                    Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_COOL_DARK)))
            .lore(Prefab.Text.UMBRAL_BLADE_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.UMBRAL_BLADE_KEY, PersistentDataType.STRING, thrower.getUniqueId().toString())
            .tagSwordItem(SwordItemType.UMBRAL_BLADE)
            .tagAttackStyle(WeaponAttackStyle.SLASH)
            .build();
    }

    public void resetWeaponDisplay() {
        if (display != null) {
            display.remove();
            display = null;
        }

        LivingEntity e = thrower.self();
        display = (ItemDisplay) e.getWorld().spawnEntity(e.getEyeLocation(), EntityType.ITEM_DISPLAY);
        displaySetupInstructions.accept(display);
    }

    @Override
    public void generateFunctions(double initialVelocity) {
        if (ctrlPointsForLunge == null) {
            super.generateFunctions(initialVelocity);
        }
        else {
            calcBezierTrajectory();
        }
    }

    protected void calcBezierTrajectory() {
        SwordEntity target = inState(GrabImpaleState.class) ?
            getThrower().getGrabbedEntity() :
            thrower.getTargetedEntity(20);

        origin = display.getLocation();
        cur = origin.clone();
        prev = cur.clone();

        Vector dir;
        if (target == null) {
            Location intent = thrower.locFromEyeDir(20);
            dir = intent.toVector().subtract(display.getLocation().toVector());
        }
        else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), display.getLocation(), target.getChestLocation(), 0.5);

            dir = target.getChestLocation().toVector().subtract(display.getLocation().toVector());
        }
        this.currentBasis = VectorUtil.getBasis(display.getLocation().setDirection(dir), dir);

        ControlVectors adjusted = ctrlPointsForLunge.adjustToBasis(currentBasis, 1);
        this.positionFunction = BezierUtil.cubicBezier3D(adjusted);
        this.velocityFunction = t -> dir.multiply(0.5);
    }

    public void onGrab(Combatant combatant) {
        if (!isOwnedBy(combatant)) {
            // TODO: #122 - Add rejection logic for non-thrower grabs
            return;
        }

        if (combatant.holdingUmbralItemInMainHand()) {
            if (inState(LungingState.class) ||
                inState(AttackingHeavyState.class) ||
                inState(AttackingQuickState.class)) {
                thrower.setVelocity(thrower.self().getVelocity().add(to.clone().multiply(1.5)));
            }

            request(BladeRequest.WIELD);
        }
        else {
            request(BladeRequest.STANDBY);

            // Do a spinning attack like katarina?
        }
    }

    @Override
    protected void teleport() {
        TimeArbiter.teleportDisplay(display, cur, to, 2);
    }

    @Override
    protected void onCatch() {
        // No action on catch.
    }

    @Override
    protected void onGrounded() {
        if (stuckBlock != null) {
            this.blockDustPillarParticle = new ParticleWrapper(Particle.DUST_PILLAR, 50, 1, 1, 1, stuckBlock.getBlockData());
            blockDustPillarParticle.display(cur);
        }
        request(BladeRequest.RECALL);
    }

    @Override
    protected void onEnd() {
        super.onEnd();
        finishedLunging = true;
        cleanupBeforeNewThrow();
    }

    @Override
    protected void handleOnReleaseActions() {
        Prefab.Sounds.THROW.playForAllInRadius(getThrower().self());
    }

    @Override
    public void handleItemDamageAndCheckIfBroken() {}

    @Override
    public void disposeWithNewInteractiveItem() {
        request(BladeRequest.RECALL);
    }

    @Override
    public void dispose() {
        super.dispose();
        bladeStateMachine.setDeactivated(true);
    }

    public void cleanupBeforeNewThrow() {
        hit = false;
        grounded = false;
        caught = false;
        stuckBlock = null;
        timeScalingFactor = -1;
    }

    public void setDashingDirection(DashDirection direction) {
        this.dashDirection = direction;

        SwordScheduler.runBukkitTaskLater(
            () -> dashDirection = DashDirection.NONE,
            250, TimeUnit.MILLISECONDS
        );
    }

    public boolean isDashing() {
        return !dashDirection.equals(DashDirection.NONE);
    }

    public void requestQuickAttack(int comboStep) {
        currentComboStep = comboStep;
        request(BladeRequest.ATTACK_QUICK);
    }
}
