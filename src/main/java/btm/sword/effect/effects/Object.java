package btm.sword.effect.effects;

import btm.sword.effect.Effect;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.VectorUtils;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Object extends Effect {
	final List<List<Vector>> objectParts;
	
	public Object(List<List<Vector>> objectParts, List<List<ParticleWrapper>> particles, double resolution, int partitions) {
		super(particles, resolution, partitions);
		this.objectParts = objectParts;
		
		points = new ArrayList<>(objectParts.size());
	}
	
	@Override
	public void displayAllParticles(List<List<Location>> points) {
		for (int i = 0; i < points.size(); i++) {
			int x = Math.min(i, particles.size()-1);
			
			for (Location l : points.get(i)) {
				for (ParticleWrapper p : particles.get(x))
					p.display(l);
			}
		}
	}
	
	@Override
	public void generatePoints(Location origin, Vector direction) {
		ArrayList<Vector> basis = VectorUtils.getBasis(origin, direction);
		
		for (List<Vector> part : objectParts) {
			List<Location> section = new ArrayList<>(part.size());
			for (Vector v : part) {
				section.add(origin.clone().add(VectorUtils.transformWithNewBasis(basis, v)));
			}
			points.add(section);
		}
	}
}
