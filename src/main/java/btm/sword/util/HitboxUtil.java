package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HitboxUtil {
	public static HashSet<LivingEntity> line(LivingEntity executor, Location o, Vector e, double maxRange, double thickness) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		for (double i = 0; i < maxRange; i += thickness) {
			hit.addAll(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));
		}
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		return hit;
	}
	
	public static LivingEntity lineFirst(LivingEntity executor, Location o, Vector e, double maxRange, double thickness) {
		for (double i = 0; i < maxRange; i += thickness) {
			List<LivingEntity> hits = new ArrayList<>(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));

			for (LivingEntity t : hits) {
				if (!t.isDead() && !t.equals(executor)) {
					return t;
				}
			}
		}
		return null;
	}
	
	
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
	
	public static HashSet<LivingEntity> sphere(LivingEntity executor, Location o, double radius, boolean removeExecutor) {
		HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(radius));
		hit.removeIf(Entity::isDead);
		if (removeExecutor) hit.remove(executor);
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
	
	// plus shaped, in to out (for now)
	public static LivingEntity multipleRayTrace(LivingEntity executor, double maxRange, int numberExtraRays, double spacingWidth) {
		int range = (int) Math.round(maxRange);
		RayTraceResult result = executor.rayTraceEntities(range);
		Location o = executor.getEyeLocation();
		Vector e = o.getDirection();
		List<Vector> basis = VectorUtil.getBasis(o , e);
		Vector rightStep = basis.getFirst().normalize().multiply(spacingWidth);
		Vector upStep = basis.get(1).normalize().multiply(spacingWidth);
		
		int i = 0;
		while (result == null && i < numberExtraRays) {
			result = executor.getWorld().rayTraceEntities(o.clone().add(rightStep.clone().multiply(i+1)) , e, range);
			if (result == null)
				result = executor.getWorld().rayTraceEntities(o.clone().add(rightStep.clone().multiply(-1*(i+1))) , e, range);
			if (result == null)
				result = executor.getWorld().rayTraceEntities(o.clone().add(upStep.clone().multiply(i+1)) , e, range);
			if (result == null)
				result = executor.getWorld().rayTraceEntities(o.clone().add(upStep.clone().multiply(-1*(i+1))) , e, range);
		}
		
		if (result != null && result.getHitEntity() instanceof LivingEntity target && !target.isDead())
			return target;
		
		return null;
	}
	
	public static HashSet<LivingEntity> sphereAtRayHit(LivingEntity executor, double maxRange, double radius, Vector offsetFromHit, boolean removeExecutor) {
		HashSet<LivingEntity> hit = new HashSet<>();
		
		LivingEntity origin = rayTrace(executor, maxRange);
		
		if (origin == null)
			return hit;
		
		return sphere(executor, origin.getLocation().add(offsetFromHit), radius, removeExecutor);
	}
}
