package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class DirectionalPointVisual extends VisualEffect {
	Vector positionOffset;
	
	public DirectionalPointVisual(List<Particle> particles, int count, double offset, Vector positionOffset) {
		super(particles, count, offset);
		this.positionOffset = positionOffset;
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		Vector forward = direction.clone().normalize();
		Vector upReference = new Vector(0,1,0);
		if (Math.abs(forward.dot(upReference)) > 0.999) {
			upReference = new Vector(1,0,0);  // forward is too vertical, switch reference
		}
		Vector right = upReference.clone().crossProduct(forward).normalize();
		Vector up = forward.clone().crossProduct(right).normalize();
		
		Vector worldOffset = right.clone().multiply(positionOffset.getX())
							.add(up.clone().multiply(positionOffset.getY()))
							.add(forward.clone().multiply(positionOffset.getZ()));
		
		Location l = origin.add(worldOffset);
		
		for (Particle p : particles) {
			origin.getWorld().spawnParticle(p, l, count, offset, offset, offset);
		}
	}
}
