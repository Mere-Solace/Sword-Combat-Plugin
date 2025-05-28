package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.utils.ParticleData;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LineShape extends EffectShape {
	double length;
	
	public LineShape(EffectExecutionType executionType, List<ParticleData> particles, int count, double offset, double speed, double resolution, int partitions, double length) {
		super(executionType, particles, count, offset, speed, resolution, partitions);
		this.length = length;
	}
	
	public LineShape(List<ParticleData> particles, int count, double offset, double speed, double resolution, double length) {
		super(particles, count, offset, speed, resolution);
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
		
		for (List<Location> section : points) {
			Sword.getInstance().getLogger().info("New Section:\n");
			for (Location point : section) {
				Sword.getInstance().getLogger().info(point.toString() + "\n");
			}
		}
		
		return points;
	}
}
