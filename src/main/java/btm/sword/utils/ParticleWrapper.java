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
	private double speed = 0;
	
	public ParticleWrapper(Particle particle) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
	}
	
	public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double speed) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
		this.count = count;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOffset;
		this.speed = speed;
	}
	
	public ParticleWrapper(Particle particle, Particle.DustOptions options) {
		this.particle = particle;
		this.options = options;
		this.transition = null;
	}
	
	public ParticleWrapper(Particle particle, Particle.DustTransition transition) {
		this.particle = particle;
		this.options = null;
		this.transition = transition;
	}
	
	public Particle getParticle() {
		return particle;
	}
	
	public Particle.DustOptions getOptions() {
		return options;
	}
	
	public Particle.DustTransition getTransition() {
		return transition;
	}
	
	public void display(Location origin) {
		World world = origin.getWorld();
		if (particle.getDataType() != Particle.DustOptions.class) {
			world.spawnParticle(particle, origin, count, xOffset, yOffset, zOffset, speed);
		}
		else if (options == null) {
			world.spawnParticle(particle, origin, count, transition);
		}
		else {
			world.spawnParticle(particle, origin, count, options);
		}
	}
}
