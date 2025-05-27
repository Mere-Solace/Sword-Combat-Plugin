package btm.sword.visualeffect;

import btm.sword.shape.Shape;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.List;

public class VisualEffect {
	Shape shape;
	List<ParticleData> particles;
	int count;
	double offset;
	
	public VisualEffect(Shape shape, List<ParticleData> particles, int count, double offset) {
		this.shape = shape;
		this.particles = particles;
		this.count = count;
		this. offset = offset;
	}
	
	public void drawEffect(Vector direction, Location origin) {
		World world = origin.getWorld();
		for (Location l : shape.generatePoints(direction, origin)) {
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
}
