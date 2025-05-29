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
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.BLACK, 5f)),
						new ParticleWrapper(Particle.SMALL_FLAME, 3, .2, .2, .2, 0)
					),
					List.of(
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.GRAY, 5f)),
						new ParticleWrapper(Particle.FLAME, 5, .1, .1, .1, 0)
					),
					List.of(
						new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 2, .1, .1, .1, 0),
						new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 2.5f))
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

