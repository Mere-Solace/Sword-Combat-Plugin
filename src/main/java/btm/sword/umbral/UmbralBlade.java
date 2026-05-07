package btm.sword.umbral;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.action.movement.DashDirection;
import btm.sword.action.throwing.InteractiveItem;
import btm.sword.action.throwing.Lodgeable;
import btm.sword.action.throwing.impale.Impalement;
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
import btm.sword.umbral.motion.BladeMotion;
import btm.sword.umbral.motion.BladeMotionHost;
import btm.sword.umbral.motion.drivers.ImpalementFollowDriver;
import btm.sword.umbral.presentation.BladeDisplay;
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
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.math.ControlVectors;
import btm.sword.util.misc.Debug;
import btm.sword.util.prefab.Prefab;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * The signature UmbralBlade weapon — coordinator of a layered FSM-driven combat system.
 *
 * <p>Composed of four strictly-owned subsystems:
 * <ul>
 *   <li>{@link BladeIdentity} — immutable thrower / weapon / link / blade tuple (existence)</li>
 *   <li>{@link BladeDisplay} — single owner of the {@link ItemDisplay} entity (presentation)</li>
 *   <li>{@link BladeMotion} — single writer of world-space position via pluggable drivers (motion)</li>
 *   <li>{@link UmbralStateMachine} — intent / time progression (behavior)</li>
 * </ul>
 *
 * <p>Implements {@link InteractiveItem} and {@link Lodgeable} directly — no inheritance from
 * {@code ThrownItem}. The blade does not participate in the parent throw/flight pipeline; its
 * lunge motion is driven by an FSM-installed {@code LungingDriver} on the motion subsystem.</p>
 *
 * <p>Lifecycle: {@code UNINITIALIZED -> ALIVE -> DISPOSED}. {@link #dispose} is idempotent.</p>
 */
public class UmbralBlade implements InteractiveItem, Lodgeable, BladeMotionHost {

    /** Type of dash-reclaim attack to perform on the next quick or heavy attack entry. */
    public enum ReclaimType {
        NONE,
        CIRCULAR_SLASH,
        EVISCERATE,
        FORWARD_RUSH
    }

    // -----------------------------------------------------------------------
    // Identity & subsystems (immutable references for the lifetime of the blade)
    // -----------------------------------------------------------------------

    /**
     * Immutable identity — thrower, original weapon, soul-link anchor, visual blade item.
     * Constructed once in the constructor and never mutated.
     */
    @Getter
    private final BladeIdentity identity;

    /**
     * Per-tick motion subsystem. Owns the currently-installed
     * {@link btm.sword.umbral.motion.BladeMotionDriver} and is the single writer of the blade's
     * world-space position via the {@link BladeMotionHost} bridge below.
     */
    @Getter
    private final BladeMotion bladeMotion;

    /**
     * Single owner of the underlying {@link ItemDisplay} handle. All presentation operations
     * route through this wrapper.
     */
    @Getter
    private final BladeDisplay bladeDisplay;

    /** Finite state machine driving the blade's behavioral states. */
    @Getter
    private final UmbralStateMachine bladeStateMachine;

    /** Initial display setup consumer; reused by {@link #resetWeaponDisplay}. */
    private final Consumer<ItemDisplay> displaySetup;

    // -----------------------------------------------------------------------
    // Combat configuration
    // -----------------------------------------------------------------------

    @Getter
    private Function<Combatant, Attack>[] basicAttacks;
    @Getter
    private int currentComboStep = -1;
    @Getter
    private final Vector3f scale = new Vector3f(
        (float) Config.UmbralBlade.SCALE_X, (float) Config.UmbralBlade.SCALE_Y, (float) Config.UmbralBlade.SCALE_Z);
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
     * Independent of actual inventory contents so player manipulation cannot desync it.
     */
    @Getter
    @Setter
    private boolean bladeWielded = false;

    @Getter
    @Setter
    private long lastActionTime = 0;

    // -----------------------------------------------------------------------
    // Impalement bookkeeping (set by the lunge collision pipeline; read by FSM transitions)
    // -----------------------------------------------------------------------

    /**
     * The entity most recently struck by a lunge. Set by the lunge {@code onEntity} callback;
     * read by FSM transitions to drive the LungingState/GrabImpaleState -> LodgedState branch.
     */
    @Getter
    @Setter
    private SwordEntity hitEntity;

    /**
     * The blade's velocity direction at the moment of the last hit. Captured by lunge drivers
     * and read by FSM knockback computations and {@link #impale}.
     */
    private Vector lastVelocity = new Vector();

    /**
     * Whether the blade has been flagged as retrieved. Debounced by {@link #setRetrieved} to
     * preserve the legacy 60 ms hold-window behavior.
     */
    @Getter
    private boolean retrieved;

    /** The active {@link Impalement} record while the blade is in {@link LodgedState}. */
    @Getter
    @Setter
    private Impalement thisImpalement;

    /**
     * Predicate tested by the {@link ImpalementFollowDriver} to decide when impalement ends.
     * Composed at the blade level so the driver remains FSM-agnostic.
     */
    @Getter
    private final Predicate<UmbralBlade> exitImpalementStatePredicate;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    private boolean disposed = false;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /** Constructs the UmbralBlade, spawning its display and initialising the FSM and attack chains. */
    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        this.displaySetup = display -> {
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
        };

        this.bladeDisplay = new BladeDisplay(scale);
        this.bladeDisplay.respawn(thrower.self(), displaySetup);

        this.identity = new BladeIdentity(thrower, weapon, createLinkItem(thrower), createBladeItem(thrower, weapon));

        this.attackEndCallback = () -> attackCompleted = true;

        loadBasicAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        bladeStateMachine.initTransitions();

        this.bladeMotion = new BladeMotion(this);

        this.exitImpalementStatePredicate = blade -> !inState(LodgedState.class);
        this.endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
    }

    // -----------------------------------------------------------------------
    // Identity-derived accessors
    // -----------------------------------------------------------------------

    /** Returns the blade's owning {@link Combatant}. */
    public Combatant getThrower() {
        return identity.thrower();
    }

    /** Returns the original weapon {@link ItemStack} the blade was created from. */
    public ItemStack getWeapon() {
        return identity.weapon();
    }

    /** Returns the visual blade {@link ItemStack} displayed when the blade is wielded. */
    public ItemStack getBlade() {
        return identity.blade();
    }

    /**
     * Returns the Soul Link {@link ItemStack} for use as an inventory item.
     * Use {@link #getLinkAnchor()} when slot-restoration logic is needed.
     */
    public ItemStack getLink() {
        return identity.link().getItemStack();
    }

    /** Returns the {@link SoulLinkItem} anchor for slot-restoration and satisfaction checks. */
    public SoulLinkItem getLinkAnchor() {
        return identity.link();
    }

    /**
     * Returns the correct {@link ItemStack} for slot 0 based on the internal
     * {@link #bladeWielded} flag — the blade when active, the link otherwise.
     */
    public ItemStack getExpectedSlotItem() {
        return bladeWielded ? identity.blade() : getLink();
    }

    /**
     * Scans the player's inventory and removes any blade or link items found outside slot 0.
     * Ensures slot 0 contains the correct item based on {@link #bladeWielded}.
     */
    public void purgeStrayItems() {
        if (!(identity.thrower() instanceof SwordPlayer sp)) return;
        org.bukkit.inventory.PlayerInventory inv = sp.getPlayer().getInventory();

        for (int i = 1; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.isEmpty()) continue;
            if (KeyRegistry.hasKey(item, KeyRegistry.UMBRAL_BLADE_KEY)
                || KeyRegistry.hasKey(item, KeyRegistry.SOUL_LINK_KEY)) {
                inv.setItem(i, null);
            }
        }

        ItemStack slot0 = inv.getItem(0);
        ItemStack expected = getExpectedSlotItem();
        NamespacedKey expectedKey = bladeWielded ? KeyRegistry.UMBRAL_BLADE_KEY : KeyRegistry.SOUL_LINK_KEY;
        if (slot0 == null || slot0.isEmpty() || !KeyRegistry.hasKey(slot0, expectedKey)) {
            inv.setItem(0, expected);
        }
    }

    // -----------------------------------------------------------------------
    // Input request API
    // -----------------------------------------------------------------------

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
            if (inputBuffer.consumeIfPresent(request)) return true;
        }
        return false;
    }

    /** Returns {@code true} and consumes the request if present and the blade is not in {@link InactiveState}. */
    public boolean isRequestedAndActive(BladeRequest request) {
        return isRequested(request) && !inState(InactiveState.class);
    }

    /** Returns {@code true} if this blade belongs to the given combatant. */
    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId().equals(identity.thrower().getUniqueId());
    }

    /** Returns {@code true} if the FSM is currently in the given state class. */
    public boolean inState(Class<? extends State<UmbralBlade>> clazz) {
        return bladeStateMachine.getState().getClass().equals(clazz);
    }

    // -----------------------------------------------------------------------
    // Per-tick orchestration
    // -----------------------------------------------------------------------

    /**
     * Ticks the blade: disposes if the thrower is invalid, advances the installed motion driver,
     * then advances the FSM by one tick. Motion is ticked first so that states observe the
     * post-tick blade position when their {@code onTick} runs.
     */
    public void onTick() {
        if (disposed) return;
        if (identity.thrower().isInvalid()) {
            dispose();
            return;
        }
        bladeMotion.tick();
        bladeStateMachine.tick();
    }

    // -----------------------------------------------------------------------
    // BladeMotionHost — bridge from the motion subsystem to this blade's display + thrower.
    // -----------------------------------------------------------------------

    @Override
    public Combatant thrower() {
        return identity.thrower();
    }

    @Override
    public void teleportDisplay(Location location, Vector direction, int teleportDuration) {
        bladeDisplay.teleportTo(location, direction, teleportDuration);
    }

    @Override
    public void setDisplayTransformation(Transformation transformation, int transformDuration) {
        bladeDisplay.setTransformation(transformation, transformDuration);
    }

    @Override
    public Location currentDisplayLocation() {
        return bladeDisplay.currentLocation();
    }

    @Override
    public boolean isDisplayValid() {
        return bladeDisplay.isValid();
    }

    // -----------------------------------------------------------------------
    // InteractiveItem — required for legacy world-item integration paths
    // -----------------------------------------------------------------------

    @Override
    public ItemDisplay getDisplay() {
        return bladeDisplay.handle();
    }

    @Override
    public ItemStack getItemStack() {
        return identity.blade();
    }

    // -----------------------------------------------------------------------
    // Lodgeable — impalement bookkeeping contract
    // -----------------------------------------------------------------------

    @Override
    public SwordEntity getImpaledEntity() {
        return hitEntity;
    }

    /**
     * Sets the retrieved flag with a 60 ms automatic reset window. Only schedules the reset when
     * setting to {@code true} so repeated {@code false} calls do not stack scheduled tasks.
     */
    @Override
    public void setRetrieved(boolean retrieved) {
        this.retrieved = retrieved;
        if (retrieved) {
            SwordScheduler.runBukkitTaskLater(() -> this.retrieved = false, 60, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * In the FSM-driven blade, "dispose with new interactive item" is interpreted as a recall
     * request — the blade returns to the thrower rather than dropping a world item.
     */
    @Override
    public void disposeWithNewInteractiveItem() {
        request(BladeRequest.RECALL);
    }

    // -----------------------------------------------------------------------
    // Display transitions per FSM state
    // -----------------------------------------------------------------------

    // TODO: #240 - Make a method for calculating correct orientation of blade for edge to align with plane of swing on attack
    /** Applies the display transformation for the given state class after a 50 ms delay. */
    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (!bladeDisplay.isValid()) return;

        int duration = getInterpolationDurationForState(state);
        SwordScheduler.runBukkitTaskLater(() -> {
            bladeDisplay.setInterpolation(0, duration);
            bladeDisplay.setTransformationImmediate(getStateDisplayTransformation(state));
            }, 50, TimeUnit.MILLISECONDS
        );
    }

    /** Returns the interpolation duration in ticks to use when transitioning into the given state. */
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
                new Quaternionf());
        } else if (state == GrabImpaleState.class) {
            return new Transformation(
                new Vector3f(),
                new Quaternionf().rotateX((float) -Math.PI / 2),
                scale,
                new Quaternionf());
        } else if (state == LodgedState.class) {
            return bladeDisplay.currentTransformation();
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

    /** Starts the cosine idle bobbing animation on the blade's display. */
    public void startIdleMovement() {
        bladeDisplay.startBobbing(Config.UmbralBlade.IDLE_MOVEMENT_AMPLITUDE, Config.UmbralBlade.IDLE_MOVEMENT_PERIOD);
    }

    /** Cancels the idle bobbing animation if running. */
    public void endIdleMovement() {
        bladeDisplay.stopBobbing();
    }

    /** Removes the existing display entity and respawns a fresh one at the thrower's eye level. */
    public void resetWeaponDisplay() {
        bladeDisplay.respawn(identity.thrower().self(), displaySetup);
    }

    // -----------------------------------------------------------------------
    // Lunge collision integration — called from LungingDriver callbacks
    // -----------------------------------------------------------------------

    /**
     * Captures the blade's instantaneous velocity vector, used by FSM transition actions to
     * compute knockback and by {@link #impale} to orient the impalement follow.
     *
     * @param velocity the velocity vector at impact time; defensively cloned
     */
    public void setLastVelocity(Vector velocity) {
        this.lastVelocity = velocity == null ? new Vector() : velocity.clone();
    }

    /** Returns a defensive clone of the last captured velocity vector. */
    public Vector getVelocity() {
        return lastVelocity.clone();
    }

    /** Emits the dramatic dust-pillar particle effect at the given location for a stuck-block hit. */
    public void emitStuckBlockEffect(Block block, Location at) {
        new ParticleWrapper(
            () -> Particle.DUST_PILLAR, () -> 50, () -> 1.0, () -> 1.0, () -> 1.0)
            .withBlockData(block::getBlockData)
            .display(at);
    }

    /**
     * Embeds the blade in the given target entity by installing an {@link ImpalementFollowDriver}
     * and registering the {@link Impalement} bookkeeping. Reads {@link #hitEntity} (set by the
     * lunge {@code onEntity} callback) and {@link #lastVelocity} (also captured at hit time).
     */
    public void impale(LivingEntity hit) {
        thisImpalement = new Impalement(hitEntity);
        thisImpalement.startShouldDisposeCheckTask(hitEntity, this);
        hitEntity.addImpalement(thisImpalement);

        double max = hit.getEyeLocation().getY();
        double feet = hit.getLocation().getY();
        double diff = max - feet;
        Location curLoc = bladeDisplay.currentLocation();
        double curY = curLoc != null ? curLoc.getY() : feet;
        double heightOffset = Math.max(0, Math.min(curY - feet, hit.getHeight()));

        boolean followHead = !Config.Combat.IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS.contains(hitEntity.type())
            && heightOffset >= diff * Config.Combat.IMPALEMENT_HEAD_ZONE_RATIO;

        Vector dir = lastVelocity.lengthSquared() < 1e-9 ? new Vector(0, 0, 1) : lastVelocity.clone().normalize();
        bladeMotion.install(new ImpalementFollowDriver(
            hitEntity,
            dir,
            heightOffset,
            followHead,
            () -> !inState(LodgedState.class),
            2
        ));
    }

    // -----------------------------------------------------------------------
    // Combat helpers
    // -----------------------------------------------------------------------

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

        if (inState(WaitingState.class)) {
            setReclaimType(ReclaimType.CIRCULAR_SLASH, Config.UmbralBlade.RECLAIM_WINDOW_MS);
        } else if (inState(LodgedState.class)) {
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
        } else {
            request(BladeRequest.STANDBY);
        }
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

    // -----------------------------------------------------------------------
    // Disposal
    // -----------------------------------------------------------------------

    /**
     * Idempotent disposal: deactivates the FSM, stops motion, removes the display entity. After
     * disposal {@link #onTick} is a no-op and all subsystems are in their terminal state.
     */
    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        bladeStateMachine.getState().onExit(this);
        bladeStateMachine.setDeactivated(true);
        bladeMotion.stop();
        bladeDisplay.dispose();
    }

    // -----------------------------------------------------------------------
    // Private factories — pure helpers used during construction
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void loadBasicAttacks() {
        basicAttacks = new Function[]{
            combatant -> new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH1_WINDUP,
                true, true, 1,
                10, 30, 500,
                0, 1)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH1,
                        true, false, 0,
                        20, 10, 100,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    100),

            combatant -> new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH2_WINDUP,
                true, true, 2,
                10, 30, 500,
                0, 1)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH2,
                        true, false, 0,
                        20, 10, 100,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    100),

            combatant -> new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH3_WINDUP,
                false, true, 3,
                10, 30, 500,
                0, 1.2)
                .setBlade(this)
                .setInitialMovementTicks(25)
                .setDrawParticles(false)
                .setNextAttack(
                    new UmbralBladeAttack(bladeDisplay, AttackType.WIDE_UMBRAL_SLASH3,
                        false, false, 0,
                        20, 10, 50,
                        0, 1)
                        .setBlade(this)
                        .setOnEntityHitInstructions(swordEntity -> Prefab.Particles.BLEED.display(swordEntity.getChestLocation()))
                        .setCallback(attackEndCallback, 200),
                    250)
        };
    }

    /**
     * Builds the {@link SoulLinkItem} for the given thrower. Pure factory — no instance state.
     */
    private static SoulLinkItem createLinkItem(Combatant thrower) {
        return new SoulLinkItem(new ItemStackBuilder(Material.HEAVY_CORE)
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
    }

    /**
     * Builds the visual blade {@link ItemStack} for the given thrower and source weapon.
     * Pure factory — no instance state.
     */
    private static ItemStack createBladeItem(Combatant thrower, ItemStack weapon) {
        return new ItemStackBuilder(weapon.getType())
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
}
