package btm.sword.effect;

import btm.sword.utils.ParticleWrapper;
import btm.sword.utils.Utils;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ObjectShape extends EffectShape {
	final List<List<Vector>> objectParts;
	
	public ObjectShape(List<List<Vector>> objectParts, EffectExecutionType executionType, List<List<ParticleWrapper>> particles, double resolution, int partitions) {
		super(executionType, particles, resolution, partitions);
		this.objectParts = objectParts;
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
	public List<List<Location>> generatePoints(Location origin, Vector direction) {
		ArrayList<Vector> basis = Utils.getBasis(origin, direction);
		
		List<List<Location>> points = new ArrayList<>();
		for (List<Vector> part : objectParts) {
			List<Location> section = new ArrayList<>(part.size());
			for (Vector v : part) {
				section.add(origin.clone().add(Utils.transformWithNewBasis(basis, v)));
			}
			points.add(section);
		}
		return points;
	}
}
