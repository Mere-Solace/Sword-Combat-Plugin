package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class LineVisual extends VisualEffect {
	double spacing;
	
	public LineVisual(List<ParticleData> particles, int count, double offset, double spacing) {
		super(particles, count, offset);
		this.spacing = spacing;
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		World world = origin.getWorld();
		Vector step = direction.clone().normalize().multiply(spacing);
		
		Location cur = origin.clone();
		
		for (double i = 0; i < range; i += spacing) {
			for (ParticleData p : particles) {
				if (p.getOptions() != null) {
					world.spawnParticle(p.getParticle(), cur, count, offset, offset, offset, 0, p.getOptions());
				}
				else {
					world.spawnParticle(p.getParticle(), cur, count, offset, offset, offset);
				}
			}
			
			cur = cur.add(step);
		}
	}
}
