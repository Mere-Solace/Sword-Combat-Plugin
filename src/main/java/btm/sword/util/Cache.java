package btm.sword.util;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Cache {
	public static final List<Vector> basicSword1;
	public static final List<Vector> basicSword2;
	public static final List<Vector> basicSword3;
	
	public static final List<Vector> dragonKillerArc;
	public static final List<Double> dragonKillerArcRatios;
	
	public static final ParticleWrapper testFlameParticle;
	public static final ParticleWrapper testSoulFlameParticle;
	public static final ParticleWrapper testObsidianTearParticle;
	public static final ParticleWrapper testLavaDripParticle;
	
	public static final ParticleWrapper basicSwordBlueTransitionParticle;
	
	public static final ParticleWrapper grabCloudParticle;
	
	static {
		basicSword1 = new ArrayList<>(List.of(
				new Vector(-2.06, -1.2, -0.5),
				new Vector(3.26, 0.85, -0.4),
				new Vector(-2.3, -0.1,3),
				new Vector(1.9, 0.27, 5)));
		basicSword2 = new ArrayList<>(List.of(
				new Vector(2.6, -1.15, -1.2),
				new Vector(-1.47, 2.05, 0),
				new Vector(1.6, -0.05, 7),
				new Vector(-3.66, 1.85, 0.32)));
		basicSword3 = new ArrayList<>(List.of(
				new Vector(-0.15, 2.8, -1.5),
				new Vector(-1.1, -2.2, 1.1),
				new Vector(1.57, 1.96, 3),
				new Vector(0, 0.27, 5)));
//		forwardSwordSlash1 = new ArrayList<>(List.of(
//				new Vector(-5.487, 0.209, -0.177),
//				new Vector(3.4515, -4.1275, -8.2482),
//				new Vector(-4.5312, 0.6692,12.39),
//				new Vector(8.7792, -1.2778, 4.956)));
		
		dragonKillerArc = new ArrayList<>(List.of(
				new Vector(0.26, 2.23, -2.5),
				new Vector(0, -1.53, 2.66),
				new Vector(-0.63, 4.04, 0.74),
				new Vector(0.14, 1.8, 3.4)
		));
		dragonKillerArcRatios = new ArrayList<>(List.of(
				0.86, 2.13, 2.3, 0.87
		));
		
		testFlameParticle = new ParticleWrapper(Particle.FLAME, 2, 0.025, 0.025, 0.025, 0);
		testSoulFlameParticle = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 2, 0.025, 0.025, 0.025, 0);
		testObsidianTearParticle = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 2, 0.025, 0.025, 0.025, 0);
		testLavaDripParticle = new ParticleWrapper(Particle.FALLING_LAVA, 2, 0.025, 0.025, 0.025, 0);
		
		basicSwordBlueTransitionParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
				new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.85f));
		
		grabCloudParticle = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
	}
}
