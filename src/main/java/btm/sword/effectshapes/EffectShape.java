package btm.sword.effectshapes;

import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Collection;

public abstract class EffectShape {
	double maxDistance;
	
	Collection<LivingEntity> getTargets(Location origin, Vector direction) {
		return origin.getNearbyEntitiesByType(LivingEntity.class, maxDistance, maxDistance, maxDistance);
	}
}
