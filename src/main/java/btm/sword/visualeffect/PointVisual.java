package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class PointVisual extends VisualEffect {
	
	public PointVisual(List<Particle> particles, int count, double offset) {
		super(particles, count, offset);
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		for (Particle p : particles) {
			origin.getWorld().spawnParticle(p, origin, count, offset, offset, offset);
		}
	}
}
