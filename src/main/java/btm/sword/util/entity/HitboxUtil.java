package btm.sword.util.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Utility class for hitbox-related operations such as detecting entities within specific shapes
 * and performing ray tracing for entity hits in Bukkit.
 * <p>
 * Provides static methods to find entities intersecting lines, spheres, secants, and rays,
 * filtering by distance, thickness, and optional predicates.
 * </p>
 */
public class HitboxUtil {
    /**
     * Returns all {@link LivingEntity} instances within a line-shaped hitbox starting from a location,
     * extending towards a direction vector up to maxRange, with a specified hitbox thickness.
     * The executor is excluded from the results.
     *
     * @param executor the {@link LivingEntity} performing the hitbox check (excluded from results)
     * @param o starting {@link Location} of the line hitbox
     * @param e direction {@link Vector} along which the hitbox extends
     * @param maxRange maximum range (length) to check
     * @param thickness radius of the hitbox around the line
     * @return a set of living entities intersecting the line hitbox, excluding the executor and dead entities
     */
    public static HashSet<LivingEntity> line(LivingEntity executor, Location o, Vector e, double maxRange, double thickness) {
        HashSet<LivingEntity> hit = new HashSet<>();

        for (double i = 0; i < maxRange; i += thickness) {
            hit.addAll(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));
        }
        hit.removeIf(Entity::isDead);
        hit.remove(executor);
        return hit;
    }

    /**
     * Returns the first {@link LivingEntity} hit within a line-shaped hitbox,
     * filtered by a provided {@link Predicate} and excluding the executor.
     *
     * @param executor the living entity performing the hitbox check
     * @param o starting location
     * @param e direction vector
     * @param maxRange maximum range to check along the line
     * @param thickness radius of the hitbox around the line
     * @param filter predicate to filter entities (e.g., team checks)
     * @return the first hit living entity passing the filter, or null if none found
     */
    public static LivingEntity lineFirst(LivingEntity executor, Location o, Vector e, double maxRange, double thickness, Predicate<Entity> filter) {
        for (double i = 0; i < maxRange; i += thickness) {
            List<LivingEntity> hits = new ArrayList<>(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness, filter));

            for (LivingEntity t : hits) {
                if (!t.isDead() && !t.equals(executor)) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Returns all {@link LivingEntity} instances near points equally spaced along a secant
     * between two {@link Location}s, with a given thickness, optionally removing the executor.
     *
     * @param executor the living entity performing the check (optional removal)
     * @param origin the start location of the secant
     * @param end the end location of the secant
     * @param thickness radius of hit checks around points on the secant
     * @param removeExecutor whether to exclude the executor from results
     * @return a set of living entities detected near the secant
     */
    public static HashSet<LivingEntity> secant(LivingEntity executor, Location origin, Location end, double thickness, boolean removeExecutor) {
        HashSet<LivingEntity> hit = new HashSet<>();

        Vector direction = end.clone().subtract(origin).toVector();
        int steps = (int) (direction.length() / (thickness));
        if (steps == 0) steps = 1;

        Vector step = direction.clone().normalize().multiply(thickness);
        Location cur = origin.clone();

        for (int i = 0; i <= steps; i++) {
            for (Entity e : cur.getNearbyLivingEntities(thickness)) {
                if (e instanceof LivingEntity entity &&
                        !entity.isDead()) {
                    hit.add(entity);
                }
            }
            cur.add(step);
        }

        if (removeExecutor)
            hit.remove(executor);

        return hit;
    }

    /**
     * Returns all {@link LivingEntity} instances near points equally spaced along a secant
     * between two {@link Location}s, with a given thickness, optionally removing the executor.
     *
     * @param executor the living entity performing the check (optional removal)
     * @param origin the start location of the secant
     * @param end the end location of the secant
     * @param thickness radius of hit checks around points on the secant
     * @param removeExecutor whether to exclude the executor from results
     * @param filter Predicate for allowing or disallowing certain entities to be detected
     * @return a set of living entities detected near the secant
     */
    public static HashSet<LivingEntity> secant(LivingEntity executor, Location origin, Location end, double thickness, boolean removeExecutor, Predicate<LivingEntity> filter) {
        HashSet<LivingEntity> hit = new HashSet<>();

        Vector direction = end.clone().subtract(origin).toVector();
        int steps = (int) (direction.length() / (thickness));
        if (steps == 0) steps = 1;

        Vector step = direction.clone().normalize().multiply(thickness);
        Location cur = origin.clone();

        for (int i = 0; i <= steps; i++) {
            for (Entity e : cur.getNearbyLivingEntities(thickness)) {
                if (e instanceof LivingEntity entity &&
                        !entity.isDead() &&
                        filter.test(entity)) { // only change
                    hit.add(entity);
                }
            }
            cur.add(step);
        }

        if (removeExecutor)
            hit.remove(executor);

        return hit;
    }

    public static HashSet<LivingEntity> secant(Location origin, Location end, double thickness, Predicate<Entity> filter) {
        HashSet<LivingEntity> hit = new HashSet<>();

        Vector direction = end.clone().subtract(origin).toVector();
        int steps = (int) (direction.length() / (thickness));
        if (steps == 0) steps = 1;

        Vector step = direction.clone().normalize().multiply(thickness);
        Location cur = origin.clone();

        for (int i = 0; i <= steps; i++) {
            for (Entity e : cur.getNearbyLivingEntities(thickness)) {
                if (e instanceof LivingEntity entity &&
                    !entity.isDead() &&
                    filter.test(entity)) { // only change
                    hit.add(entity);
                }
            }
            cur.add(step);
        }

        return hit;
    }

    /**
     * Returns all {@link LivingEntity} instances within a spherical radius around a location,
     * optionally removing the executor.
     *
     * @param executor the entity performing the check (optional removal)
     * @param o the center location of the sphere
     * @param radius the radius of the sphere
     * @param removeExecutor whether to exclude the executor from results
     * @return a set of living entities within the sphere radius
     */
    public static HashSet<LivingEntity> sphere(LivingEntity executor, Location o, double radius, boolean removeExecutor) {
        HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(radius));
        hit.removeIf(Entity::isDead);
        if (removeExecutor) hit.remove(executor);
        return hit;
    }

    /**
     * Performs a ray trace from the executor's eye location forward up to a maximum range,
     * returning the first non-dead {@link LivingEntity} hit, or null if none.
     *
     * @param executor the living entity performing the ray trace
     * @param maxRange the maximum distance to trace
     * @return the first valid hit living entity or null if none
     */
    public static LivingEntity rayTrace(LivingEntity executor, double maxRange) {
        RayTraceResult result = executor.rayTraceEntities((int) Math.round(maxRange));
        if (result == null)
            return null;

        if (result.getHitEntity() instanceof LivingEntity target && !target.isDead())
            return target;

        return null;
    }

    /**
     * Performs a ray trace from a specified origin in a given direction,
     * with maximum distance and ray size, filtering hit entities by a predicate.
     *
     * @param origin the starting location of the ray
     * @param direction the direction vector to trace
     * @param maxDistance maximum distance of the ray
     * @param raySize radius of the ray for hitbox checks
     * @param filter predicate to filter entities during tracing
     * @return the hit entity found or null if none
     */
    public static Entity ray(Location origin, Vector direction, double maxDistance, double raySize, Predicate<Entity> filter) {
        RayTraceResult result = origin.getWorld().rayTraceEntities(origin, direction, maxDistance, raySize, filter);
        return result == null ? null : result.getHitEntity();
    }



    /**
     * Returns all {@link LivingEntity} instances within a spherical radius at the
     * point where a ray from the executor hits an entity, offset by a vector.
     *
     * @param executor the living entity performing the check
     * @param maxRange max range for ray trace
     * @param radius radius of the sphere around the ray hit point
     * @param offsetFromHit offset vector added to the ray hit location
     * @param removeExecutor whether to exclude the executor from results
     * @return set of living entities near the adjusted ray hit location
     */
    public static HashSet<LivingEntity> sphereAtRayHit(LivingEntity executor, double maxRange, double radius, Vector offsetFromHit, boolean removeExecutor) {
        HashSet<LivingEntity> hit = new HashSet<>();

        LivingEntity origin = rayTrace(executor, maxRange);

        if (origin == null)
            return hit;

        return sphere(executor, origin.getLocation().add(offsetFromHit), radius, removeExecutor);
    }
}
