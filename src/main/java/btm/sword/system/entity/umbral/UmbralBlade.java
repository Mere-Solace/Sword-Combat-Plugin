package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
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
import btm.sword.system.attack.UmbralBladeAttack;
import btm.sword.system.attack.style.AttackType;
import btm.sword.system.attack.style.WeaponAttackStyle;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.input.InputBuffer;
import btm.sword.system.entity.umbral.statemachine.UmbralStateMachine;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.GrabImpaleState;
import btm.sword.system.entity.umbral.statemachine.state.InactiveState;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.LungingState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DisplayUtil;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.math.BezierUtil;
import btm.sword.utility.math.ControlVectors;
import btm.sword.utility.math.VectorUtil;
import btm.sword.utility.statemachine.State;
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
        bladeStateMachine.initTransitions(this);

        exitImpalementStatePredicate = blade -> !inState(LodgedState.class);

        endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
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

    public void registerAsInteractableItem() {
        InteractiveItemArbiter.put(this);
    }

    public void unregisterAsInteractableItem() {
        InteractiveItemArbiter.remove(display, false);
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
    protected boolean shouldShowLandingMarker() {
        return false;
    }

    @Override
    protected void teleport() {
        TimeArbiter.teleportDisplay(display, cur, to, 2,
            UmbralBlade.class, 458);
    }

    @Override
    protected void rotate() {

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
