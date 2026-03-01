package btm.sword.system.action.throwing.types;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.InteractiveItem;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.action.throwing.ItemThrowStyle;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.action.throwing.impale.Impalement;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.ItemUsageManager;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DisplayUtil;
import btm.sword.utility.display.ParticleWrapper;
import btm.sword.utility.entity.EntityUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Represents a thrown item entity that is actively simulated in the world.
 * <p>
 * Handles all aspects of the throw lifecycle — initialization, motion physics,
 * collision detection, and interaction outcomes such as hitting entities,
 * embedding in blocks, or being caught.
 */
@Getter
@Setter
public class ThrownItem implements InteractiveItem {
    protected final Combatant thrower;
    private ParticleWrapper blockTrail;
    protected ItemDisplay display;
    protected ItemStack itemStack;
    protected Consumer<ItemDisplay> displaySetupInstructions;

    protected Impalement thisImpalement;

    protected float xDisplayOffset;
    protected float yDisplayOffset;
    protected float zDisplayOffset;

    protected Location origin;
    protected Location cur;
    protected Location prev;
    protected Vector to; // cur - prev
    protected Vector velocity;

    protected double timeScalingFactor = -1;
    protected double timeCutoff = -1;
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

    protected Predicate<ThrownItem> exitImpalementStatePredicate;

    protected TimeArbiter.TaskHandle disposeTask;

    protected boolean setupSuccessful;

    private TextDisplay landingMarker;
    private TimeArbiter.TaskHandle landingParticleTask;
    private ItemThrowStyle throwStyle;
    private float landingMarkerSize = 4.0f;

    public ThrownItem(Combatant thrower, Consumer<ItemDisplay> displaySetupInstructions, int setupPeriod) {
        this.thrower = thrower;
        this.displaySetupInstructions = displaySetupInstructions;
        setupSuccessful = false;

        xDisplayOffset = -0.6f;
        yDisplayOffset = 0.25f;
        zDisplayOffset = -0.1f;

        setup(setupPeriod);
    }

    protected void setup(int period) {
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> {
                try {
                    LivingEntity e = thrower.self();
                    display = (ItemDisplay) e.getWorld().spawnEntity(e.getEyeLocation(), EntityType.ITEM_DISPLAY);
                    displaySetupInstructions.accept(display);
                    setupSuccessful = true;
                } catch (Exception e) {
                    e.addSuppressed(e);
                }
            },
            0, period,
            ThrowAction.class, "setup",
            new PredicateRunnablePair(
                () -> setupSuccessful,
                this::afterSpawn
            )
        );
    }

    protected void afterSpawn() {
        blockTrail = display.getItemStack().getType().isBlock() ?
                new ParticleWrapper(Particle.BLOCK, 5, 0.25,  0.25,  0.25,
                        display.getItemStack().getType().createBlockData()) :
                null;

        if (thrower instanceof SwordPlayer sp) {
            sp.setMainHandItemStackDuringThrow(sp.getMainItemStackAtTimeOfHold());
            sp.setOffHandItemStackDuringThrow(sp.getOffItemStackAtTimeOfHold());
        }
        else {
            thrower.setMainHandItemStackDuringThrow(thrower.getItemStackInHand(true));
            thrower.setOffHandItemStackDuringThrow(thrower.getItemStackInHand(false));
        }
        // Base values for where the ItemDisplay is held in relation to the player's eye location
        xDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_X;
        yDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_Y;
        zDisplayOffset = Config.Physics.THROWN_ITEMS_DISPLAY_OFFSET_Z;
    }

    public void setTimeScalingFactor(double numberOfIterations) {
        this.timeScalingFactor = (this.timeCutoff > 0 ? timeCutoff : 1)/numberOfIterations;
    }

    /**
     * Called when the item is primed to be thrown (held ready but not yet released).
     * <p>
     * Manages visual positioning, cancels premature throws, and displays in-hand effects.
     */
    public void onReady() {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(false);
            sp.setThrownItemIndex();

            // Interacting with an entity will cause the shield holding mechanic to falter
            if (sp.isInteractingWithEntity()) {
                sp.setAttemptingThrow(false);
                sp.setThrowSuccessful(true);
                // this throw should be weaker because it's automatic. Could turn into a lunge or thrust or smth else
                sp.getThrownItem().onRelease(2);
                thrower.setItemTypeInHand(Material.AIR, true);
                sp.endHoldingRight();
                sp.resetTree();
                return;
            }
        }
        determineOrientation();
        final LivingEntity throwerEntity = thrower.self();
        final int[] iteration = {0};

        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
          null,
            () -> {
                if (thrower instanceof SwordPlayer sp) {
                    if (!sp.isChangingHandIndex() && sp.getCurrentInvIndex() == sp.getThrownItemIndex()) {
                        if (iteration[0] < 10)
                            sp.itemNameDisplay("- HURL IT AT 'EM SOLDIER! -", TextColor.color(100, 100, 100), null);
                        else
                            sp.itemNameDisplay("| HURL IT AT 'EM SOLDIER! |", TextColor.color(150, 150, 150), null);

                        if (iteration[0] > 20) iteration[0] = 0;
                        iteration[0]++;
                    }
                }

                throwerEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));

                TimeArbiter.teleportDisplay(display, throwerEntity.getEyeLocation(), null, 2);
            },
            0, 50,
            ThrownItem.class, "onReady",
            new PredicateRunnablePair(
                thrower::isThrowCancelled,
                () -> {
                    display.remove();
                    ThrowAction.throwCancel(thrower);
                    thrower.setThrownItem(null);
                }
            ),
            new PredicateRunnablePair(
                thrower::isThrowSuccessful,
                () -> thrower.setItemTypeInHand(Material.AIR, true)
            )
        );
    }

    /**
     * Called when the item is released (actually thrown).
     * <p>
     * Initializes trajectory, physics parameters, and continuous motion updates.
     *
     * @param initialVelocity The starting velocity magnitude of the throw.
     */
    public void onRelease(double initialVelocity) {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(true);
            SwordScheduler.runBukkitTaskLater(() ->
                sp.setThrewItem(false),
                100, TimeUnit.MILLISECONDS
            );
        }

        generateFunctions(initialVelocity);

        if (shouldShowLandingMarker()) {
            spawnLandingMarker(precomputeLanding());
        }

        handleOnReleaseActions();

        xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
        determineOrientation();

        TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> {
                applyFunctions();

                if (!prev.equals(cur) && cur.clone().subtract(prev).toVector().dot(velocity) > 0) {
                    DisplayUtil.setSmoothTeleportDuration(display, 1);
                }

                teleport();
                rotate();

                Prefab.Particles.THROW_TRAIl.display(prev); // TODO: #119 - Make type of particles dynamic
                if (blockTrail != null && timeStep.get() % 3 == 0) // TODO: #119 - Make period dynamic
                    blockTrail.display(prev);

                evaluate();
                prev = cur.clone();

                timeStep.incrementAndGet();
            },
            null,
            0, 50,
            ThrownItem.class, "onRelease",
            new PredicateRunnablePair(
                () -> grounded || hit || caught || display.isDead() ||
                    (timeCutoff > 0 && timeStep.get() * timeScalingFactor > timeCutoff),
                () -> {
                    String reason = grounded ? "grounded" :
                        hit ? "hit" :
                            caught ? "caught" :
                                display.isDead() ? "display dead" :
                                    (timeCutoff > 0 && timeStep.get() * timeScalingFactor > timeCutoff) ? "time cutoff" :
                                        "unknown";
                    thrower.message("Ending due to: " + reason);

                    onEnd();
                }
            )
        );
    }

    protected void teleport() {
        ItemThrowStyle style = getThrowStyle();
        if (style == ItemThrowStyle.SPEAR || style == ItemThrowStyle.HATCHET) {
            // Tip tracks velocity direction — spear stays pointed, hatchet flips around that axis
            TimeArbiter.teleportDisplay(display, cur, velocity, 2);
        }
        else {
            TimeArbiter.teleportDisplay(display, cur, currentBasis.forward(), 2);
        }
    }

    protected void applyFunctions() {
        double time;
        if (timeScalingFactor < 0) {
            time = timeStep.get();
        }
        else {
            time = timeStep.get() * timeScalingFactor;
        }
        cur = origin.clone().add(positionFunction.apply(time));
        if (prev != null) to = cur.clone().subtract(prev).toVector();
        velocity = velocityFunction.apply(time);
    }

    /**
     * Play sounds and remove the item in the main hand of the player. Should be overridden for different logic.
     */
    protected void handleOnReleaseActions() {
        Prefab.Sounds.THROW.playForAllInRadius(thrower.self());
        thrower.setItemStackInHand(ItemStack.of(Material.AIR), true);
        InteractiveItemArbiter.put(this);
    }

    protected void generateFunctions(double initialVelocity) {
        calculatePhysicsFunctions(initialVelocity);
    }

    private void calculatePhysicsFunctions(double initialVelocity) {
        this.initialVelocity = initialVelocity;

        LivingEntity ex = thrower.self();
        Location o = ex.getEyeLocation();
        this.currentBasis = VectorUtil.getBasisWithoutPitch(ex);

        // clamp the pitch between these values so that strange undefined vertical vector behavior doesn't occur.
        double min = Math.toRadians(-89);
        double max = Math.toRadians(89);
        double phi = Math.max(min, Math.min(max, Math.toRadians(-1 * o.getPitch())));

        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double forwardCoefficient = initialVelocity * cosPhi;
        double upwardCoefficient = initialVelocity * sinPhi;

        origin = o.add(currentBasis.right().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_FORWARD))
            .add(currentBasis.up().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_UP))
            .add(currentBasis.forward().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_BACK));
        cur = origin.clone();
        prev = cur.clone();
        Vector flatDir = thrower.getFlatDir().rotateAroundY(Config.Physics.THROWN_ITEMS_TRAJECTORY_ROTATION);
        velocity = flatDir.clone();
        Vector forwardVelocity = flatDir.clone().multiply(forwardCoefficient);
        Vector upwardVelocity = Config.Direction.UP().multiply(upwardCoefficient);

        double gravDamper = Config.Physics.THROWN_ITEMS_GRAVITY_DAMPER;

        positionFunction = t -> flatDir.clone().multiply(forwardCoefficient * t)
            .add(Config.Direction.UP().multiply((upwardCoefficient * t) - (initialVelocity * (1 / gravDamper) * t * t)));

        velocityFunction = t -> forwardVelocity.clone()
            .add(upwardVelocity.clone().add(Config.Direction.UP().multiply(-initialVelocity * (2 / (gravDamper)) * t)));
    }

    /**
     * Applies appropriate rotation to the display based on the item's {@link ItemThrowStyle}.
     * <p>
     * SPEAR items keep their tip pointed forward with no spin. HATCHET items flip forward
     * on the Z-axis (like a knife throw). LOB items tumble slowly. Everything else spins
     * at the default rate.
     */
    private void rotate() {
        switch (getThrowStyle()) {
            case SPEAR -> { return; } // tip stays pointed forward, no spin
            case HATCHET -> applyRotation(false, (float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_AXE);
            case LOB -> applyRotation(true, (float) (Config.Physics.THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED * 0.4));
            default -> applyRotation(true, (float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED);
        }
    }

    private void applyRotation(boolean rotateX, float speed) {
        Transformation curTr = display.getTransformation();
        Quaternionf curRotation = curTr.getLeftRotation();
        Quaternionf newRotation = rotateX
            ? curRotation.rotateX(speed)
            : curRotation.rotateZ(speed);
        display.setTransformation(new Transformation(
            curTr.getTranslation(), newRotation, curTr.getScale(), curTr.getRightRotation()
        ));
    }

    /**
     * Called once the throw has completed its trajectory or interaction.
     * <p>
     * Delegates to the correct outcome handler depending on state flags.
     */
    protected void onEnd() {
        removeLandingMarker();
        if (caught) onCatch();
        else if (hit) onHit();
        else if (grounded) onGrounded();
        timeStep.set(0);
    }

    /**
     * Handles logic when the thrown item hits the ground or block.
     * <p>
     * Creates marker particles, positions the display, and schedules timed cleanup.
     */
    protected void onGrounded() {
        if (stuckBlock != null) {
            new ParticleWrapper(Particle.BLOCK, 50, 1, 1, 1, stuckBlock.getBlockData()).display(cur);
        }

        double offset = 0.1;
        Vector n = velocity.normalize();
        Vector step = n.clone().multiply(offset);

        Location probe = cur.clone();
        Vector backwards = velocity.normalize().multiply(-0.1);

        int x = 1;
        while (!probe.getBlock().isPassable()) {
            probe.add(backwards);
            x++;
            if (x > 30) break;
        }

        SwordScheduler.runBukkitTaskLater(() -> {
            Location land = probe.clone();
            land.setDirection(to.normalize());
            TimeArbiter.teleportDisplay(display, land, null, 1);
            },
            51, TimeUnit.MILLISECONDS
        );

        startGroundedDisposeTask(step);
    }

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
            ThrowAction.class, "startGroundedDisposeTask",
            new PredicateRunnablePair(
                () -> display.isDead(), null
            ),
            new PredicateRunnablePair(
                () -> iteration.get() > Config.Timing.THROWN_ITEMS_DISPOSAL_TIMEOUT,
                () -> {
                    if (!display.isDead()) display.remove();
                }
            )
        );
    }

    /**
     * Handles logic when the thrown item successfully hits a living entity.
     * <p>
     * Manages impalement, knockback, pinning, and delayed disposal.
     */
    public void onHit() {
        if (hitEntity == null) return;

        // TODO: #127 - Better checks for weapon, can tag with impactType = 'impale'
        String name = display.getItemStack().getType().toString();

        handleItemDamageAndCheckIfBroken();

        if (name.endsWith("_SWORD") || name.endsWith("AXE")) {
            startImpalementTask(hitEntity);
        }
        else {
            nonImpalingImpact(hitEntity);
        }
    }

    public void handleItemDamageAndCheckIfBroken() {
        if (display == null || itemStack == null) return;

        if (itemStack.isEmpty() ||
            ItemUsageManager.isUnbreakable(itemStack)) return;

        int currentDamage = ItemUsageManager.currentItemDamage(itemStack);

        Debug.debug(ThrownItem.class, 514, "Current Damage: " + currentDamage);

        int maxThrownItemUses = 3;
        int onThrowHitDamageToItem = ItemUsageManager.maxItemDamage(itemStack) / maxThrownItemUses;

        Debug.debug(ThrownItem.class, 514, "OnThrowHitDamage: " + onThrowHitDamageToItem);
        Debug.debug(ThrownItem.class, 514, "Check Value: " + onThrowHitDamageToItem * (1 - maxThrownItemUses));


        if (currentDamage >= onThrowHitDamageToItem * (maxThrownItemUses - 1)) {
            Prefab.Particles.ITEM_THROW_BREAK.display(display.getLocation());

            Debug.debug(ThrownItem.class, 519, "Item is getting destroyed");

            itemStack.damage(77777777, thrower.self());
            display.remove();
        }
        else {
            ItemUsageManager.damageItemStack(itemStack, onThrowHitDamageToItem, thrower.self());
        }
    }

    protected void nonImpalingImpact(SwordEntity target) {
        target.hit(thrower, Prefab.Attacks.thrownWeapon,
            velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER));

        target.world().createExplosion(target.getChestLocation(),
            Config.Combat.THROWN_DAMAGE_OTHER_EXPLOSION_POWER,
            Config.World.EXPLOSIONS_SET_FIRE,
            Config.World.EXPLOSIONS_BREAK_BLOCKS);

        if (display.isValid()) {
            disposeWithNewInteractiveItem();
        }
    }

    private void startImpalementTask(SwordEntity target) {
        Vector kb = EntityUtil.isOnGround(target.self()) ?
            velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED) :
            VectorUtil.getProjOntoPlane(velocity, Config.Direction.UP()).multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE);

        if (display.isValid()) {
            impale(target.self());
        }
        target.hit(thrower, Prefab.Attacks.thrownWeapon, kb);
    }

    /**
     * Impales a {@link LivingEntity} when struck, embedding the item visually and applying follow behavior.
     *
     * @param hit The living entity being impaled.
     */
    public void impale(LivingEntity hit) {
        thisImpalement = new Impalement(hitEntity);
        thisImpalement.startShouldDisposeCheckTask(hitEntity, this);
        hitEntity.addImpalement(thisImpalement);

        double max = hit.getEyeLocation().getY();
        double feet = hit.getLocation().getY();
        double diff = max - feet;

        double heightOffset = Math.max(0, Math.min(cur.getY() - feet, hit.getHeight()));

        boolean followHead = !Config.Combat.IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS.contains(hitEntity.type())
            && heightOffset >= diff * Config.Combat.IMPALEMENT_HEAD_ZONE_RATIO;
        DisplayUtil.itemDisplayFollow(hitEntity, display,  velocity.clone().normalize(), heightOffset, followHead,
            exitImpalementStatePredicate, this, null, null);
    }

    public void setRetrieved(boolean retrieved) {
        this.retrieved = retrieved;
        SwordScheduler.runBukkitTaskLater(() -> setRetrieved(false), 60, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles when the thrower catches their own thrown item midair.
     * <p>
     * Returns the item to inventory and disposes of the display.
     */
    protected void onCatch() {
        thrower.giveItem(display.getItemStack());
        dispose();
    }

    /**
     * Evaluates the item’s current interaction state each tick.
     * <p>
     * Checks for collisions with entities or blocks.
     */
    public void evaluate() {
        if (hit || grounded || caught) return;
        hitCheck();
        groundedCheck();
    }

    /**
     * Checks if the thrown item has collided with a block and become grounded.
     */
    public void groundedCheck() {
        RayTraceResult hitBlock = display.getWorld()
            .rayTraceBlocks(cur, velocity,
                cur.toVector()
                    .subtract(prev.toVector())
                    .lengthSquared() * Config.Detection.THROW_GROUND_CHECK_MULTIPLIER,
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

    /**
     * Checks for collision with entities using ray tracing.
     * <p>
     * Determines whether the item hits an enemy or is caught by its thrower.
     */
    public void hitCheck() {
        Predicate<Entity> effFilter = getFilter();

        if (prev == null) {
            disposeWithNewInteractiveItem();
        }

        RayTraceResult hitEntity = display.getWorld()
            .rayTraceEntities(prev, velocity,
                cur.toVector()
                    .subtract(prev.toVector())
                    .lengthSquared() * Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER,
                Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER, effFilter);

        if (hitEntity == null) return; // Again, might change this later for non-living damageables.

        if (!(hitEntity.getHitEntity() instanceof LivingEntity)) return;

        if (thrower.isSelf((LivingEntity) hitEntity.getHitEntity())) {
            caught = true;
        }
        else {
            hit = true;
            // Assign the hit entity (can only be one), maybe more actually effect though AOE damage from blocks or larger weapons
            this.hitEntity = SwordEntityArbiter.getOrAdd((LivingEntity) hitEntity.getHitEntity());
        }
    }

    private @NotNull Predicate<Entity> getFilter() {
        Predicate<Entity> filter = entity ->
                        entity.getUniqueId() != display.getUniqueId() &&
                        (entity instanceof LivingEntity l) &&
                        !l.isDead();
        // Throwing a weapon should not immediately result in catching it, therefore a grace period is in place.
        return timeStep.get() < Config.Timing.THROWN_ITEMS_CATCH_GRACE_PERIOD ?
            entity -> filter.test(entity) && entity.getUniqueId() != thrower.getUniqueId() :
            filter;
    }

    /**
     * Determines the correct {@link Transformation} for the item display based on its type.
     * <p>
     * Ensures proper orientation in-hand and mid-flight.
     */
    public void determineOrientation() {
        String name = display.getItemStack().getType().toString();
        Vector3f base = new Vector3f(xDisplayOffset, yDisplayOffset, zDisplayOffset);

        // *** These numbers are fine for now, config later but this system may change.

        if (name.endsWith("_SWORD")) {
            display.setTransformation(new Transformation(
                    base.add(new Vector3f()),
                    new Quaternionf()
                            .rotateY((float) Math.PI/2)
                            .rotateZ((float) Math.PI/2),
                    new Vector3f(1,1,1),
                    new Quaternionf()
            ));
        }
        else if (name.endsWith("AXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")) {
            display.setTransformation(new Transformation(
                    base.add(new Vector3f()),
                    new Quaternionf().rotateY((float) -Math.PI/2)
                            .rotateZ((float) Math.PI/4),
                    new Vector3f(1.5f,1.5f,1.5f),
                    new Quaternionf()
            ));
        }
        else if (display.getItemStack().getType() == Material.SHIELD) {
            display.setTransformation(new Transformation(
                    base.add(new Vector3f(0,0,0)),
                    new Quaternionf().rotateY((float) (Math.PI/1.01f) * 0),
                    new Vector3f(1,1,1),
                    new Quaternionf()
            ));
        }
        else {
            display.setTransformation(new Transformation(
                    base.add(new Vector3f()),
                    new Quaternionf().rotateZ((float) Math.PI/8),
                    new Vector3f(1,1,1),
                    new Quaternionf()
            ));
        }
    }

    // -----------------------------------------------------------------------
    // Throw style
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link ItemThrowStyle} for this item, reading from the item's PDC first,
     * then falling back to a material-based default.
     * <p>
     * The result is cached after the first call.
     */
    protected ItemThrowStyle getThrowStyle() {
        if (throwStyle != null) return throwStyle;
        if (itemStack != null && !itemStack.isEmpty()) {
            String stored = KeyRegistry.getKeyField(itemStack, KeyRegistry.THROW_STYLE_KEY, PersistentDataType.STRING);
            if (stored != null) {
                try {
                    throwStyle = ItemThrowStyle.valueOf(stored);
                    return throwStyle;
                } catch (IllegalArgumentException ignored) { }
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

    // -----------------------------------------------------------------------
    // Landing marker (issue #15)
    // -----------------------------------------------------------------------

    /**
     * Whether this thrown item should display a landing prediction marker.
     * Subclasses that handle their own landing feedback (e.g. UmbralBlade) may override this.
     */
    protected boolean shouldShowLandingMarker() {
        return true;
    }

    /**
     * Simulates the flight trajectory to find the first block that will be struck,
     * returning the hit position and the face that was hit.
     *
     * @return the predicted landing, or {@code null} if no block is found within range
     */
    private LandingPrediction precomputeLanding() {
        if (origin == null || positionFunction == null) return null;

        double maxTime = timeCutoff > 0 ? timeCutoff : 200;
        double step = timeScalingFactor > 0 ? timeScalingFactor : 1.0;

        Location prevLoc = origin.clone();
        for (double t = step; t <= maxTime + step; t += step) {
            Location nextLoc = origin.clone().add(positionFunction.apply(t));

            Vector diff = nextLoc.toVector().subtract(prevLoc.toVector());
            double len = diff.length();
            if (len < 1e-6) {
                prevLoc = nextLoc;
                continue;
            }

            RayTraceResult result = prevLoc.getWorld().rayTraceBlocks(
                prevLoc, diff.normalize(), len + 0.3, FluidCollisionMode.NEVER, true);

            if (result != null && result.getHitBlock() != null && !result.getHitBlock().getType().isAir()) {
                BlockFace face = result.getHitBlockFace();
                Location hitPos = result.getHitPosition().toLocation(prevLoc.getWorld());
                return new LandingPrediction(hitPos, face != null ? face : BlockFace.UP);
            }
            prevLoc = nextLoc;
        }
        return null;
    }

    /**
     * Spawns the landing prediction marker at the precomputed landing location.
     * <p>
     * Ground landings get a flat ⊗ marker lying 0.2 blocks above the surface with an upward
     * particle stream. Wall landings get a vertical marker with {@link Display.Billboard#FIXED}.
     */
    private void spawnLandingMarker(LandingPrediction prediction) {
        if (prediction == null || display == null || !display.isValid()) return;

        boolean isWall = prediction.face() != BlockFace.UP && prediction.face() != BlockFace.DOWN;
        Location markerPos = prediction.position().clone();
        markerPos.setYaw(0);
        markerPos.setPitch(0);

        if (!isWall) {
            markerPos.add(0, 0.2, 0);
        } else {
            // Float the marker slightly off the wall surface
            markerPos.add(
                prediction.face().getModX() * 0.05,
                prediction.face().getModY() * 0.05,
                prediction.face().getModZ() * 0.05
            );
        }

        landingMarker = (TextDisplay) display.getWorld().spawnEntity(markerPos, EntityType.TEXT_DISPLAY);
        landingMarker.text(
            Component.text(" ︾ \n》 《\n ︽ ")
                .color(TextColor.color(255, 0, 0))
                .decorate(TextDecoration.BOLD)
        );
        landingMarker.setBillboard(Display.Billboard.FIXED);
        landingMarker.setDefaultBackground(false);
        landingMarker.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        landingMarker.setBrightness(new Display.Brightness(15, 15));
        landingMarker.setShadowed(true);
        landingMarker.setGlowColorOverride(Color.fromRGB(255, 0, 0));
        landingMarker.setGlowing(true);

        Transformation tr;
        if (!isWall) {
            markerPos.setDirection(thrower.getFlatDir());
            // Rotate the text face to point upward so the marker lies flat on the ground
            tr = new Transformation(
                new Vector3f(0, 0, 1.5f),
                new Quaternionf().rotateX((float) (-Math.PI / 2)),
                new Vector3f(landingMarkerSize),
                new Quaternionf()
            );
        } else {
            tr = new Transformation(
                new Vector3f(0, -1.5f, 0),
                new Quaternionf().rotateY(wallFaceYaw(prediction.face())),
                new Vector3f(landingMarkerSize),
                new Quaternionf()
            );
        }
        landingMarker.setTransformation(tr);

        // Particle stream from the exact landing point (at ground/wall surface, below the text)
        Location streamBase = prediction.position().clone();
        landingParticleTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            null,
            () -> Prefab.Particles.LANDING_STREAM.display(streamBase),
            null,
            0, 150,
            ThrownItem.class, "landingMarkerStream",
            new PredicateRunnablePair(() -> landingMarker == null || !landingMarker.isValid(), null)
        );
    }

    /** Returns the Y-axis rotation (radians) that makes a FIXED TextDisplay face outward from a wall. */
    private float wallFaceYaw(BlockFace face) {
        return switch (face) {
            case NORTH -> 0f;
            case SOUTH -> (float) Math.PI;
            case EAST -> (float) (Math.PI / 2);
            case WEST -> (float) (-Math.PI / 2);
            default -> 0f;
        };
    }

    /** Removes the landing prediction marker and cancels the particle stream task. */
    private void removeLandingMarker() {
        if (landingMarker != null && landingMarker.isValid()) {
            landingMarker.remove();
        }
        landingMarker = null;
        if (landingParticleTask != null && !landingParticleTask.isCancelled()) {
            landingParticleTask.cancel();
        }
        landingParticleTask = null;
    }

    /** Holds the precomputed landing position and the block face that was struck. */
    private record LandingPrediction(Location position, BlockFace face) {}

    public void setTimeStep(int timeStep) {
        this.timeStep.set(timeStep);
    }

    public Location getOrigin() {
        return origin.clone();
    }

    public Location getCur() {
        return cur.clone();
    }

    public Location getPrev() {
        return prev.clone();
    }

    public Vector getTo() {
        return to.clone();
    }

    public Vector getVelocity() {
        return velocity.clone();
    }


    /**
     * Disposes of the item by naturally dropping its item form into the world.
     * <p>
     * Used after hitting entities or ending its trajectory naturally.
     */
    public void disposeWithNewInteractiveItem() {
        if (display == null) return;

        final Location dropLocation = hitEntity != null ? hitEntity.self().getLocation() : display.getLocation();

        InteractiveItemArbiter.dropNaturally(dropLocation, display.getItemStack());

        dispose();
    }

    /**
     * Cleanly disposes of the item display and cancels any running tasks.
     * <p>
     * Should be called when the thrown item is collected or deleted.
     */
    public void dispose() {
        removeLandingMarker();
        if (display != null) {
            display.remove(); // TODO: load chunks whenever something is being despawned.
        }
        if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
    }
}
