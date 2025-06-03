package btm.sword.effect.objectshapes;

import btm.sword.util.ParticleWrapper;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

public enum ObjectShapePrefab {
	BUSTER_SWORD(BigSwordShapeGenerator.generate(),
			List.of(
					List.of(
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 1.5f))
					),
					List.of(
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.GRAY, 2f))
//						new ParticleWrapper(Particle.FLAME, 1, .1, .1, .1, 0.001)
					),
					List.of(
						new ParticleWrapper(Particle.CRIT, 1, .1, .1, .1, 3),
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 0.75f)),
						new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 4, 0.1, 0.1, 0.1, 1,
								new Particle.DustTransition(Color.fromRGB(246, 255, 102), Color.fromRGB(222, 169, 22), 2f))
					)
			)
	);
	
	private final List<List<Vector>> points;
	private final List<List<ParticleWrapper>> particles;
	
	ObjectShapePrefab(List<List<Vector>> points, List<List<ParticleWrapper>> particles) {
		this.points = points;
		this.particles = particles;
	}
	
	public List<List<Vector>> getPoints() {
		return points;
	}
	
	public List<List<ParticleWrapper>> getParticles() {
		return particles;
	}
}

