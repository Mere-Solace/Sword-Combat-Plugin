package btm.sword.effect.effects;

import btm.sword.effect.Effect;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Line extends Effect {
	double length;
	
	public Line(List<List<ParticleWrapper>> particles, double resolution, int partitions, double length) {
		super(particles, resolution, partitions);
		this.length = length;
		
		points = new ArrayList<>((int) (length*resolution));
	}
	
	public Line(List<List<ParticleWrapper>> particles, double resolution, double length) {
		super(particles, resolution);
		this.length = length;
		
		points = new ArrayList<>((int) (length*resolution));
	}
	
	@Override
	public void onRun() {
		int pointsPerPartition = (int) ((length*resolution)/partitions);
		
		Vector step = direction.clone().normalize().multiply(1/resolution);
		
		for (int x = 0; x < pointsPerPartition; x++) {
			points.add(location.add(step));
		}
	}
}
