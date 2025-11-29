package btm.sword.system.action.utility.thrown;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import btm.sword.system.SwordScheduler;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.util.Prefab;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.entity.EntityUtil;
import btm.sword.util.math.Basis;
import btm.sword.util.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.TextColor;

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
    protected Consumer<ItemDisplay> displaySetupInstructions;

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
    protected int timeStep = 0;

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

    protected BukkitTask disposeTask;

    protected boolean setupSuccessful;

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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (setupSuccessful) {
                    afterSpawn();
                    cancel();
                    return;
                }
                try {
                    LivingEntity e = thrower.self();
                    display = (ItemDisplay) e.getWorld().spawnEntity(e.getEyeLocation(), EntityType.ITEM_DISPLAY);
                    displaySetupInstructions.accept(display);
                    setupSuccessful = true;
                } catch (Exception e) {
                    e.addSuppressed(e);
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
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

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (thrower.isThrowCancelled()) {
                    display.remove();
                    ThrowAction.throwCancel(thrower);
                    thrower.setThrownItem(null);
                    cancel();
                    return;
                }
                else if (thrower.isThrowSuccessful()) {
                    thrower.setItemTypeInHand(Material.AIR, true);
                    cancel();
                    return;
                }

                if (thrower instanceof SwordPlayer sp) {
                    if (!sp.isChangingHandIndex() && sp.getCurrentInvIndex() == sp.getThrownItemIndex()) {
                        if (i < 10)
                            sp.itemNameDisplay("- HURL IT AT 'EM SOLDIER! -", TextColor.color(100, 100, 100), null);
                        else
                            sp.itemNameDisplay("| HURL IT AT 'EM SOLDIER! |", TextColor.color(150, 150, 150), null);

                        if (i > 20) i = 0;
                        i++;
                    }
                }

                throwerEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));

                DisplayUtil.smoothTeleport(display, 2);
                display.teleport(throwerEntity.getEyeLocation());
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    sp.setThrewItem(false);
                }
            }.runTaskLater(Sword.getInstance(), 2);
        }

        generateFunctions(initialVelocity);

        handleOnReleaseActions();

        xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
        determineOrientation();

        new BukkitRunnable() {
            @Override
            public void run() {
                thrower.message("Time Step: " + timeStep);
                thrower.message("Time Cutoff: " + timeCutoff);
                if (grounded || hit || caught || display.isDead() || (timeCutoff > 0 && timeStep * timeScalingFactor > timeCutoff)) {
                    String reason = grounded ? "grounded" :
                        hit ? "hit" :
                            caught ? "caught" :
                                display.isDead() ? "display dead" :
                                    (timeCutoff > 0 && timeStep * timeScalingFactor > timeCutoff) ? "time cutoff" :
                                        "unknown";
                    thrower.message("Ending due to: " + reason);

                    onEnd();
                    cancel();
                    return;
                }

                applyFunctions();

                if (!prev.equals(cur) && cur.clone().subtract(prev).toVector().dot(velocity) > 0) {
                    DisplayUtil.smoothTeleport(display, 1);
                }

                teleport();
                rotate();

                Prefab.Particles.THROW_TRAIl.display(cur); // TODO: #119 - Make type of particles dynamic
                if (blockTrail != null && timeStep % 3 == 0) // TODO: #119 - Make period dynamic
                    blockTrail.display(cur);

                evaluate();
                prev = cur.clone();
                timeStep++; // Step time value forward for next iteration
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }

    protected void teleport() {
        String name = display.getItemStack().getType().toString();
        if (name.endsWith("_SWORD")) {
            display.teleport(cur.setDirection(velocity));
        }
        else {
            display.teleport(cur.setDirection(currentBasis.forward()));
        }
    }

    protected void applyFunctions() {
        double time;
        if (timeScalingFactor < 0) {
            time = timeStep;
        }
        else {
            time = timeStep * timeScalingFactor;
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
     * Applies appropriate rotation to the display based on item type.
     * <p>
     * Ensures visually realistic spin behavior per tool class.
     */
    private void rotate() {
        Transformation curTr = display.getTransformation();
        Quaternionf curRotation = curTr.getLeftRotation();
        Quaternionf newRotation;
        String name = display.getItemStack().getType().toString();

        // TODO: #127 - Make more extensible somehow?
        if (name.endsWith("_SWORD")) {
            newRotation = curRotation.rotateZ((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_SWORD);
        }
        else if (name.endsWith("_AXE")) {
            newRotation = curRotation.rotateZ((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_AXE);
        }
        else if (name.endsWith("_HOE")) {
            newRotation = curRotation.rotateZ((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_HOE);
        }
        else if (name.endsWith("_PICKAXE")) {
            newRotation = curRotation.rotateZ((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_PICKAXE);
        }
        else if (name.endsWith("_SHOVEL")) {
            newRotation = curRotation.rotateZ((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_SHOVEL);
        }
        else if (display.getItemStack().getType() == Material.SHIELD) {
            newRotation = curRotation.rotateX((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_SHIELD);
        }
        else {
            newRotation = curRotation.rotateX((float) Config.Physics.THROWN_ITEMS_ROTATION_SPEED_DEFAULT_SPEED);
        }

        display.setTransformation(
                new Transformation(
                        curTr.getTranslation(),
                        newRotation,
                        curTr.getScale(),
                        curTr.getRightRotation()
                )
        );
    }

    /**
     * Called once the throw has completed its trajectory or interaction.
     * <p>
     * Delegates to the correct outcome handler depending on state flags.
     */
    protected void onEnd() {
        if (caught) onCatch();
        else if (hit) onHit();
        else if (grounded) onGrounded();
        timeStep = 0;
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

        new BukkitRunnable() {
            @Override
            public void run() {
                Location land = probe.clone();
                land.setDirection(to.normalize());
                DisplayUtil.smoothTeleport(display, 1);
                display.teleport(land);
            }
        }.runTaskLater(Sword.getInstance(), 1L);

        startDisposeTask(step);
    }

    protected void startDisposeTask(Vector step) {
        disposeTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (display.isDead()) {
                    cancel();
                }

                if (tick >= Config.Timing.THROWN_ITEMS_DISPOSAL_TIMEOUT) {
                    if (!display.isDead()) display.remove();
                    cancel();
                }

                Prefab.Particles.THROWN_ITEM_MARKER.display(cur.clone().add(step));
                Prefab.Particles.THROWN_ITEM_MARKER.display(cur);
                Prefab.Particles.THROWN_ITEM_MARKER.display(cur.clone().subtract(step));

                tick += Config.Timing.THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL;
            }
        }.runTaskTimer(Sword.getInstance(), 1L, Config.Timing.THROWN_ITEMS_DISPOSAL_CHECK_INTERVAL);
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

        if (name.endsWith("_SWORD") || name.endsWith("AXE")) {
            startImpalementTask(hitEntity);
            startLifecycleCheckTask(hitEntity);
        }
        else {
            nonImpalingImpact(hitEntity);
        }
    }

    protected void nonImpalingImpact(SwordEntity target) {
        target.hit(thrower, Prefab.Attacks.thrownWeapon,
            velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER));

        target.world().createExplosion(target.getChestLocation(),
            Config.Combat.THROWN_DAMAGE_OTHER_EXPLOSION_POWER,
            Config.World.EXPLOSIONS_SET_FIRE,
            Config.World.EXPLOSIONS_BREAK_BLOCKS);

        disposeNaturally();
    }

    private void startImpalementTask(SwordEntity target) {
        Vector kb = EntityUtil.isOnGround(target.self()) ?
            velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED) :
            VectorUtil.getProjOntoPlane(velocity, Config.Direction.UP()).multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE);

        impale(target.self());
        target.hit(thrower, Prefab.Attacks.thrownWeapon, kb);

        new BukkitRunnable() {
            @Override
            public void run() {
                RayTraceResult pinnedBlock = target.self().getWorld().rayTraceBlocks(
                    target.getChestLocation(), velocity.clone().normalize(),
                    Config.Detection.THROW_PIN_RAY_DISTANCE, FluidCollisionMode.NEVER,
                    true);

                if (pinnedBlock == null || pinnedBlock.getHitBlock() == null || pinnedBlock.getHitBlock().getType().isAir())
                    return;

                startPinCheckTask(target);
            }
        }.runTaskLater(Sword.getInstance(), Config.Timing.THROWN_ITEMS_PIN_DELAY);
    }

    protected void startPinCheckTask(SwordEntity target) {
        float yaw = cur.setDirection(velocity.clone().multiply(-1)).getYaw();
        target.self().setBodyYaw(yaw);
        target.addPinningImpalement();

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (display.isDead() || i > Config.Combat.IMPALEMENT_PIN_MAX_ITERATIONS ||
                    target.isGrabbed() || isRetrieved()) {
                    target.removePinningImpalement();
                    if (!display.isDead()) disposeNaturally();
                    cancel();
                }
                target.self().setBodyYaw(yaw);
                target.self().setVelocity(new Vector());

                i += Config.Combat.IMPALEMENT_PIN_CHECK_INTERVAL;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, Config.Combat.IMPALEMENT_PIN_CHECK_INTERVAL);
    }

    public void setRetrieved(boolean retrieved) {
        this.retrieved = retrieved;
        SwordScheduler.runBukkitTaskLater(() -> setRetrieved(false), 60, TimeUnit.MILLISECONDS);
    }

    protected void startLifecycleCheckTask(SwordEntity target) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (display == null || target == null) {
                    disposeNaturally();
                    cancel();
                    return;
                }

                if (display.isDead()) {
                    target.removeImpalement();
                    cancel();
                }
                else if (target.isDead()) {
                    disposeNaturally();
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }

    /**
     * Handles when the thrower catches their own thrown item midair.
     * <p>
     * Returns the item to inventory and disposes of the display.
     */
    protected void onCatch() {
//        thrower.message("Caught it!");
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
            disposeNaturally();
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
            this.hitEntity = SwordEntityArbiter.getOrAdd((LivingEntity) hitEntity.getHitEntity());
        }
    }

    private @NotNull Predicate<Entity> getFilter() {
        Predicate<Entity> filter = entity ->
                        entity.getUniqueId() != display.getUniqueId() &&
                        (entity instanceof LivingEntity l) &&
                        !l.isDead();
        // Throwing a weapon should not immediately result in catching it, therefore a grace period is in place.
        return timeStep < Config.Timing.THROWN_ITEMS_CATCH_GRACE_PERIOD ?
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

    /**
     * Impales a {@link LivingEntity} when struck, embedding the item visually and applying follow behavior.
     *
     * @param hit The living entity being impaled.
     */
    public void impale(LivingEntity hit) {
        hitEntity.addImpalement();

        double max = hit.getEyeLocation().getY();
        double feet = hit.getLocation().getY();
        double diff = max - feet;

        double heightOffset = Math.max(0, Math.min(cur.getY() - feet, hit.getHeight()));

        boolean followHead = !Config.Combat.IMPALEMENT_HEAD_FOLLOW_EXCEPTIONS.contains(hitEntity.type())
                && heightOffset >= diff * Config.Combat.IMPALEMENT_HEAD_ZONE_RATIO;
        DisplayUtil.itemDisplayFollow(hitEntity, display,  velocity.clone().normalize(), heightOffset, followHead,
            null, null, null, null);
    }

    /**
     * Disposes of the item by naturally dropping its item form into the world.
     * <p>
     * Used after hitting entities or ending its trajectory naturally.
     */
    public void disposeNaturally() {
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
        if (display != null) {
            display.remove(); // TODO: load chunks whenever something is being despawned.
        }
        if (disposeTask != null && !disposeTask.isCancelled()) disposeTask.cancel();
    }
}
