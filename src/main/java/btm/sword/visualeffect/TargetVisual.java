package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class TargetVisual extends VisualEffect {
	public TargetVisual(List<Particle> particles, int count, double offset) {
		super(particles, count, offset);
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			for (Particle p : particles) {
				origin.getWorld().spawnParticle(p, target.getLocation().add(0, 1, 0), count, offset, offset, offset);
			}
		}
	}
}
