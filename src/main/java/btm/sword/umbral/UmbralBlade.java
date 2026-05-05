package btm.sword.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import btm.sword.action.movement.DashDirection;
import btm.sword.action.throwing.Lodgeable;
import btm.sword.action.throwing.types.ThrownItem;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.UmbralBladeAttack;
import btm.sword.combat.style.AttackType;
import btm.sword.combat.style.WeaponAttackStyle;
import btm.sword.config.Config;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.KeyRegistry;
import btm.sword.item.core.SwordItemType;
import btm.sword.item.special.SoulLinkItem;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.runtime.statemachine.State;
import btm.sword.umbral.input.BladeRequest;
import btm.sword.umbral.input.InputBuffer;
import btm.sword.umbral.statemachine.UmbralStateMachine;
import btm.sword.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.umbral.statemachine.state.AttackingQuickState;
import btm.sword.umbral.statemachine.state.GrabImpaleState;
import btm.sword.umbral.statemachine.state.InactiveState;
import btm.sword.umbral.statemachine.state.LodgedState;
import btm.sword.umbral.statemachine.state.LungingState;
import btm.sword.umbral.statemachine.state.RecallingState;
import btm.sword.umbral.statemachine.state.SheathedState;
import btm.sword.umbral.statemachine.state.StandbyState;
import btm.sword.umbral.statemachine.state.WaitingState;
import btm.sword.umbral.statemachine.state.WieldState;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.misc.Debug;
import btm.sword.util.prefab.Prefab;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit
/** The signature UmbralBlade weapon — a thrown item backed by a full FSM managing all combat states and visual transitions. */
public class UmbralBlade extends ThrownItem implements Lodgeable {
    /** Type of dash-reclaim attack to perform on the next quick or heavy attack entry. */
    public enum ReclaimType {
        NONE,
        CIRCULAR_SLASH, // in waiting state
        EVISCERATE, // If pulling from Lodged
        FORWARD_RUSH // otherwise?
    }

    @Getter
    private final UmbralStateMachine bladeStateMachine;
    @Getter
    private Function<Combatant, Attack>[] basicAttacks;
    @Getter
    private int currentComboStep = -1;

    private SoulLinkItem link;
    @Getter
    private ItemStack blade;
    @Getter
    private final ItemStack weapon;
    @Getter
    @Setter
    private long lastActionTime = 0;

    @Getter
    private final Vector3f scale = new Vector3f(
        (float) Config.UmbralBlade.SCALE_X, (float) Config.UmbralBlade.SCALE_Y, (float) Config.UmbralBlade.SCALE_Z);

    @Getter
    @Setter
    private TimeArbiter.TaskHandle idleMovementTask;

    @Getter
    private final Predicate<UmbralBlade> endHoverPredicate;
    @Getter
    private final Runnable attackEndCallback;
    @Getter
    @Setter
    private boolean attackCompleted = false;

    @Getter
    @Setter
    private ControlVectors ctrlPointsForLunge;
    @Getter
    @Setter
    private boolean finishedLunging = false;
    @Getter
    @Setter
    private boolean usingCustomFuncs;
    @Getter
    @Setter
    private boolean skillFinished;
    @Getter
    @Setter
    private DashDirection dashDirection = DashDirection.NONE;
    @Getter
    @Setter
    private ReclaimType reclaimType = ReclaimType.NONE;
    @Getter
    private final InputBuffer inputBuffer = new InputBuffer();

    /**
     * Tracks whether the blade item (true) or the link item (false) should occupy slot 0.
     * Set by state transitions — independent of actual inventory contents so player
     * manipulation cannot desync it.
     */
    @Getter
    @Setter
    private boolean bladeWielded = false;

    /**
     * Returns the Soul Link {@link ItemStack} for use as an inventory item.
     * Use {@link #getLinkAnchor()} when slot-restoration logic is needed.
     *
     * @return the Soul Link ItemStack
     */
    public ItemStack getLink() {
        return link.getItemStack();
    }

    /**
     * Returns the {@link SoulLinkItem} anchor for slot-restoration and satisfaction checks.
     *
     * @return the Soul Link anchor
     */
    public SoulLinkItem getLinkAnchor() {
        return link;
    }

    /**
     * Returns the correct {@link ItemStack} for slot 0 based on the internal
     * {@link #bladeWielded} flag — the blade when active, the link otherwise.
     *
     * @return the ItemStack that should currently occupy slot 0
     */
    public ItemStack getExpectedSlotItem() {
        return bladeWielded ? blade : getLink();
    }

    /**
     * Scans the player's inventory and removes any blade or link items found outside slot 0.
     * Ensures slot 0 contains the correct item based on {@link #bladeWielded}.
     */
    public void purgeStrayItems() {
        if (!(thrower instanceof SwordPlayer sp)) return;
        org.bukkit.inventory.PlayerInventory inv = sp.getPlayer().getInventory();

        for (int i = 1; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.isEmpty()) continue;
            if (KeyRegistry.hasKey(item, KeyRegistry.UMBRAL_BLADE_KEY)
                || KeyRegistry.hasKey(item, KeyRegistry.SOUL_LINK_KEY)) {
                inv.setItem(i, null);
            }
        }

        // Ensure slot 0 has the correct item
        ItemStack slot0 = inv.getItem(0);
        ItemStack expected = getExpectedSlotItem();
        NamespacedKey expectedKey = bladeWielded ? KeyRegistry.UMBRAL_BLADE_KEY : KeyRegistry.SOUL_LINK_KEY;
        if (slot0 == null || slot0.isEmpty() || !KeyRegistry.hasKey(slot0, expectedKey)) {
            inv.setItem(0, expected);
        }
    }

    /** Constructs the UmbralBlade, spawning its display entity and initialising the FSM and attack chains. */
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
        });

        // TODO: Removed the setup call; must set up elsewhere now.

        this.weapon = weapon;

        generateUmbralItems();

        this.attackEndCallback = () -> attackCompleted = true;

        loadBasicAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        bladeStateMachine.initTransitions();

        exitImpalementStatePredicate = blade -> !inState(LodgedState.class);

        endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
    }

    /** Pushes the given request into the input buffer for evaluation on the next FSM tick. */
    public void request(BladeRequest request) {
        inputBuffer.push(request);
    }

    /** Returns {@code true} and consumes the request if it is present in the input buffer. */
    public boolean isRequested(BladeRequest request) {
        return inputBuffer.consumeIfPresent(request);
    }

    /** Returns {@code true} if any of the given requests is present in the input buffer. */
    public boolean isRequested(BladeRequest... requests) {
        for (BladeRequest request : requests) {
            if (inputBuffer.consumeIfPresent(request)) {
                return true;
            }
        }
        return false;
    }

    /** Returns {@code true} and consumes the request if present and the blade is not in {@link InactiveState}. */
    public boolean isRequestedAndActive(BladeRequest request) {
        return isRequested(request) && !inState(InactiveState.class);
    }

    /** Returns {@code true} if this blade belongs to the given combatant. */
    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId().equals(thrower.getUniqueId());
    }

    /** Returns {@code true} if the FSM is currently in the given state class. */
    public boolean inState(Class<? extends State<UmbralBlade>> clazz) {
        return bladeStateMachine.getState().getClass().equals(clazz);
    }

    /** Ticks the blade: disposes if the thrower is invalid, then advances the FSM by one tick. */
    public void onTick() {
        if (thrower.isInvalid()) {
//            thrower.message("Ending Umbral Blade");
            dispose();
        }

        if (bladeStateMachine != null)
            bladeStateMachine.tick();
    }

    // TODO: #240 - Make a method for calculating correct orientation of blade for edge to align with plane of swing on attack
    /** Applies the display transformation for the given state class after a 50 ms delay. */
    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (display == null) return;

        int duration = getInterpolationDurationForState(state);
        SwordScheduler.runBukkitTaskLater(() -> {
            DisplayUtil.setInterpolationValues(display, 0, duration);
            display.setTransformation(getStateDisplayTransformation(state));
            }, 50, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Returns the interpolation duration in ticks to use when transitioning the blade display
     * into the given state. Fast action states use short durations; idle/equipped states use longer
     * durations for smoother visual transitions.
     *
     * @param state the target state class
     * @return interpolation duration in ticks
     */
    private int getInterpolationDurationForState(Class<? extends State<UmbralBlade>> state) {
        if (state == LungingState.class || state == GrabImpaleState.class) return 2;
        if (state == AttackingQuickState.class || state == AttackingHeavyState.class) return 2;
        if (state == RecallingState.class) return 3;
        if (state == SheathedState.class || state == WieldState.class) return 5;
        if (state == StandbyState.class || state == WaitingState.class) return 8;
        return 4;
    }

    /** Returns the display {@link Transformation} to apply when entering the given state. */
    public Transformation getStateDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (state == SheathedState.class) {
            return new Transformation(
                new Vector3f(0.28f, -1.35f, -0.42f),
                new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / 1.65f),
                scale,
                new Quaternionf());
        } else if (state == StandbyState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotationY(0).rotateZ((float) Math.PI),
                scale,
                new Quaternionf());
        } else if (state == RecallingState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) -Math.PI / 2),
                scale,
                new Quaternionf());
        } else if (state == LungingState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) Math.PI / 2),
                scale,
                new Quaternionf()
            );
        } else if (state == GrabImpaleState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) -Math.PI / 2),
                scale,
                new Quaternionf()
            );
        } else if (state == LodgedState.class) {
            return display.getTransformation();
        } else if (state == AttackingQuickState.class || state == AttackingHeavyState.class) {
            return new Transformation(
                new Vector3f(0, 0, -1),
                new Quaternionf().rotateX((float) Math.PI / 2),
                scale,
                new Quaternionf());
        } else {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI),
                scale,
                new Quaternionf());
        }
    }

    /** Starts the cosine idle bobbing animation on the blade's display entity. */
    public void startIdleMovement() {
        if (idleMovementTask != null) {
            idleMovementTask.cancel();
        }
        final double[] step = {0};
        idleMovementTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                TimeArbiter.setDisplayTransformation(display,
                    new Transformation(
                        new Vector3f(0, (float) (Math.cos(step[0]) * Config.UmbralBlade.IDLE_MOVEMENT_AMPLITUDE), 0),
                        display.getTransformation().getLeftRotation(),
                        scale,
                        new Quaternionf()
                    ),
                    Config.UmbralBlade.IDLE_MOVEMENT_PERIOD
                );

                step[0] += Math.PI / 8;
            },
            null,
            null,
            0, Config.UmbralBlade.IDLE_MOVEMENT_PERIOD,
            UmbralBlade.class, "startIdleMovement"
        );
    }

    /** Cancels the idle bobbing animation task if it is currently running. */
    public void endIdleMovement() {
        if (idleMovementTask != null) {
            idleMovementTask.cancel();
        }
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

    @SuppressWarnings("unchecked")
    private void loadBasicAttacks() {
        basicAttacks = new Function[]{
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
        this.link = new SoulLinkItem(new ItemStackBuilder(Material.HEAVY_CORE)
            .hideAll()
            .name(Component.text("~ ", Config.SwordColor.TEXT_ITEM_NAME)
                .append(Component.text(thrower.getDisplayName() + "'s Soul Link",
                    Config.SwordColor.TEXT_ITEM_NAME, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_ITEM_NAME)))
            .lore(Prefab.Text.SOUL_LINK_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, PersistentDataType.STRING, thrower.getUniqueId().toString())
            .tagSwordItem(SwordItemType.UMBRAL_LINK)
            .build());

        this.blade = new ItemStackBuilder(weapon.getType())
            .hideAll()
            .name(Component.text("~ ", Config.SwordColor.TEXT_COOL_DARK)
                .append(Component.text(thrower.getDisplayName() + "'s Blade",
                    Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .append(Component.text(" ~", Config.SwordColor.TEXT_COOL_DARK)))
            .lore(Prefab.Text.UMBRAL_BLADE_LORE)
            .unbreakable(true)
            .tag(KeyRegistry.UMBRAL_BLADE_KEY, PersistentDataType.STRING, thrower.getUniqueId().toString())
            .tag(KeyRegistry.SPECIAL_ITEM_KEY, PersistentDataType.BOOLEAN, true)
            .tag(KeyRegistry.NON_MOVABLE_KEY, PersistentDataType.BOOLEAN, true)
            .tagSwordItem(SwordItemType.UMBRAL_BLADE)
            .tagAttackStyle(WeaponAttackStyle.SLASH)
            .build();
    }

    /** Removes the existing display entity and respawns a fresh one at the thrower's eye level. */
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
        this.velocityFunction = t -> dir.clone().multiply(0.5);
    }

    /** Sets the reclaim type and schedules a reset to {@link ReclaimType#NONE} after the given duration. */
    public void setReclaimType(ReclaimType type, int durationMilliseconds) {
        reclaimType = type;
        SwordScheduler.runBukkitTaskLater(
            () -> reclaimType = ReclaimType.NONE,
            durationMilliseconds, TimeUnit.MILLISECONDS
        );
    }

    /** Called when a combatant grabs the blade; sets reclaim type and requests the appropriate follow-up state. */
    public void onGrab(Combatant combatant) {
        Debug.umbral("onGrab. inState()=" + bladeStateMachine.getState().name());

        if (!isOwnedBy(combatant)) {
            // TODO: #241 - Add rejection logic for non-thrower grabs
            return;
        }

        if (inState(WaitingState.class)) { // set reclaim type for 1/4 of a second.
            setReclaimType(ReclaimType.CIRCULAR_SLASH, Config.UmbralBlade.RECLAIM_WINDOW_MS);
        }
        else if (inState(LodgedState.class)) {
            setReclaimType(ReclaimType.EVISCERATE, Config.UmbralBlade.RECLAIM_WINDOW_MS);
        }

        if (combatant.holdingUmbralItemInMainHand()) {
            TimeArbiter.runFixedIterationTaskTimer(
                null,
                () -> request(BladeRequest.WIELD),
                Config.UmbralBlade.WIELD_ON_GRAB_DELAY,
                Config.UmbralBlade.WIELD_ON_GRAB_PERIOD,
                Config.UmbralBlade.WIELD_ON_GRAB_ITERATIONS,
                UmbralBlade.class,
                "onGrab",
                null,
                new PredicateRunnablePair(
                    () -> inState(WieldState.class),
                    () -> {}
                )
            );

        }
        else {
            request(BladeRequest.STANDBY);
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
        // no-op
    }

    @Override
    protected void onCatch() {
        // No action on catch.
    }

    @Override
    protected void onGrounded() {
        if (stuckBlock != null) {
            this.blockDustPillarParticle = new ParticleWrapper(
                () -> Particle.DUST_PILLAR, () -> 50, () -> 1.0, () -> 1.0, () -> 1.0)
                .withBlockData(() -> stuckBlock.getBlockData());
            blockDustPillarParticle.display(cur);
        }
        request(BladeRequest.RECALL);
    }

    @Override
    public void onEnd() {
        super.onEnd();
        finishedLunging = true;
        resetFlightState();
    }

    @Override
    public void onHit() {
        // no-op — hit effects are applied explicitly in FSM transition actions and LodgedState.onEnter()
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
        bladeStateMachine.getState().onExit(this);
        bladeStateMachine.setDeactivated(true);
        if (display != null && display.isValid()) {
            display.remove();
        }
        display = null;
    }

    /** Clears all mid-flight flags so the blade can be launched again cleanly. */
    public void resetFlightState() {
        hit = false;
        grounded = false;
        caught = false;
        stuckBlock = null;
        timeScalingFactor = -1;
    }

    /** Sets the active dash direction and resets it to {@link DashDirection#NONE} after 250 ms. */
    public void setDashingDirection(DashDirection direction) {
        this.dashDirection = direction;

        SwordScheduler.runBukkitTaskLater(
            () -> dashDirection = DashDirection.NONE,
            250, TimeUnit.MILLISECONDS
        );
    }

    /** Returns {@code true} if the blade currently has an active dash direction. */
    public boolean isDashing() {
        return !dashDirection.equals(DashDirection.NONE);
    }

    /** Sets the combo step and pushes an {@link BladeRequest#ATTACK_QUICK} to the input buffer. */
    public void requestQuickAttack(int comboStep) {
        currentComboStep = comboStep;
        request(BladeRequest.ATTACK_QUICK);
    }
}
