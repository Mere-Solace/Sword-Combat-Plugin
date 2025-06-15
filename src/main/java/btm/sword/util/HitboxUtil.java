package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class HitboxUtil {
	public static HashSet<LivingEntity> lineKnownLength(LivingEntity executor, Location o, Vector e, double maxRange, double thickness) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		for (double i = 0; i < maxRange; i += maxRange / thickness) {
			hit.addAll(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));
		}
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		return hit;
	}
	
	public static HashSet<LivingEntity> line(LivingEntity executor, Location origin, Location end, double thickness) {
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
						!entity.equals(executor)) {
					hit.add(entity);
				}
			}
			cur.add(step);
		}
		
		return hit;
	}
	
	public static HashSet<LivingEntity> sphere(LivingEntity executor, Location o, double radius) {
		HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(radius));
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		return hit;
	}
	
	public static LivingEntity rayTrace(LivingEntity executor, double maxRange) {
		RayTraceResult result = executor.rayTraceEntities((int) Math.round(maxRange));
		if (result == null)
			return null;
		
		if (result.getHitEntity() instanceof LivingEntity target && !target.isDead())
			return target;
		
		return null;
	}
	
	public static HashSet<LivingEntity> sphereAtRayHit(LivingEntity executor, double maxRange, double radius, Vector offsetFromHit) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		LivingEntity origin = rayTrace(executor, maxRange);
		
		if (origin == null)
			return hit;
		
		return sphere(executor, origin.getLocation().add(offsetFromHit), radius);
	}
}
