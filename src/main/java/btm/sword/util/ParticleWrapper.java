package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public class ParticleWrapper {
	private final Particle particle;
	private int count = 1;
	private double xOffset = 0;
	private double yOffset = 0;
	private double zOffset = 0;
	private double speed = -1;
	private double data = 1;
	private Particle.DustOptions options = null;
	private Particle.DustTransition transition = null;
	private BlockData blockData = null;
	
	public ParticleWrapper(Particle particle) {
		this.particle = particle;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset) {
		this(particle);
		this.count = count;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOffset;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double speed) {
		this(particle, count, xOffset, yOffset, zOffset);
		this.speed = speed;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, Particle.DustOptions options) {
		this(particle, count, xOffset, yOffset, zOffset);
		this.options = options;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double data, Particle.DustTransition transition) {
		this(particle, count, xOffset, yOffset, zOffset);
		this.data = data;
		this.transition = transition;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, BlockData blockData) {
		this(particle, count, xOffset, yOffset, zOffset);
		this.blockData = blockData;
	}
	
	public void display(Location location) {
		World world = location.getWorld();
		if (transition == null && options == null && blockData == null)
			if (speed == -1)
				world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset);
			else
				world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, speed);
			
		else if (options == null && blockData == null)
			world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, data, transition);
		
		else if (blockData == null)
			world.spawnParticle(particle, location, count, options);
		
		else
			world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, blockData);
	}
}
