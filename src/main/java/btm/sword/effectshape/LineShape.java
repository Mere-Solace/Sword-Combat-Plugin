package btm.sword.effectshape;

import btm.sword.utils.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LineShape extends EffectShape {
	double length;
	
	public LineShape(List<List<ParticleWrapper>> particles, double resolution, int partitions, double length) {
		super(particles, resolution, partitions);
		this.length = length;
		
		points = new ArrayList<>((int) (length*resolution));
	}
	
	public LineShape(List<List<ParticleWrapper>> particles, double resolution, double length) {
		super(particles, resolution);
		this.length = length;
		
		points = new ArrayList<>((int) (length*resolution));
	}
	
	@Override
	public void generatePoints(Location origin, Vector direction) {
		int pointsPerPartition = (int) ((length*resolution)/partitions);
		
		Vector step = direction.clone().normalize().multiply(1/resolution);
		Location cur = origin.clone();
		
		for (int i = 0; i < partitions; i++) {
			List<Location> section = new ArrayList<>(partitions);
			for (int x = 0; x < pointsPerPartition; x++) {
				section.add(cur.clone());
				cur = cur.add(step);
			}
			points.add(section);
		}
	}
}
