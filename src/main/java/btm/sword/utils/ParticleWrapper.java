package btm.sword.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleWrapper {
	private final Particle particle;
	private final Particle.DustOptions options;
	private final Particle.DustTransition transition;
	private int count = 1;
	private double xOffset = 0;
	private double yOffset = 0;
	private double zOffset = 0;
	private double data = -1;
	
	public ParticleWrapper(Particle particle) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
		this.count = count;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOffset;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double speed) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
		this.count = count;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOffset;
		this.data = speed;
	}
	
	public ParticleWrapper(Particle particle, Particle.DustOptions options) {
		this.particle = particle;
		this.options = options;
		this.transition = null;
	}
	
	public ParticleWrapper(Particle particle, int count, double offset, double data, Particle.DustTransition transition) {
		this.particle = particle;
		this.options = null;
		this.transition = transition;
		this.count = count;
		this.xOffset = offset;
		this.yOffset = offset;
		this.zOffset = offset;
		this.data = data;
	}
	
	public void display(Location location) {
		World world = location.getWorld();
		if (transition == null && options == null)
			if (data == -1)
				world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset);
			else
				world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, data);
		else if (options == null)
			world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, data, transition);
		else
			world.spawnParticle(particle, location, count, options);
	}
}
