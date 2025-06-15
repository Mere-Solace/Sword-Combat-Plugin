package btm.sword.util;

import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Cache {
	public static final List<Vector> forwardSwordSlash1;
	
	public static final List<Vector> dragonKillerArc;
	public static final List<Double> dragonKillerArcRatios;
	
	public static final ParticleWrapper testFlameParticles;
	public static final ParticleWrapper testSoulFlameParticles;
	
	static {
		forwardSwordSlash1 = new ArrayList<>(List.of(
				new Vector(-5.487, 0.209, -0.177),
				new Vector(3.4515, -4.1275, -8.2482),
				new Vector(-4.5312, 0.6692,12.39),
				new Vector(8.7792, -1.2778, 4.956)));
		
		dragonKillerArc = new ArrayList<>(List.of(
				new Vector(0.26, 2.23, -2.5),
				new Vector(0, -1.53, 2.66),
				new Vector(-0.63, 4.04, 0.74),
				new Vector(0.14, 1.8, 3.4)
		));
		dragonKillerArcRatios = new ArrayList<>(List.of(
				0.86, 2.13, 2.3, 0.87
		));
		
		testFlameParticles = new ParticleWrapper(Particle.FLAME, 5, 0.1, 0.1, 0.1, 0);
		testSoulFlameParticles = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 5, 0.1, 0.1, 0.1, 0);
	}
}
