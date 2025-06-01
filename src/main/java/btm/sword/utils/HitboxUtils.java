package btm.sword.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class HitboxUtils {
	public static HashSet<LivingEntity> arc(Player executor, Location o, Vector e, float maxRange, float minRange, float thickness, float maxAngle, float roll) {
		HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(maxRange));
		
		List<Vector> basis = VectorUtils.getBasis(o, e);
		VectorUtils.rotateBasis(basis, roll, 0);
		
		for (LivingEntity target : hit) {
			Vector toTarget = target.getEyeLocation().subtract(o).toVector();
			
			double forwardDist = toTarget.dot(basis.getLast());
			double sideOffset = Math.abs(toTarget.dot(basis.getFirst()));
			double upOffset = Math.abs(toTarget.dot(basis.get(1)));
			
			if (target.isDead() ||
					forwardDist < minRange ||
					sideOffset > maxRange * Math.abs(Math.cos(Math.min(Math.PI, Math.toRadians(maxAngle)))) ||
					upOffset > thickness)
				hit.remove(target);
		}
		hit.remove(executor);
		return hit;
	}
	
	public static HashSet<LivingEntity> line(Player executor, Location o, Vector e, float maxRange, float thickness) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		for (float i = 0; i < maxRange; i += maxRange / thickness) {
			hit.addAll(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));
		}
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		return hit;
	}
	
	public static HashSet<LivingEntity> sphere(Player executor, Location o, float radius) {
		HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(radius));
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		return hit;
	}
	
	public static LivingEntity rayTrace(Player executor, float maxRange) {
		RayTraceResult result = executor.rayTraceEntities(Math.round(maxRange));
		if (result == null)
			return null;
		
		if (result.getHitEntity() instanceof LivingEntity target && !target.isDead())
			return target;
		
		return null;
	}
	
	public static HashSet<LivingEntity> sphereAtRayHit(Player executor, float maxRange, float radius, Vector offsetFromHit) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		LivingEntity origin = rayTrace(executor, maxRange);
		
		if (origin == null)
			return hit;
		
		return sphere(executor, origin.getLocation().add(offsetFromHit), radius);
	}
}
