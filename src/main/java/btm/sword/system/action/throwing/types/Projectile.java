package btm.sword.system.action.throwing.types;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.action.throwing.ProjectileManager;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * A pure-physics projectile with no display entity and no thrower reference.
 * <p>
 * Use this class for invisible or effect-only projectiles that only need collision detection
 * and callbacks. For a visually-simulated item display projectile see {@link VisualProjectile}.
 * </p>
 * <p>
 * Register instances with {@link ProjectileManager} to have them ticked automatically.
 * </p>
 */
@Getter
@Setter
public class Projectile {
    protected World world;
    protected Vector position;
    protected Vector previousPosition;
    protected Vector velocity;
    protected Vector origin;

    protected Function<Double, Vector> positionFunction;
    protected Function<Double, Vector> velocityFunction;

    /** Raw tick counter — only incremented on the main thread by {@link #tick()}. */
    protected int tick;
    protected double timeScalingFactor = -1;
    protected double timeCutoff = -1;

    protected boolean grounded;
    protected boolean hit;
    protected boolean expired;

    /** UUIDs of entities already struck — prevents multi-hit on fast projectiles. */
    protected final Set<UUID> hitEntities = new HashSet<>();

    /** Called with the struck {@link SwordEntity} when a living entity is hit. */
    public Consumer<SwordEntity> onHitCallback;
    /** Called when the projectile lands on a block. */
    public Runnable onGroundCallback;
    /** Called when the time cutoff is exceeded. */
    public Runnable onExpireCallback;

    /**
     * Constructs a projectile at the given world origin, moving in the given direction at the given speed.
     *
     * @param world     the world in which the projectile exists
     * @param origin    starting position
     * @param direction normalized launch direction
     * @param speed     initial speed in blocks per tick (approximate)
     */
    public Projectile(World world, Vector origin, Vector direction, double speed) {
        this.world = world;
        this.origin = origin.clone();
        this.position = origin.clone();
        this.previousPosition = origin.clone();

        Vector dir = direction.clone().normalize();
        this.velocity = dir.clone().multiply(speed);

        double gravDamper = Config.Physics.THROWN_ITEMS_GRAVITY_DAMPER;
        double horiz = Math.sqrt(dir.getX() * dir.getX() + dir.getZ() * dir.getZ());
        double phi = Math.atan2(dir.getY(), horiz);
        double forwardCoef = speed * Math.cos(phi);
        double upwardCoef = speed * Math.sin(phi);
        Vector flat = dir.clone().setY(0).normalize();
        Vector fwdVel = flat.clone().multiply(forwardCoef);
        Vector upVel = Config.Direction.UP().multiply(upwardCoef);

        positionFunction = t -> flat.clone().multiply(forwardCoef * t)
            .add(Config.Direction.UP().multiply(
                (upwardCoef * t) - (speed * (1.0 / gravDamper) * t * t)));

        velocityFunction = t -> fwdVel.clone()
            .add(upVel.clone()
                .add(Config.Direction.UP().multiply(-speed * (2.0 / gravDamper) * t)));
    }

    /**
     * Convenience factory that constructs and registers the projectile in one step.
     *
     * @param world     the world
     * @param origin    starting position
     * @param direction launch direction (normalized)
     * @param speed     initial speed
     * @return the newly registered {@link Projectile}
     */
    public static Projectile launch(World world, Vector origin, Vector direction, double speed) {
        Projectile p = new Projectile(world, origin, direction, speed);
        ProjectileManager.register(p);
        return p;
    }

    /**
     * Advances one physics step.
     *
     * @return {@code true} to continue ticking; {@code false} when the projectile should be removed
     */
    public boolean tick() {
        double time = timeScalingFactor < 0 ? tick : tick * timeScalingFactor;
        previousPosition = position.clone();
        position = origin.clone().add(positionFunction.apply(time));
        velocity = velocityFunction.apply(time);

        evaluate();
        tick++;

        if (timeCutoff > 0 && time > timeCutoff) {
            expired = true;
            if (onExpireCallback != null) onExpireCallback.run();
        }

        return !grounded && !hit && !expired;
    }

    /**
     * Runs collision checks for this tick.
     */
    protected void evaluate() {
        if (hit || grounded) return;
        hitCheck();
        groundedCheck();
    }

    /**
     * Checks for entity collision along the previous→current sweep.
     */
    protected void hitCheck() {
        double dist = position.clone().subtract(previousPosition).length();
        if (dist < 1e-6) return;

        Location prevLoc = previousPosition.toLocation(world);
        RayTraceResult result = world.rayTraceEntities(
            prevLoc,
            velocity,
            dist * Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER,
            Config.Detection.THROW_HIT_CHECK_DIST_MULTIPLIER,
            entity -> entity instanceof LivingEntity l && !l.isDead()
                && !hitEntities.contains(entity.getUniqueId())
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity le)) return;

        hit = true;
        hitEntities.add(le.getUniqueId());
        SwordEntity target = SwordEntityArbiter.getOrAdd(le);
        if (onHitCallback != null) onHitCallback.accept(target);
    }

    /**
     * Checks whether the projectile has hit a solid block.
     */
    protected void groundedCheck() {
        double dist = position.clone().subtract(previousPosition).length();
        if (dist < 1e-6) return;

        Location prevLoc = previousPosition.toLocation(world);
        RayTraceResult result = world.rayTraceBlocks(
            prevLoc, velocity,
            dist * Config.Detection.THROW_GROUND_CHECK_MULTIPLIER,
            FluidCollisionMode.NEVER, true);

        if (result == null || result.getHitBlock() == null || result.getHitBlock().getType().isAir()) return;

        grounded = true;
        if (onGroundCallback != null) onGroundCallback.run();
    }
}
