package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class TargetVisual extends VisualEffect {
	public TargetVisual(List<ParticleData> particles, int count, double offset) {
		super(particles, count, offset);
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			for (ParticleData p : particles) {
				if (p.getOptions() != null) {
					origin.getWorld().spawnParticle(p.getParticle(), target.getLocation().add(0, 1, 0), count, offset, offset, offset, 0, p.getOptions());
				} else {
					origin.getWorld().spawnParticle(p.getParticle(), target.getLocation().add(0, 1, 0), count, offset, offset, offset);
				}
			}
		}
	}
}
