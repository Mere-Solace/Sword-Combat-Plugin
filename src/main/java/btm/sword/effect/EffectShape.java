package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;

public abstract class EffectShape {
	protected EffectExecutionType executionType = EffectExecutionType.INSTANT;
	protected List<ParticleWrapper> particles;
	protected double resolution = 3;
	protected int partitions = 1;
	
	public EffectShape(List<ParticleWrapper> particles) {
		this.particles = particles;
	}
	
	public EffectShape(List<ParticleWrapper> particles, double resolution) {
		this.particles = particles;
		this.resolution = resolution;
	}
	
	public EffectShape(EffectExecutionType executionType, List<ParticleWrapper> particles, double resolution, int partitions) {
		this.executionType = executionType;
		this.particles = particles;
		this.resolution = resolution;
		this.partitions = partitions;
	}
	
	public abstract List<List<Location>> generatePoints(Location origin, Vector direction);
	
	protected void printPoints(List<List<Location>> points) {
		Sword.getInstance().getLogger().info("\nEffect Locations:");
		for (List<Location> section : points) {
			Sword.getInstance().getLogger().info("New Section:");
			for (int i = 0; i < section.size(); i++) {
				Sword.getInstance().getLogger().info("Point " + i +  " " + section.get(i).toString());
			}
		}
	}
}
