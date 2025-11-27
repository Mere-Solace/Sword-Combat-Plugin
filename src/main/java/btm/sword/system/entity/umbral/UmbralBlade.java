package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.AttackType;
import btm.sword.system.attack.GeneratedAttackProfile;
import btm.sword.system.attack.UmbralBladeAttack;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.input.InputBuffer;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateMachine;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.FinisherState;
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
import btm.sword.system.statemachine.State;
import btm.sword.system.statemachine.Transition;
import btm.sword.util.Prefab;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.math.Basis;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;
import btm.sword.util.math.VectorUtil;
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
    private Function<Combatant, Attack>[] heavyAttacks;

    private ItemStack link;
    private ItemStack blade;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    private static final int idleMovementPeriod = 3;
    private static final float idleMovementAmplitude = 0.25f;
    private BukkitTask idleMovement;

    private final Predicate<UmbralBlade> endHoverPredicate;
    private final Runnable attackEndCallback;
    private boolean attackCompleted = false;

    private ControlVectors ctrlPointsForLunge;
    private boolean finishedLunging = false;

    private boolean skillFinished;

    private boolean dashingForward;
    private boolean dashingBackward;

    private final InputBuffer inputBuffer = new InputBuffer();

    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        super(thrower, display -> {
            display.setItemStack(weapon);
            display.setTransformation(new Transformation(
                    new Vector3f(0.28f, -1.35f, -0.42f),
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
        loadHeavyAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        initStateMachine();

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
                DisplayUtil.smoothTeleport(blade.getDisplay(), 10);
                blade.getDisplay().teleport(
                    blade.getDisplay().getLocation().subtract(
                        blade.getDisplay().getLocation().getDirection().multiply(6)));

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
        return combatant.getUniqueId() == thrower.getUniqueId();
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
    // TODO: #121 - Make transitions smooth af and slow with arcs and such and interpolation
    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (display == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                DisplayUtil.setInterpolationValues(display, 0, 2); // TODO: #119 - Make duration dynamic
                display.setTransformation(getStateDisplayTransformation(state));
            }
        }.runTaskLater(Sword.getInstance(), 1L);
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

    public BukkitTask hoverBehindWielder() {
        // Play unsheathing animation

        // follows player shoulder position smoothly
        return DisplayUtil.itemDisplayFollowLerp(thrower, display,
            new Vector(0.7, 0.7, -0.5),
            5, 3, false);
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
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    DisplayUtil.smoothTeleport(display, 2);
                    display.teleport(thrower.self().getLocation().setDirection(thrower.getFlatDir()));
                    thrower.self().addPassenger(display);
                }
            }, 50/x, TimeUnit.MILLISECONDS);
        }
    }

    public void startIdleMovement() {
        idleMovement = new BukkitRunnable() {
            double step = 0;
            @Override
            public void run() {
                DisplayUtil.setInterpolationValues(display, 0, idleMovementPeriod);
                display.setTransformation(
                    new Transformation(
                        new Vector3f(0, (float) Math.cos(step) * idleMovementAmplitude, 0),
                        display.getTransformation().getLeftRotation(),
                        scale,
                        new Quaternionf()
                    )
                );

                step += Math.PI/8;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, idleMovementPeriod);
    }

    public void endIdleMovement() {
        if (idleMovement != null && !idleMovement.isCancelled()) {
            idleMovement.cancel();
            idleMovement = null;
        }
    }

    // TODO: #121 - Make item Display changes look less jerky

    public BukkitTask returnToWielderAndRequestState(BladeRequest request) {
        return DisplayUtil.displaySlerpToOffset(thrower, display,
            thrower.getChestVector(), 1.75, 5, 2, 1.5, false,
            new BukkitRunnable() {
                @Override
                public void run() {
                    request(request);
                }
            });
    }

    public void performSimpleAttack(double range) {
        if (isDashing()) {
            umbralDashAttack(dashingForward);
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

        int attackNumber = ThreadLocalRandom.current().nextInt(basicAttacks.length);
        attack = basicAttacks[attackNumber].apply(thrower);

        attack.setOriginOfAll(attackOrigin).execute(thrower);
    }

    private void umbralDashAttack(boolean forward) {
        AttackType type = forward ? AttackType.F_DASH_ATTACK : AttackType.B_DASH_ATTACK;
        new UmbralBladeAttack(display, type,
            forward, false, 1,
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
        Vector start;
        Vector end;
        Vector c1;
        Vector c2;
        Basis attackFrame;
        double dist;

        int direction;

        if (target == null || target.isInvalid()) {
            Location targeted = thrower.getChestLocation().add(thrower.dir().multiply(range));
            Vector to = targeted.toVector().subtract(display.getLocation().toVector());
            direction = thrower.rightBasisVector(false).dot(to) > 0 ? -1: 1;

            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), attackOrigin, targeted, 0.25);

            attackOrigin.setDirection(to);
            attackFrame = new Basis(attackOrigin, true);

            dist = targeted.toVector().subtract(attackOrigin.toVector()).length();

            start = attackFrame.forward().multiply(-dist/10)
                .add(attackFrame.right().multiply(direction * dist/5));
            c1 = attackOrigin.clone()
                .add(attackFrame.forward().multiply(dist/5))
                .add(attackFrame.right().multiply(direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
            c2 = targeted.clone()
                .add(attackFrame.forward().multiply(-dist/5))
                .add(attackFrame.right().multiply(direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
            end = targeted.clone()
                .add(attackFrame.forward().multiply(dist/2))
                .add(attackFrame.right().multiply(-direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
        }
        else {
            DrawUtil.secant(List.of(Prefab.Particles.TEST_SPARKLE), display.getLocation(), target.getChestLocation(), 0.5);

            Location targeted = target.getChestLocation();
            Vector to = targeted.toVector().subtract(display.getLocation().toVector());
            direction = thrower.rightBasisVector(false).dot(to) > 0 ? -1: 1;

            attackOrigin.setDirection(to);
            attackFrame = new Basis(attackOrigin, true);

            dist = targeted.toVector().subtract(attackOrigin.toVector()).length();
            start = attackFrame.forward().multiply(-dist/10)
                .add(attackFrame.right().multiply(direction * dist/5));
            c1 = attackOrigin.clone()
                .add(attackFrame.forward().multiply(dist/5))
                .add(attackFrame.right().multiply(direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
            c2 = targeted.clone()
                .add(attackFrame.forward().multiply(-dist/5))
                .add(attackFrame.right().multiply(direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
            end = targeted.clone()
                .add(attackFrame.forward().multiply(dist/2))
                .add(attackFrame.right().multiply(-direction * dist/3))
                .toVector()
                .subtract(attackOrigin.toVector());
        }

        ControlVectors ctrl = new ControlVectors(start, end, c1, c2);
        GeneratedAttackProfile profile = new GeneratedAttackProfile(ctrl, Attack::getTo);

        int duration = 40 * (int) Math.log(Math.max(1, dist * dist));

        Attack attack = new UmbralBladeAttack(display, profile,
            true, true, 1,
            10, 30, (int) (duration * 1.75),
            0.2, -0.1)
            .setBlade(this)
            .setInitialMovementTicks(5)
            .setDrawParticles(false)
            .setNextAttack(
                new UmbralBladeAttack(display, profile,
                    true, false, 0,
                    30, 10, duration,
                    0, 1)
                    .setBlade(this)
                    .setHitInstructions(
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
        // load from config or registry later
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
                        .setHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    100)

        };
    }

    private void loadHeavyAttacks() {

    }

    private void generateUmbralItems() {
        // item Stack used for determining umbral blade inputs
        this.link = new ItemStackBuilder(Material.HEAVY_CORE)
            .name(Component.text("~ ", Config.SwordColor.TEXT_ITEM_NAME)
                .append(Component.text(thrower.getDisplayName() + "'s Soul Link",
                    Config.SwordColor.TEXT_ITEM_NAME, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_ITEM_NAME)))
            .lore(Prefab.Text.SOUL_LINK_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, thrower.getUniqueId().toString())
            .hideAll()
            .build();

        this.blade = new ItemStackBuilder(weapon.getType())
            .name(Component.text("~ ", Config.SwordColor.TEXT_COOL_DARK)
                .append(Component.text(thrower.getDisplayName() + "'s Blade",
                    Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_COOL_DARK)))
            .lore(Prefab.Text.UMBRAL_BLADE_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.UMBRAL_BLADE_KEY, thrower.getUniqueId().toString())
            .hideAll()
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
    protected void generateFunctions(double initialVelocity) {
        if (ctrlPointsForLunge == null) {
            super.generateFunctions(initialVelocity);
        }
        else {
            calcBezierTrajectory();
        }
    }

    protected void calcBezierTrajectory() {
        SwordEntity target = thrower.getTargetedEntity(20);

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
        if (combatant.getUniqueId() != thrower.getUniqueId()) {
            // TODO: #122 - Add rejection logic for non-thrower grabs
            return;
        }

        thrower.message("Grabbed it!");

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
    public void impale(LivingEntity hit) {
        hitEntity.addImpalement();

        double max = hit.getEyeLocation().getY();
        double feet = hit.getLocation().getY();
        double diff = max - feet;

        double heightOffset = Math.max(0, Math.min(cur.getY() - feet, hit.getHeight()));

        boolean followHead = !Config.Combat.IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS.contains(hitEntity.type())
            && heightOffset >= diff * Config.Combat.IMPALEMENT_HEAD_ZONE_RATIO;
        DisplayUtil.itemDisplayFollow(hitEntity, display,  velocity.clone().normalize(), heightOffset, followHead,
            blade -> !inState(LodgedState.class), this, null, null);
    }

    @Override
    protected void teleport() {
        display.teleport(cur.setDirection(to));
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
    public void disposeNaturally() {
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
    }

    public void setDashingDirection(boolean forward) {
        dashingForward = forward;
        dashingBackward = !forward;

        new BukkitRunnable() {
            @Override
            public void run() {
                dashingForward = false;
                dashingBackward = false;
            }
        }.runTaskLater(Sword.getInstance(), 10L);
    }

    public boolean isDashing() {
        return dashingForward || dashingBackward;
    }
}
