package btm.sword.utils;

import org.bukkit.Particle;

public class ParticleData {
	private final Particle particle;
	private final Particle.DustOptions options;
	private final Particle.DustTransition transition;
	
	public ParticleData(Particle particle) {
		this.particle = particle;
		this.options = null;
		this.transition = null;
	}
	
	public ParticleData(Particle particle, Particle.DustOptions options) {
		this.particle = particle;
		this.options = options;
		this.transition = null;
	}
	
	public ParticleData(Particle particle, Particle.DustOptions options, Particle.DustTransition transition) {
		this.particle = particle;
		this.options = options;
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
}
