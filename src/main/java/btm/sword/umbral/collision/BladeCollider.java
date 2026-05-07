package btm.sword.umbral.collision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import btm.sword.util.entity.HitboxUtil;

/**
 * Stateless world-query helper used by UmbralBlade FSM states to detect block and entity
 * collisions along a per-tick motion segment.
 * <p>
 * <b>Ownership:</b> {@code BladeCollider} owns no mutable state. The caller is responsible for
 * supplying the segment endpoints and any per-tick "previous position" cache — that cache lives
 * inside the producing {@link btm.sword.umbral.motion.BladeMotionDriver motion driver}, since
 * only the driver knows what "previous tick" means semantically.
 * <p>
 * <b>Composition:</b> wraps Bukkit's {@link World#rayTraceBlocks} / {@link World#rayTraceEntities}
 * for single-hit queries and {@link HitboxUtil#secant} for multi-hit sweeps. It does not
 * reimplement raytracing.
 */
public final class BladeCollider {

    private BladeCollider() {}

    /**
     * Performs a block-only raytrace from {@code from} to {@code to} and returns the first
     * non-air block intersected, if any.
     *
     * @param from the segment start in world space
     * @param to   the segment end in world space; must share a world with {@code from}
     * @param mode how fluid blocks are treated by the trace
     * @return the first {@link BlockHit} along the segment, or {@link Optional#empty()} if the
     *         segment hits nothing
     */
    public static Optional<BlockHit> scanBlocks(Location from, Location to, FluidCollisionMode mode) {
        Vector segment = to.toVector().subtract(from.toVector());
        double length = segment.length();
        if (length < 1e-6) return Optional.empty();

        Vector direction = segment.clone().multiply(1.0 / length);
        RayTraceResult result = from.getWorld().rayTraceBlocks(from, direction, length, mode, true);
        if (result == null) return Optional.empty();

        Block block = result.getHitBlock();
        if (block == null || block.getType().isAir()) return Optional.empty();

        Location hitPosition = result.getHitPosition().toLocation(from.getWorld());
        BlockFace face = result.getHitBlockFace() != null ? result.getHitBlockFace() : BlockFace.UP;
        return Optional.of(new BlockHit(result, block, hitPosition, face));
    }

    /**
     * Performs an entity raytrace from {@code from} to {@code to} and returns the first matching
     * entity along the segment, if any.
     *
     * @param from    the segment start in world space
     * @param to      the segment end in world space; must share a world with {@code from}
     * @param raySize the radial thickness of the ray, used as Bukkit's {@code raySize} parameter
     * @param filter  predicate to include or exclude candidate entities; may be {@code null} to
     *                accept all entities
     * @return the first {@link EntityHit} along the segment, or {@link Optional#empty()} if no
     *         matching entity is intersected
     */
    public static Optional<EntityHit> firstEntity(Location from, Location to, double raySize, Predicate<Entity> filter) {
        Vector segment = to.toVector().subtract(from.toVector());
        double length = segment.length();
        if (length < 1e-6) return Optional.empty();

        Vector direction = segment.clone().multiply(1.0 / length);
        RayTraceResult result = from.getWorld().rayTraceEntities(from, direction, length, raySize, filter);
        if (result == null || result.getHitEntity() == null) return Optional.empty();

        Location hitPosition = result.getHitPosition().toLocation(from.getWorld());
        return Optional.of(new EntityHit(result, result.getHitEntity(), hitPosition));
    }

    /**
     * Sweeps the segment between {@code from} and {@code to} using a sphere walk and returns
     * every {@link LivingEntity} detected along the path. Each detection is reported with the
     * entity's location at the moment of detection; the {@link EntityHit#result()} field is
     * {@code null} for sweep-based hits since no single raytrace produced them.
     * <p>
     * Use this when the blade should affect every entity in its swept volume on a single tick;
     * use {@link #firstEntity} when only the closest hit matters.
     *
     * @param from    the segment start in world space
     * @param to      the segment end in world space; must share a world with {@code from}
     * @param raySize the radial thickness of the sweep, used as the spherical step size
     * @param filter  predicate to include or exclude candidate entities; must not be {@code null}
     * @return the list of {@link EntityHit} detections along the segment, in no particular order;
     *         empty if nothing was detected
     */
    public static List<EntityHit> scanEntities(Location from, Location to, double raySize, Predicate<Entity> filter) {
        HashSet<LivingEntity> hits = HitboxUtil.secant(from, to, raySize, filter);
        if (hits.isEmpty()) return List.of();

        List<EntityHit> out = new ArrayList<>(hits.size());
        for (LivingEntity entity : hits) {
            out.add(new EntityHit(null, entity, entity.getLocation()));
        }
        return out;
    }
}
