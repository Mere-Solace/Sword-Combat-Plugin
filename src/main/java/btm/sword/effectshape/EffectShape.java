package btm.sword.effectshape;

import btm.sword.Sword;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public abstract class EffectShape {
	protected List<List<ParticleWrapper>> particles;
	protected double resolution = 3;
	protected int partitions = 1;
	protected int delayTicks = 5;
	
	public List<List<Location>> points;
	
	public EffectShape(List<List<ParticleWrapper>> particles) {
		this.particles = particles;
	}
	
	public EffectShape(List<List<ParticleWrapper>> particles, double resolution) {
		this.particles = particles;
		this.resolution = resolution;
	}
	
	public EffectShape(List<List<ParticleWrapper>> particles, double resolution, int partitions) {
		this.particles = particles;
		this.resolution = resolution;
		this.partitions = partitions;
	}
	
	public EffectShape(List<List<ParticleWrapper>> particles, double resolution, int partitions, int delayTicks) {
		this.particles = particles;
		this.resolution = resolution;
		this.partitions = partitions;
		this.delayTicks = delayTicks;
	}
	
	public abstract void generatePoints(Location origin, Vector direction);
	
	public void display(EffectExecutionType executionType) {
		if (executionType.equals(EffectExecutionType.INSTANT)) {
			displayAllParticles(points);
		}
		else if (executionType.equals(EffectExecutionType.ITERATIVE)) {
			for (int i = 0; i < points.size(); i++) {
				final int finalI = i;
				long delay = Math.max(1, delayTicks*i);
				new BukkitRunnable() {
					@Override
					public void run() {
						displaySectionOfParticles(points.get(finalI));
					}
				}.runTaskLater(Sword.getInstance(), delay);
			}
		}
	}
	
	public void displaySectionOfParticles(List<Location> points) {
		for (int i = 0; i < points.size(); i++) {
			if (i > particles.size()-1)
				for (ParticleWrapper p : particles.getLast())
					p.display(points.get(i));
			else
				for (ParticleWrapper p : particles.get(i))
					p.display(points.get(i));
		}
	}
	
	public void displayAllParticles(List<List<Location>> points) {
		for (List<Location> section : points) {
			for (int i = 0; i < section.size(); i++) {
				if (i > particles.size()-1)
					for (ParticleWrapper p : particles.getLast())
						p.display(section.get(i));
				else
					for (ParticleWrapper p : particles.get(i))
						p.display(section.get(i));
			}
		}
	}
	
	public List<List<Location>> getPoints() {
		return points;
	}
}
