package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LineShape extends EffectShape {
	double length;
	
	public LineShape(EffectExecutionType executionType, List<ParticleWrapper> particles, double resolution, int partitions, double length) {
		super(executionType, particles, resolution, partitions);
		this.length = length;
	}
	
	public LineShape(List<ParticleWrapper> particles, double resolution, double length) {
		super(particles, resolution);
		this.length = length;
	}
	
	@Override
	public List<List<Location>> generatePoints(Location origin, Vector direction) {
		List<List<Location>> points = new ArrayList<>((int) (length*resolution));
		
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
	
		Sword.getInstance().getLogger().info(points.toString());
		return points;
	}
}
