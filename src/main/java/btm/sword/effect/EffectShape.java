package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public abstract class EffectShape {
	protected EffectUsageType usageType = EffectUsageType.BOTH;
	protected EffectExecutionType executionType = EffectExecutionType.INSTANT;
	protected List<List<ParticleWrapper>> particles;
	protected double resolution = 3;
	protected int partitions = 1;
	protected double duration = -1;
	protected float period = 0.5f;
	
	public EffectShape(List<List<ParticleWrapper>> particles) {
		this.particles = particles;
	}
	
	public EffectShape(List<List<ParticleWrapper>> particles, double resolution) {
		this.particles = particles;
		this.resolution = resolution;
	}
	
	public EffectShape(EffectExecutionType executionType, List<List<ParticleWrapper>> particles, double resolution, int partitions) {
		this.executionType = executionType;
		this.particles = particles;
		this.resolution = resolution;
		this.partitions = partitions;
		
		if (executionType.equals(EffectExecutionType.INSTANT)) {
			period = 0;
		}
	}
	
	public EffectShape(EffectExecutionType executionType, List<List<ParticleWrapper>> particles, double resolution, int partitions, float period) {
		this.executionType = executionType;
		this.particles = particles;
		this.resolution = resolution;
		this.partitions = partitions;
		this.period = period;
	}
	
	public abstract List<List<Location>> generatePoints(Location origin, Vector direction);
	
	public void display(List<List<Location>> points) {
		if (executionType.equals(EffectExecutionType.INSTANT)) {
			displayAllParticles(points);
		}
		else if (executionType.equals(EffectExecutionType.ITERATIVE)) {
			displaySectionOfParticles(points.getFirst());
			for (int i = 1; i < points.size(); i++) {
				final int finalI = i;
				long delay = Math.max(1, (long) (20*period*i));
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
	
	public void printPoints(List<List<Location>> points) {
		Sword.getInstance().getLogger().info("~~~~ Effect info:" + "Partitions: " + partitions);
		Sword.getInstance().getLogger().info("~~~ Effect Locations:");
		for (List<Location> section : points) {
			Sword.getInstance().getLogger().info("~~ New Section:");
			for (int i = 0; i < section.size(); i++) {
				Sword.getInstance().getLogger().info("Point " + i +  " " + section.get(i).toString());
			}
		}
	}
}
