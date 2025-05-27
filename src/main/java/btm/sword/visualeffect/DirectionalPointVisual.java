package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class DirectionalPointVisual extends VisualEffect {
	Vector positionOffset;
	
	public DirectionalPointVisual(List<ParticleData> particles, int count, double offset, Vector positionOffset) {
		super(particles, count, offset);
		this.positionOffset = positionOffset;
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		World world = origin.getWorld();
		
		Vector forward = direction.clone();
		Vector ref = new Vector(0,1,0);
		if (Math.abs(forward.dot(ref)) > 0.999) {
			double yaw = Math.toRadians(origin.getYaw());
			ref = new Vector(-Math.sin(yaw),0,Math.cos(yaw));
		}
		Vector right = ref.clone().crossProduct(forward).normalize();
		Vector up = forward.clone().crossProduct(right).normalize();
		
		Vector worldOffset = right.clone().multiply(positionOffset.getX())
							.add(up.clone().multiply(positionOffset.getY()))
							.add(forward.clone().multiply(positionOffset.getZ()));
		
		Location l = origin.add(worldOffset);
		
		for (ParticleData p : particles) {
			if (p.getOptions() != null) {
				world.spawnParticle(p.getParticle(), l, count, offset, offset, offset, 0, p.getOptions());
			}
			else {
				world.spawnParticle(p.getParticle(), l, count, offset, offset, offset);
			}
		}
	}
}
