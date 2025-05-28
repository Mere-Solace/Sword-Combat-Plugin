package btm.sword.effect;

import btm.sword.utils.ParticleData;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;

public abstract class EffectShape {
	protected EffectExecutionType executionType = EffectExecutionType.INSTANT;
	protected List<ParticleData> particles;
	protected int count;
	protected double offset;
	protected double speed;
	protected double resolution = 3;
	protected int partitions = 1;

	
	public EffectShape(List<ParticleData> particles, int count, double offset, double speed) {
		this.particles = particles;
		this.count = count;
		this. offset = offset;
		this.speed = speed;
	}
	
	public EffectShape(List<ParticleData> particles, int count, double offset, double speed, double resolution) {
		this.particles = particles;
		this.count = count;
		this. offset = offset;
		this.speed = speed;
		this.resolution = resolution;
	}
	
	public EffectShape(EffectExecutionType executionType, List<ParticleData> particles, int count, double offset, double speed, double resolution, int partitions) {
		this.executionType = executionType;
		this.particles = particles;
		this.count = count;
		this. offset = offset;
		this.speed = speed;
		this.resolution = resolution;
		this.partitions = partitions;
	}
	
	public abstract List<List<Location>> generatePoints(Location origin, Vector direction);
}
