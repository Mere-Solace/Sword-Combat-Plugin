package btm.sword.umbral.motion.drivers;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import btm.sword.umbral.collision.BladeCollider;
import btm.sword.umbral.collision.BlockHit;
import btm.sword.umbral.collision.EntityHit;
import btm.sword.umbral.motion.BladeMotionContext;
import btm.sword.umbral.motion.BladeMotionDriver;
import btm.sword.util.math.Basis;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;

/**
 * Drives the blade along a parametric cubic-Bézier path while running per-tick collision scans
 * against the world.
 *
 * <p>Replaces the inherited {@code stepFlight} pipeline used by {@code LungingState} and
 * {@code GrabImpaleState}'s lunge phase. The driver owns the bezier integration and exposes
 * collision results to the caller via three callbacks:
 * <ul>
 *   <li>{@code onEntity} — invoked when the segment between previous and current samples
 *       intersects a matching entity. Reports DONE.</li>
 *   <li>{@code onBlock} — invoked when the segment intersects a non-air block. Reports DONE.</li>
 *   <li>{@code onCompleteWithoutCollision} — invoked when {@code durationTicks} elapses without
 *       any collision. Reports DONE.</li>
 * </ul>
 * The callbacks each receive the impact direction (segment vector) so consumers can compute
 * impalement orientation or knockback without re-deriving it.</p>
 *
 * <p>The driver's lifecycle is the standard
 * {@link BladeMotionDriver} contract: {@code onInstall} bakes the bezier
 * function and captures origin; each {@code tick} samples and tests; {@code onUninstall} clears
 * internal references. The driver does not import FSM, schedulers, or display handles.</p>
 */
public final class LungingDriver implements BladeMotionDriver {

    private static final double ENTITY_RAY_SIZE = 0.0;

    private final ControlVectors controlVectors;
    private final Basis basis;
    private final double scale;
    private final long durationTicks;
    private final double timeStep;
    private final int teleportDuration;
    private final Predicate<Entity> entityFilter;
    private final BiConsumer<EntityHit, Vector> onEntity;
    private final BiConsumer<BlockHit, Vector> onBlock;
    private final Runnable onCompleteWithoutCollision;

    private Location origin;
    private Location previousSample;
    private Function<Double, Vector> positionFunction;

    /**
     * @param controlVectors             the four cubic-Bézier control points
     * @param basis                      the basis to project control vectors into world space
     * @param scale                      scale factor passed to {@link ControlVectors#adjustToBasis}
     * @param durationTicks              number of ticks before the driver reports DONE without collision
     * @param timeStep                   parametric step size per tick
     * @param teleportDuration           smoothing duration in ticks for the teleport interpolation
     * @param entityFilter               predicate restricting which entities count as a hit
     * @param onEntity                   callback for the first entity intersection along a tick segment
     * @param onBlock                    callback for the first block intersection along a tick segment
     * @param onCompleteWithoutCollision callback fired when the bezier expires with no collision
     */
    public LungingDriver(ControlVectors controlVectors,
                         Basis basis,
                         double scale,
                         long durationTicks,
                         double timeStep,
                         int teleportDuration,
                         Predicate<Entity> entityFilter,
                         BiConsumer<EntityHit, Vector> onEntity,
                         BiConsumer<BlockHit, Vector> onBlock,
                         Runnable onCompleteWithoutCollision) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        this.controlVectors = controlVectors;
        this.basis = basis;
        this.scale = scale;
        this.durationTicks = durationTicks;
        this.timeStep = timeStep;
        this.teleportDuration = teleportDuration;
        this.entityFilter = entityFilter;
        this.onEntity = onEntity;
        this.onBlock = onBlock;
        this.onCompleteWithoutCollision = onCompleteWithoutCollision;
    }

    @Override
    public void onInstall(BladeMotionContext context) {
        ControlVectors adjusted = controlVectors.adjustToBasis(basis, scale);
        positionFunction = BezierUtil.cubicBezier3D(adjusted);
        origin = context.currentLocation();
        previousSample = origin == null ? null : origin.clone();
    }

    @Override
    public Status tick(BladeMotionContext context) {
        if (origin == null || positionFunction == null) {
            return finishComplete();
        }

        double t = context.tickCount() * timeStep;
        Vector sample = positionFunction.apply(t);
        Location target = origin.clone().add(sample);

        Vector direction;
        if (previousSample == null) {
            direction = basis.forward();
        } else {
            direction = target.toVector().subtract(previousSample.toVector());
            if (direction.lengthSquared() < 1e-9) direction = basis.forward();
        }

        if (previousSample != null) {
            Optional<EntityHit> entityHit = BladeCollider.firstEntity(previousSample, target, ENTITY_RAY_SIZE, entityFilter);
            if (entityHit.isPresent()) {
                if (onEntity != null) onEntity.accept(entityHit.get(), direction.clone());
                return Status.DONE;
            }
            Optional<BlockHit> blockHit = BladeCollider.scanBlocks(previousSample, target, FluidCollisionMode.NEVER);
            if (blockHit.isPresent()) {
                if (onBlock != null) onBlock.accept(blockHit.get(), direction.clone());
                return Status.DONE;
            }
        }

        context.teleportTo(target, direction, teleportDuration);
        previousSample = target;

        if (context.tickCount() >= durationTicks) {
            return finishComplete();
        }
        return Status.RUNNING;
    }

    @Override
    public void onUninstall(BladeMotionContext context) {
        origin = null;
        previousSample = null;
        positionFunction = null;
    }

    private Status finishComplete() {
        if (onCompleteWithoutCollision != null) onCompleteWithoutCollision.run();
        return Status.DONE;
    }
}
