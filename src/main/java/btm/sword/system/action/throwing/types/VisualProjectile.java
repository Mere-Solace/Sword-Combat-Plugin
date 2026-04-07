package btm.sword.system.action.throwing.types;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.action.throwing.ItemThrowStyle;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DisplayUtil;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * A display-backed simulated projectile with parametric physics.
 * <p>
 * Extends {@link SimulatedDisplay} to add trajectory physics, collision detection,
 * and event hooks. Does not require a {@link btm.sword.system.entity.impl.Combatant}
 * owner — for thrower-aware behaviour see {@link ThrownItem}.
 * </p>
 * <p>
 * Subclasses override the protected hook methods ({@link #resolveOriginLocation()},
 * {@link #resolveBasis()}, {@link #resolveFlatDir()}, {@link #resolvePitch()}) to
 * provide context-specific trajectory parameters.
 * </p>
 */
@Getter
@Setter
public class VisualProjectile extends SimulatedDisplay {
    /** Instructions run on the spawned {@link ItemDisplay} immediately after it exists. */
    protected Consumer<ItemDisplay> displaySetupInstructions;

    private ParticleWrapper blockTrail;

    /** In-hand display offsets relative to the spawn point, in blocks. */
    protected float xDisplayOffset;
    protected float yDisplayOffset;
    protected float zDisplayOffset;

    /** Current world position of the projectile display. */
    protected Location origin;
    protected Location cur;
    protected Location prev;

    /** Displacement from the previous position to the current one this tick. */
    protected Vector to;
    protected Vector velocity;

    /**
     * Overrides the trajectory direction used by {@link #calculatePhysicsFunctions}.
     * When non-null, pitch and flat direction are derived from this vector instead of
     * the thrower's look direction.
     */
    protected Vector launchDirection;

    /**
     * Uniform scale applied to the item display transform in {@link #determineOrientation()}.
     * Defaults to {@code 1.0}. Set before launch (e.g. in {@link ThrowAction#throwDirect}) to
     * adjust the visual size of the thrown item.
     */
    protected float displayScale = 1.0f;

    protected Vector3f displayScaleVector = new Vector3f(1.0f);

    /**
     * Scales the time parameter fed to {@link #positionFunction}.
     * Negative value means no scaling (raw tick count is used).
     */
    protected double timeScalingFactor = -1;
    protected double timeCutoff = -1;

    /** Current physics step counter. Main-thread only — do not use AtomicInteger operations off-thread. */
    protected AtomicInteger timeStep = new AtomicInteger(0);

    protected Basis currentBasis;
    protected double initialVelocity;

    protected Function<Double, Vector> positionFunction;
    protected Function<Double, Vector> velocityFunction;

    protected boolean grounded;
    protected Block stuckBlock;
    protected ParticleWrapper blockDustPillarParticle;

    protected boolean retrieved;
    protected boolean hit;
    protected SwordEntity hitEntity;
    protected boolean caught;
    protected boolean inFlight;

    /** When non-null, used by {@link DisplayUtil#itemDisplayFollow} to decide when impalement ends. */
    protected Predicate<VisualProjectile> exitImpalementStatePredicate;

    protected TimeArbiter.TaskHandle disposeTask;
    protected boolean setupSuccessful;

    // Event hooks — set before launch, no subclass required for simple use cases
    /** Called with the hit entity when the projectile strikes a living entity. */
    public Consumer<SwordEntity> onHitCallback;
    /** Called when the projectile embeds in a block. */
    public Runnable onGroundCallback;
    /** Called when the time cutoff is exceeded. */
    public Runnable onExpireCallback;

    private ItemThrowStyle throwStyle;

    /**
     * Spawns the {@link ItemDisplay} at the given location, runs display setup instructions
     * once successful, and then calls {@link #afterSpawn()}.
     *
     * @param spawnLocation the world location at which to spawn the display
     * @param period        tick interval for the spawn-check polling loop
     */
    protected void setup(Location spawnLocation, int period) {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> {
                try {
                    display = (ItemDisplay) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
                    displaySetupInstructions.accept(display);
                    setupSuccessful = true;
                } catch (Exception e) {
                    e.addSuppressed(e);
                }
            },
            0, period,
            VisualProjectile.class, "setup",
            new PredicateRunnablePair(
                () -> setupSuccessful,
                this::afterSpawn
            )
        );
    }

    /**
     * Called once the {@link ItemDisplay} has been spawned successfully.
     * Initialises the block-trail particle and resets display offsets from config.
     */
    protected void afterSpawn() {
        blockTrail = display.getItemStack().getType().isBlock()
            ? new ParticleWrapper(() -> Particle.BLOCK, () -> 5, () -> 0.25, () -> 0.25, () -> 0.25)
                .withBlockData(() -> display.getItemStack().getType().createBlockData())
            : null;

        xDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_X;
        yDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_Y;
        zDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_Z;
    }

    /**
     * Sets {@link #timeScalingFactor} so that {@code numberOfIterations} ticks span the full cutoff.
     *
     * @param numberOfIterations desired number of physics steps
     */
    public void setTimeScalingFactor(double numberOfIterations) {
        this.timeScalingFactor = (this.timeCutoff > 0 ? timeCutoff : 1) / numberOfIterations;
    }

    /**
     * Initializes flight state: generates physics functions, fires release actions, resets display offsets.
     * <p>
     * Called by {@link #onRelease(double)} and overridden in {@link ThrownItem} to prepend player-specific logic.
     *
     * @param initialVelocity the starting velocity magnitude
     */
    public void initFlight(double initialVelocity) {
        generateFunctions(initialVelocity);
        handleOnReleaseActions();
        xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
        determineOrientation();
    }

    /**
     * Executes one physics tick: apply functions, teleport display, rotate, emit particles, evaluate collisions.
     */
    protected void tickFlight() {
        applyFunctions();

        if (!prev.equals(cur) && cur.clone().subtract(prev).toVector().dot(velocity) > 0) {
            DisplayUtil.setSmoothTeleportDuration(display, 1);
        }

        teleport();
        rotate();

        Prefab.Particles.THROW_TRAIL.display(prev); // TODO: #119 - Make type of particles dynamic
        if (blockTrail != null && timeStep.get() % 3 == 0) // TODO: #119 - Make period dynamic
            blockTrail.display(prev);

        evaluate();
        prev = cur.clone();
        timeStep.incrementAndGet();
    }

    /**
     * Runs one physics tick and returns {@code true} if the flight is over.
     * <p>
     * Used by FSM state classes (e.g. LungingState) to drive physics synchronously from {@code onTick()}.
     *
     * @return {@code true} when a termination condition is met
     */
    public boolean stepFlight() {
        tickFlight();
        return grounded || hit || caught || display.isDead()
            || (timeCutoff > 0 && timeStep.get() * timeScalingFactor > timeCutoff);
    }

    /**
     * Releases the projectile and starts the self-contained physics loop.
     *
     * @param initialVelocity the starting velocity magnitude
     */
    public void onRelease(double initialVelocity) {
        initFlight(initialVelocity);

        TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            this::tickFlight,
            null,
            0, 50,
            VisualProjectile.class, "onRelease",
            new PredicateRunnablePair(
                () -> grounded || hit || caught || display.isDead()
                    || (timeCutoff > 0 && timeStep.get() * timeScalingFactor > timeCutoff),
                this::onEnd
            )
        );
    }

    /**
     * Teleports the display to {@link #cur}, pointing in the direction appropriate for this item's throw style.
     */
    protected void teleport() {
        ItemThrowStyle style = getThrowStyle();
        if (style == ItemThrowStyle.SPEAR || style == ItemThrowStyle.HATCHET) {
            TimeArbiter.teleportDisplay(display, cur, velocity, 2, VisualProjectile.class, 201);
        } else {
            TimeArbiter.teleportDisplay(display, cur, currentBasis.forward(), 2, VisualProjectile.class, 203);
        }
    }

    /**
     * Advances the position and velocity from the parametric functions.
     */
    protected void applyFunctions() {
        double time = timeScalingFactor < 0 ? timeStep.get() : timeStep.get() * timeScalingFactor;
        cur = origin.clone().add(positionFunction.apply(time));
        if (prev != null) to = cur.clone().subtract(prev).toVector();
        velocity = velocityFunction.apply(time);
    }

    /**
     * Applies the item's spin rotation each tick based on its {@link ItemThrowStyle}.
     */
    protected void rotate() {
        switch (getThrowStyle()) {
            case SPEAR -> {}
            case HATCHET, ROTATE -> applyRotation(false, (float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_AXE);
            case LOB -> applyRotation(true, (float) (Config.Physics.THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED * 0.4));
            default -> applyRotation(true, (float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED);
        }
    }

    private void applyRotation(boolean rotateX, float speed) {
        Transformation curTr = display.getTransformation();
        Quaternionf curRotation = curTr.getLeftRotation();
        Quaternionf newRotation = rotateX ? curRotation.rotateX(speed) : curRotation.rotateZ(speed);
        display.setTransformation(new Transformation(
            curTr.getTranslation(), newRotation, curTr.getScale(), curTr.getRightRotation()
        ));
    }

    /**
     * Called when the flight loop ends. Delegates to the appropriate outcome handler.
     */
    protected void onEnd() {
        if (caught) onCatch();
        else if (hit) onHit();
        else if (grounded) onGrounded();
        else if (onExpireCallback != null) onExpireCallback.run();
        timeStep.set(0);
    }

    /**
     * Called when the projectile hits a living entity.
     * Base implementation invokes {@link #onHitCallback} if set.
     */
    public void onHit() {
        if (onHitCallback != null && hitEntity != null) onHitCallback.accept(hitEntity);
    }

    /**
     * Called when the projectile embeds in a block.
     * Base implementation positions the display at the impact site and schedules disposal.
     */
    protected void onGrounded() {
        if (stuckBlock != null) {
            new ParticleWrapper(() -> Particle.BLOCK, () -> 50, () -> 1.0, () -> 1.0, () -> 1.0)
                .withBlockData(() -> stuckBlock.getBlockData()).display(cur);
        }

        Vector step = velocity.clone().normalize().multiply(0.1);
        Location probe = cur.clone();
        Vector backwards = velocity.normalize().multiply(-0.1);

        int x = 1;
        while (!probe.getBlock().isPassable()) {
            probe.add(backwards);
            if (++x > 30) break;
        }

        SwordScheduler.runBukkitTaskLater(() -> {
            Location land = probe.clone();
            land.setDirection(to.normalize());
            TimeArbiter.teleportDisplay(display, land, null, 1, VisualProjectile.class, 267);
        }, 51, TimeUnit.MILLISECONDS);

        startGroundedDisposeTask(step);

        if (onGroundCallback != null) onGroundCallback.run();
    }

    /**
     * Schedules the disposal timer that shows grounded-item marker particles until the display is removed.
     *
     * @param step the half-step vector used to place the three marker particles
     */
    protected void startGroundedDisposeTask(Vector step) {
        AtomicInteger iteration = new AtomicInteger(0);
        disposeTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                Prefab.Particles.THROWN_ITEM_MARKER.display(cur.clone().add(step));
                Prefab.Particles.THROWN_ITEM_MARKER.display(cur);
                Prefab.Particles.THROWN_ITEM_MARKER.display(cur.clone().subtract(step));
                iteration.set(iteration.get() + Config.Timing.THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL);
            },
            null,
            1, Config.Timing.THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL,
            VisualProjectile.class, "startGroundedDisposeTask",
            new PredicateRunnablePair(() -> display.isDead(), null),
            new PredicateRunnablePair(
                () -> iteration.get() > Config.Timing.THROWN_ITEMS_DISPOSAL_TIMEOUT,
                () -> { if (!display.isDead()) display.remove(); }
            )
        );
    }

    /**
     * Called when the projectile is caught by the owner entity. Base implementation disposes the display.
     */
    protected void onCatch() {
        dispose();
    }

    /**
     * Evaluates collision state each tick. Short-circuits if a terminal state is already set.
     */
    public void evaluate() {
        if (hit || grounded || caught) return;
        hitCheck();
        groundedCheck();
    }

    /**
     * Checks whether the projectile has embedded in a block this tick.
     */
    public void groundedCheck() {
        RayTraceResult hitBlock = display.getWorld()
            .rayTraceBlocks(cur, velocity,
                cur.toVector().subtract(prev.toVector()).lengthSquared()
                    * Config.Detection.THROW_GROUND_CHECK_MULTIPLIER,
                FluidCollisionMode.NEVER, true);

        if (hitBlock == null) return;
        if (hitBlock.getHitBlock() == null || hitBlock.getHitBlock().getType().isAir()) return;

        grounded = true;
        stuckBlock = hitBlock.getHitBlock();
        cur = hitBlock.getHitPosition().toLocation(display.getWorld());
    }

    /**
     * Checks for entity collision via raytrace from the previous to the current position.
     * Catch detection uses {@link #isOwnerEntity(LivingEntity)} so subclasses can identify their owner.
     */
    public void hitCheck() {
        Predicate<Entity> effFilter = getFilter();

        if (prev == null) {
            disposeWithNewInteractiveItem();
            return;
        }

        RayTraceResult result = display.getWorld()
            .rayTraceEntities(prev, velocity,
                cur.toVector().subtract(prev.toVector()).lengthSquared()
                    * Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER,
                Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER, effFilter);

        if (result == null) return;
        if (!(result.getHitEntity() instanceof LivingEntity le)) return;

        if (isOwnerEntity(le)) {
            caught = true;
        } else {
            hit = true;
            this.hitEntity = SwordEntityArbiter.getOrAdd(le);
        }
    }

    /**
     * Returns {@code true} if the given entity is the owner of this projectile.
     * Base returns {@code false} (no owner). Override in {@link ThrownItem} to use the thrower.
     *
     * @param entity the candidate entity
     * @return whether the entity is considered the owner/thrower
     */
    protected boolean isOwnerEntity(LivingEntity entity) {
        return false;
    }

    /**
     * Returns the entity filter applied during raytrace hit detection.
     * Base excludes the display entity and dead entities.
     *
     * @return the filter predicate
     */
    protected Predicate<Entity> getFilter() {
        return entity ->
            entity.getUniqueId() != display.getUniqueId()
                && (entity instanceof LivingEntity l)
                && !l.isDead();
    }

    /**
     * Applies a material-based {@link Transformation} to the display so items are correctly oriented,
     * taking display-offset fields into account.
     */
    @Override
    public void determineOrientation() {
        if (display == null) {
            Debug.umbral("display null in determineOrientation()");
            return;
        }

        String name = display.getItemStack().getType().toString();
        Vector3f base = new Vector3f(xDisplayOffset, yDisplayOffset, zDisplayOffset);

        if (name.endsWith("_SWORD")) {
            display.setTransformation(new Transformation(
                base.add(new Vector3f()),
                new Quaternionf().rotateY((float) Math.PI / 2).rotateZ((float) Math.PI / 2),
                displayScaleVector,
                new Quaternionf()
            ));
        } else if (name.endsWith("AXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")) {
            Vector3f axeScaleVector = new Vector3f(displayScaleVector).mul(1.5f); // TODO: Config?
            display.setTransformation(new Transformation(
                base.add(new Vector3f()),
                new Quaternionf().rotateY((float) -Math.PI / 2).rotateZ((float) Math.PI / 4),
                // TODO: Config all of these magic numbers?
                axeScaleVector,
                new Quaternionf()
            ));
        } else if (display.getItemStack().getType() == Material.SHIELD) {
            display.setTransformation(new Transformation(
                base.add(new Vector3f(0)),
                new Quaternionf().rotateY((float) (Math.PI / 1.01f) * 0),
                displayScaleVector,
                new Quaternionf()
            ));
        } else {
            display.setTransformation(new Transformation(
                base.add(new Vector3f()),
                new Quaternionf().rotateZ((float) Math.PI / 8),
                displayScaleVector,
                new Quaternionf()
            ));
        }
    }

    /**
     * Returns the {@link ItemThrowStyle} for this item, reading PDC first, then falling back to material.
     * Result is cached after the first call.
     *
     * @return the throw style
     */
    protected ItemThrowStyle getThrowStyle() {
        if (throwStyle != null) return throwStyle;
        if (itemStack != null && !itemStack.isEmpty()) {
            String stored = KeyRegistry.getKeyField(itemStack, KeyRegistry.THROW_STYLE_KEY, PersistentDataType.STRING);
            if (stored != null) {
                try {
                    throwStyle = ItemThrowStyle.valueOf(stored);
                    return throwStyle;
                } catch (IllegalArgumentException ignored) {}
            }
        }
        throwStyle = deriveThrowStyle();
        return throwStyle;
    }

    private ItemThrowStyle deriveThrowStyle() {
        if (itemStack == null || itemStack.isEmpty()) return ItemThrowStyle.PITCH;
        String name = itemStack.getType().toString();
        if (name.endsWith("_SWORD")) return ItemThrowStyle.SPEAR;
        if (name.endsWith("_AXE") || name.endsWith("_HOE")
            || name.endsWith("_SHOVEL") || name.endsWith("_PICKAXE")) return ItemThrowStyle.HATCHET;
        if (itemStack.getType().isBlock()) return ItemThrowStyle.LOB;
        return ItemThrowStyle.PITCH;
    }

    /**
     * Marks this projectile as retrieved, then schedules a reset after 60 ms.
     *
     * @param retrieved the retrieved state
     */
    public void setRetrieved(boolean retrieved) {
        this.retrieved = retrieved;
        SwordScheduler.runBukkitTaskLater(() -> setRetrieved(false), 60, TimeUnit.MILLISECONDS);
    }

    /**
     * Registers this projectile with {@link InteractiveItemArbiter}, then removes it and
     * drops a new interactable item at the hit or current location.
     */
    public void disposeWithNewInteractiveItem() {
        if (display == null) return;
        Location dropLocation = hitEntity != null ? hitEntity.self().getLocation() : display.getLocation();
        InteractiveItemArbiter.dropNaturally(dropLocation, display.getItemStack());
        dispose();
    }

    /**
     * Cleans up the display and cancels any pending tasks.
     */
    @Override
    public void dispose() {
        if (display != null) {
            display.remove();
        }
        if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
    }

    /**
     * Registers this projectile with {@link InteractiveItemArbiter}.
     * Subclasses may override to add additional release actions (e.g., clearing the thrower's hand).
     */
    protected void handleOnReleaseActions() {
        InteractiveItemArbiter.put(this);
    }

    /**
     * Damages the held item on a successful throw hit. Base is a no-op; overridden in {@link ThrownItem}.
     */
    public void handleItemDamageAndCheckIfBroken() {
        // no-op in base; ThrownItem implements with thrower attribution
    }

    /**
     * Delegates to {@link #calculatePhysicsFunctions(double)}.
     * Overriding in subclasses (e.g. UmbralBlade) swaps in alternate trajectory math.
     *
     * @param initialVelocity the starting speed
     */
    protected void generateFunctions(double initialVelocity) {
        calculatePhysicsFunctions(initialVelocity);
    }

    /**
     * Computes the {@link #positionFunction} and {@link #velocityFunction} for a parabolic throw.
     * Uses hook methods to resolve origin, direction, and pitch so subclasses can supply their own values.
     *
     * @param initialVelocity the starting speed
     */
    private void calculatePhysicsFunctions(double initialVelocity) {
        this.initialVelocity = initialVelocity;

        this.currentBasis = resolveBasis();

        double min = Math.toRadians(-89);
        double max = Math.toRadians(89);
        double phi = Math.max(min, Math.min(max, resolvePitch()));

        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double forwardCoefficient = initialVelocity * cosPhi;
        double upwardCoefficient = initialVelocity * sinPhi;

        origin = resolveOriginLocation();
        cur = origin.clone();
        prev = cur.clone();

        Vector flatDir = resolveFlatDir();
        velocity = flatDir.clone();
        Vector forwardVelocity = flatDir.clone().multiply(forwardCoefficient);
        Vector upwardVelocity = Config.Direction.up().multiply(upwardCoefficient);

        double gravDamper = Config.Physics.THROWN_ITEMS_GRAVITY_DAMPER;

        positionFunction = t -> flatDir.clone().multiply(forwardCoefficient * t)
            .add(Config.Direction.up().multiply(
                (upwardCoefficient * t) - (initialVelocity * (1.0 / gravDamper) * t * t)));

        velocityFunction = t -> forwardVelocity.clone()
            .add(upwardVelocity.clone()
                .add(Config.Direction.up().multiply(-initialVelocity * (2.0 / gravDamper) * t)));
    }

    // -----------------------------------------------------------------------
    // Hook methods — override in ThrownItem to supply thrower-derived values
    // -----------------------------------------------------------------------

    /**
     * Returns the world location at which the trajectory originates.
     * Requires {@link #origin} to be pre-set if not overridden.
     *
     * @return the origin location (must not be null)
     */
    protected Location resolveOriginLocation() {
        if (origin != null) return origin.clone();
        throw new IllegalStateException("VisualProjectile requires origin to be set or resolveOriginLocation() to be overridden");
    }

    /**
     * Returns the horizontal basis (yaw only) for the projectile's local coordinate frame.
     * Requires {@link #launchDirection} to be set if not overridden.
     *
     * @return the basis
     */
    protected Basis resolveBasis() {
        if (launchDirection != null) {
            float yaw = (float) Math.toDegrees(Math.atan2(-launchDirection.getX(), launchDirection.getZ()));
            Location temp = new Location(
                origin != null ? origin.getWorld() : null,
                0, 0, 0, yaw, 0);
            return VectorUtil.getBasisWithoutPitch(temp);
        }
        throw new IllegalStateException("VisualProjectile requires launchDirection or overridden resolveBasis()");
    }

    /**
     * Returns the normalized horizontal direction vector for the throw.
     * Requires {@link #launchDirection} to be set if not overridden.
     *
     * @return the flat direction (Y component should be zero)
     */
    protected Vector resolveFlatDir() {
        if (launchDirection != null) return launchDirection.clone().setY(0).normalize();
        throw new IllegalStateException("VisualProjectile requires launchDirection or overridden resolveFlatDir()");
    }

    /**
     * Returns the vertical angle (radians) of the throw, positive = upward.
     * Requires {@link #launchDirection} to be set if not overridden.
     *
     * @return the pitch in radians
     */
    protected double resolvePitch() {
        if (launchDirection != null) {
            Vector nd = launchDirection.clone().normalize();
            double horiz = Math.sqrt(nd.getX() * nd.getX() + nd.getZ() * nd.getZ());
            return Math.atan2(nd.getY(), horiz);
        }
        throw new IllegalStateException("VisualProjectile requires launchDirection or overridden resolvePitch()");
    }

    // -----------------------------------------------------------------------
    // Position accessors (defensive copies)
    // -----------------------------------------------------------------------

    /**
     * Sets the physics step counter to the given value.
     *
     * @param timeStep the new step count
     */
    public void setTimeStep(int timeStep) {
        this.timeStep.set(timeStep);
    }

    /** Sets a uniform display scale applied to all three axes of the projectile's item display. */
    public void setDisplayScale(float displayScale) {
        this.displayScale = displayScale;
        this.displayScaleVector = new Vector3f(displayScale);
    }

    /** Sets a per-axis display scale vector for the projectile's item display. */
    public void setDisplayScale(Vector3f displayScaleVector) {
        this.displayScale = 1;
        this.displayScaleVector = displayScaleVector;
    }

    /**
     * Returns a clone of the trajectory origin.
     *
     * @return the origin location
     */
    public Location getOrigin() {
        return origin.clone();
    }

    /**
     * Returns a clone of the current position.
     *
     * @return the current location
     */
    public Location getCur() {
        return cur.clone();
    }

    /**
     * Returns a clone of the previous position.
     *
     * @return the previous location
     */
    public Location getPrev() {
        return prev.clone();
    }

    /**
     * Returns a clone of the displacement vector from the previous to current position.
     *
     * @return the displacement vector
     */
    public Vector getTo() {
        return to.clone();
    }

    /**
     * Returns a clone of the current velocity vector.
     *
     * @return the velocity
     */
    public Vector getVelocity() {
        return velocity.clone();
    }
}
