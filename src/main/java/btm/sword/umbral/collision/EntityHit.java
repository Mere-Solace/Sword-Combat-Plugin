package btm.sword.umbral.collision;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;

/**
 * Result of a single entity-collision query performed by {@link BladeCollider}.
 * <p>
 * Carries the raw {@link RayTraceResult} alongside the resolved hit {@link Location} and the
 * struck {@link Entity}. The {@code result} field may be {@code null} for entries produced by
 * the multi-hit {@link BladeCollider#scanEntities} sweep, which discovers entities via radial
 * proximity rather than a single ray; in that case {@code position} is the entity's location
 * at the moment of detection. For single-hit lookups via {@link BladeCollider#firstEntity}
 * {@code result} is always non-null.
 *
 * @param result   the underlying raytrace result, or {@code null} for sweep-based detections
 * @param entity   the entity that was struck or detected
 * @param position the world-space position of the hit
 */
public record EntityHit(RayTraceResult result, Entity entity, Location position) {}
