package btm.sword.system.action.throwing.types;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.config.Config;
import btm.sword.control.PredicateRunnablePair;
import btm.sword.control.SwordScheduler;
import btm.sword.control.TimeArbiter;
import btm.sword.input.ActivationContext;
import btm.sword.system.action.throwing.ImpactType;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.action.throwing.impale.Impalement;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.entity.impl.ThrowPhase;
import btm.sword.system.item.ItemUsageManager;
import btm.sword.system.item.KeyRegistry;
import btm.sword.utility.Debug;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.DisplayUtil;
import btm.sword.utility.entity.EntityUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A thrown item tied to a specific {@link Combatant} thrower.
 * <p>
 * Extends {@link VisualProjectile} with thrower-aware physics origin, hand management,
 * impalement, and a landing prediction marker. The thrower's look direction drives trajectory
 * unless {@link #setLaunchDirection(Vector)} is called to override it (used by mob AI).
 * </p>
 */
@Getter
@Setter
public class ThrownItem extends VisualProjectile {
    protected final Combatant thrower;

    protected Impalement thisImpalement;

    /** Predicate tested by the impalement follow-task to decide when embedding ends. */
    protected Predicate<VisualProjectile> exitImpalementStatePredicate;

    /** If non-null, overrides {@link btm.sword.utility.Prefab.Attacks#THROWN_WEAPON} for hit resolution. */
    private HitValuePacket hitPacket;

    /**
     * If non-null, overrides the default grounded/airborne knockback calculation.
     * Receives the hit target and returns the knockback vector to apply.
     */
    private Function<SwordEntity, org.bukkit.util.Vector> knockbackFunction;

    // Landing marker (issue #15)
    private TextDisplay landingMarker;
    private TimeArbiter.TaskHandle landingParticleTask;
    private float landingMarkerSize = 4.0f;

    /**
     * Constructs and begins spawning the thrown item display.
     *
     * @param thrower                   the combatant performing the throw
     * @param displaySetupInstructions  applied to the {@link ItemDisplay} immediately after it spawns
     * @param setupPeriod               polling interval in ticks for the spawn-check loop
     */
    public ThrownItem(Combatant thrower, Consumer<ItemDisplay> displaySetupInstructions) {
        this.thrower = thrower;
        this.displaySetupInstructions = displaySetupInstructions;
        setupSuccessful = false;

        xDisplayOffset = -0.6f;
        yDisplayOffset = 0.25f;
        zDisplayOffset = -0.1f;
    }

    /**
     * Spawns the display at the thrower's eye location when the server tick fires.
     * Keeps using {@link ThrowAction}'s task identifier for backward compatibility.
     *
     * @param period polling interval in ticks
     */
    public void setup(int period) {
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

    /**
     * Snapshots the thrower's hand items after the display spawns.
     * For players, this uses the item held at the time of the hold input.
     */
    @Override
    protected void afterSpawn() {
        super.afterSpawn();
        if (thrower instanceof SwordPlayer sp) {
            sp.setMainHandItemStackDuringThrow(sp.getMainItemStackAtTimeOfHold());
            sp.setOffHandItemStackDuringThrow(sp.getOffItemStackAtTimeOfHold());
        } else {
            thrower.setMainHandItemStackDuringThrow(thrower.getItemStackInHand(true));
            thrower.setOffHandItemStackDuringThrow(thrower.getItemStackInHand(false));
        }
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

            if (sp.isInteractingWithEntity()) {
                sp.setThrowPhase(ThrowPhase.SUCCESS);
                sp.setActivationContext(ActivationContext.NORMAL);
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
                            sp.itemNameDisplay("- HURL IT AT 'EM SOLDIER! -",
                                TextColor.color(100, 100, 100), null, Material.GUNPOWDER);
                        else
                            sp.itemNameDisplay("| HURL IT AT 'EM SOLDIER! |",
                                TextColor.color(150, 150, 150), null, Material.GUNPOWDER);

                        if (iteration[0] > 20) iteration[0] = 0;
                        iteration[0]++;
                    }
                }

                throwerEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));

                TimeArbiter.teleportDisplay(display, throwerEntity.getEyeLocation(), null, 2,
                    ThrownItem.class, 228);
            },
            0, 50,
            ThrownItem.class, "onReady",
            new PredicateRunnablePair(
                () -> thrower.getThrowPhase() == ThrowPhase.CANCELLED,
                () -> {
                    display.remove();
                    ThrowAction.throwCancel(thrower);
                    thrower.setThrownItem(null);
                }
            ),
            new PredicateRunnablePair(
                () -> thrower.getThrowPhase() == ThrowPhase.SUCCESS,
                () -> thrower.setItemTypeInHand(Material.AIR, true)
            )
        );
    }

    /**
     * Initializes flight state without starting the timer loop.
     * <p>
     * Called by state classes (e.g. LungingState) that drive physics via {@link #stepFlight()} each tick
     * instead of using the self-contained {@link #onRelease(double)} loop.
     *
     * @param initialVelocity the starting velocity magnitude
     */
    @Override
    public void initFlight(double initialVelocity) {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(true);
            SwordScheduler.runBukkitTaskLater(() ->
                sp.setThrewItem(false),
                100, TimeUnit.MILLISECONDS
            );
        }

        super.initFlight(initialVelocity);

        if (shouldShowLandingMarker()) {
            spawnLandingMarker(precomputeLanding());
        }
    }

    /**
     * Called when the flight loop ends. Removes the landing marker before delegating to the parent.
     */
    @Override
    protected void onEnd() {
        removeLandingMarker();
        String reason = grounded ? "grounded"
            : hit ? "hit"
            : caught ? "caught"
            : display.isDead() ? "display dead" : "time cutoff";
        Debug.combat("throw ended: " + reason);
        super.onEnd();
    }

    /**
     * Handles impalement and knockback when the thrown item hits a living entity.
     */
    @Override
    public void onHit() {
        if (hitEntity == null) return;

        handleItemDamageAndCheckIfBroken();

        if (ImpactType.fromItem(itemStack) == ImpactType.IMPALE) {
            startImpalementTask(hitEntity);
        } else {
            nonImpalingImpact(hitEntity);
        }
    }

    /**
     * Returns the item to the thrower when caught.
     * Ability projectiles (tagged with {@link KeyRegistry#ABILITY_ID_KEY}) refund one
     * slot use instead of placing the raw visual item into the thrower's inventory.
     */
    @Override
    protected void onCatch() {
        ItemStack caughtItem = display.getItemStack();
        if (caughtItem != null && KeyRegistry.hasKey(caughtItem, KeyRegistry.ABILITY_ID_KEY)
                && thrower instanceof SwordPlayer sp) {
            String abilityId = KeyRegistry.getKeyField(caughtItem, KeyRegistry.ABILITY_ID_KEY,
                org.bukkit.persistence.PersistentDataType.STRING);
            sp.getAbilitySlotManager().refundByAbilityId(abilityId);
        } else {
            thrower.giveItem(caughtItem);
        }
        dispose();
    }

    /**
     * Plays the throw sound and removes the item from the correct hand.
     * <ul>
     *   <li>For {@link SwordPlayer}: clears the slot at {@code thrownItemIndex}.</li>
     *   <li>For mobs: detects which hand holds the thrown item and clears that hand.</li>
     * </ul>
     */
    @Override
    protected void handleOnReleaseActions() {
        Prefab.Sounds.THROW.playForAllInRadius(thrower.self());
        if (thrower instanceof SwordPlayer sp) {
            sp.setItemAtIndex(ItemStack.of(Material.AIR), sp.getThrownItemIndex());
        } else {
            boolean isMainHand = thrower.getItemStackInHand(true).isSimilar(itemStack);
            thrower.setItemStackInHand(ItemStack.of(Material.AIR), isMainHand);
        }
        super.handleOnReleaseActions(); // InteractiveItemArbiter.put(this)
    }

    /**
     * Damages the thrown item on a successful hit, potentially breaking it.
     */
    @Override
    public void handleItemDamageAndCheckIfBroken() {
        if (display == null || itemStack == null) return;
        if (itemStack.isEmpty() || ItemUsageManager.isUnbreakable(itemStack)) return;

        int currentDamage = ItemUsageManager.currentItemDamage(itemStack);
        int maxThrownItemUses = 3;
        int onThrowHitDamageToItem = ItemUsageManager.maxItemDamage(itemStack) / maxThrownItemUses;

        if (currentDamage >= onThrowHitDamageToItem * (maxThrownItemUses - 1)) {
            Prefab.Particles.ITEM_THROW_BREAK.display(display.getLocation());
            itemStack.damage(77777777, thrower.self());
            display.remove();
        } else {
            ItemUsageManager.damageItemStack(itemStack, onThrowHitDamageToItem, thrower.self());
        }
    }

    /**
     * Excludes the thrower from the hit filter during the catch grace period.
     *
     * @return the adjusted filter predicate
     */
    @Override
    protected Predicate<Entity> getFilter() {
        Predicate<Entity> filter = entity ->
            entity.getUniqueId() != display.getUniqueId()
                && (entity instanceof LivingEntity l)
                && !l.isDead();
        boolean excludeThrower = isAbilityItem()
            || timeStep.get() < Config.Timing.THROWN_ITEMS_CATCH_GRACE_PERIOD;
        return excludeThrower
            ? entity -> filter.test(entity) && entity.getUniqueId() != thrower.getUniqueId()
            : filter;
    }

    private boolean isAbilityItem() {
        return itemStack != null && KeyRegistry.hasKey(itemStack, KeyRegistry.ABILITY_ID_KEY);
    }

    /**
     * Returns {@code true} if the given entity is the thrower.
     */
    @Override
    protected boolean isOwnerEntity(LivingEntity entity) {
        return thrower.isSelf(entity);
    }

    // -----------------------------------------------------------------------
    // Trajectory hook overrides — supply thrower-derived values
    // -----------------------------------------------------------------------

    /**
     * Returns the thrower's eye location offset by throw origin config values.
     */
    @Override
    protected Location resolveOriginLocation() {
        LivingEntity ex = thrower.self();
        Location o = ex.getEyeLocation();
        Basis basis = VectorUtil.getBasisWithoutPitch(ex);
        return o.add(basis.right().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_FORWARD))
            .add(basis.up().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_UP))
            .add(basis.forward().multiply(Config.Physics.THROWN_ITEMS_ORIGIN_OFFSET_BACK));
    }

    /**
     * Returns the thrower's horizontal basis (ignoring pitch).
     */
    @Override
    protected Basis resolveBasis() {
        return VectorUtil.getBasisWithoutPitch(thrower.self());
    }

    /**
     * Returns the flat throw direction: {@link #launchDirection} if set, otherwise the thrower's look direction
     * with the configured trajectory rotation applied.
     */
    @Override
    protected Vector resolveFlatDir() {
        if (launchDirection != null) return launchDirection.clone().setY(0).normalize();
        return thrower.getFlatDir().rotateAroundY(Config.Physics.THROWN_ITEMS_TRAJECTORY_ROTATION);
    }

    /**
     * Returns the vertical launch angle in radians: derived from {@link #launchDirection} if set,
     * otherwise from the thrower's eye pitch.
     */
    @Override
    protected double resolvePitch() {
        if (launchDirection != null) {
            Vector nd = launchDirection.clone().normalize();
            double horiz = Math.sqrt(nd.getX() * nd.getX() + nd.getZ() * nd.getZ());
            return Math.atan2(nd.getY(), horiz);
        }
        return Math.toRadians(-1 * thrower.self().getEyeLocation().getPitch());
    }

    // -----------------------------------------------------------------------
    // Hit outcome helpers — require thrower attribution
    // -----------------------------------------------------------------------

    /**
     * Applies knockback and a small explosion to a target that was not impaled.
     *
     * @param target the struck entity
     */
    protected void nonImpalingImpact(SwordEntity target) {
        Vector kb = knockbackFunction != null
            ? knockbackFunction.apply(target)
            : velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_OTHER_KNOCKBACK_MULTIPLIER);
        target.hit(thrower, hitPacket != null ? hitPacket : Prefab.Attacks.THROWN_WEAPON, kb);

        target.world().createExplosion(target.getChestLocation(),
            Config.Combat.THROWN_DAMAGE_OTHER_EXPLOSION_POWER,
            Config.World.EXPLOSIONS_SET_FIRE,
            Config.World.EXPLOSIONS_BREAK_BLOCKS);

        if (display.isValid()) {
            disposeWithNewInteractiveItem();
        }
    }

    /**
     * Starts the impalement sequence for sword/axe hits.
     *
     * @param target the struck entity
     */
    public void startImpalementTask(SwordEntity target) {
        Vector kb = knockbackFunction != null
            ? knockbackFunction.apply(target)
            : EntityUtil.isOnGround(target.self())
                ? velocity.clone().multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_GROUNDED)
                : VectorUtil.getProjOntoPlane(velocity, Config.Direction.up())
                    .multiply(Config.Combat.THROWN_DAMAGE_SWORD_AXE_KNOCKBACK_AIRBORNE);

        if (display.isValid()) {
            impale(target.self());
        }
        target.hit(thrower, hitPacket != null ? hitPacket : Prefab.Attacks.THROWN_WEAPON, kb);
    }

    /**
     * Embeds the display entity into the struck living entity, making it follow on impact.
     *
     * @param hit the entity being impaled
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
        DisplayUtil.itemDisplayFollow(hitEntity, display, velocity.clone().normalize(), heightOffset, followHead,
            exitImpalementStatePredicate, this, null, null);
    }

    // -----------------------------------------------------------------------
    // Landing marker (issue #15)
    // -----------------------------------------------------------------------

    /**
     * Whether this thrown item should display a landing prediction marker.
     * Subclasses that handle their own landing feedback (e.g. UmbralBlade) may override this.
     *
     * @return {@code true} by default
     */
    protected boolean shouldShowLandingMarker() {
        return true;
    }

    /**
     * Simulates the flight trajectory to find the first block that will be struck.
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
     *
     * @param prediction the precomputed landing position and face, or {@code null} to skip
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

    // -----------------------------------------------------------------------
    // Dispose override — also removes the landing marker
    // -----------------------------------------------------------------------

    /**
     * For ability projectiles, silently disposes instead of dropping an interactive world item.
     * Prevents knives and other ability throws from littering the ground on entity death or expiry.
     */
    @Override
    public void disposeWithNewInteractiveItem() {
        if (isAbilityItem()) {
            dispose();
            return;
        }
        super.disposeWithNewInteractiveItem();
    }

    /**
     * Disposes of the item display, cancels tasks, and removes any landing marker.
     */
    @Override
    public void dispose() {
        removeLandingMarker();
        super.dispose();
    }

}
