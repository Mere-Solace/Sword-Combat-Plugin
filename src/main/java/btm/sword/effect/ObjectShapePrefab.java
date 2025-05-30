package btm.sword.effect;

import btm.sword.effect.objectshapes.BusterSwordShape;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

public enum ObjectShapePrefab {
	BUSTER_SWORD(BusterSwordShape.generate(),
			List.of(
					List.of(
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 1.5f))
					),
					List.of(
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.GRAY, 2f))
//						new ParticleWrapper(Particle.FLAME, 1, .1, .1, .1, 0.001)
					),
					List.of(
						new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, .1, .1, .1, 2),
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 1.5f)),
						new ParticleWrapper(Particle.DUST, new Particle.DustTransition(Color.BLUE, Color.BLACK, 2f))
			
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

