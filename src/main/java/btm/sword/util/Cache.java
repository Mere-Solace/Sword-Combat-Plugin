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
	
	public static final List<Vector> sideStepSwordR;
	public static final List<Vector> sideStepSwordL;
	
	public static final List<Vector> dragonKillerArc;
	public static final List<Double> dragonKillerArcRatios;
	
	public static final ParticleWrapper testFlameParticle;
	public static final ParticleWrapper testSoulFlameParticle;
	public static final ParticleWrapper testObsidianTearParticle;
	public static final ParticleWrapper testLavaDripParticle;
	
	public static final ParticleWrapper basicSwordBlueTransitionParticle;
	public static final ParticleWrapper basicSwordWhiteTransitionParticle;
	public static final ParticleWrapper basicSwordHit1;
	public static final ParticleWrapper basicSwordHit2;
	public static final ParticleWrapper basicSwordEnterGround;
	
	public static final ParticleWrapper grabCloudParticle;
	
	static {
		basicSword1 = new ArrayList<>(List.of(
				new Vector(-2.06, -1.26, -0.5),
				new Vector(3.26, 0.79, -0.4),
				new Vector(-2.3, -0.16,3),
				new Vector(1.9, 0.21, 5)));
		basicSword2 = new ArrayList<>(List.of(
				new Vector(2.6, -1.21, -1.2),
				new Vector(-1.47, 1.99, 0),
				new Vector(1.6, -0.11, 7),
				new Vector(-3.66, 0.26, 1.85)));
		basicSword3 = new ArrayList<>(List.of(
				new Vector(-0.15,2.8,-1.5),
				new Vector(-1.1,-2.2,-0.9),
				new Vector(1.74,1.96,4.3),
				new Vector(-1.1,-1.77,5)));

		sideStepSwordR = new ArrayList<>(List.of(
				new Vector(-1.3,1.03,2),
				new Vector(8.2,1.03,-1.9),
				new Vector(-7,-1.73,3.3),
				new Vector(9,-0.93,5)));
		sideStepSwordL = new ArrayList<>(List.of(
				new Vector(1.3,1.03,2),
				new Vector(-8.2,1.03,-1.9),
				new Vector(7,-1.73,3.3),
				new Vector(-9,-0.93,5)));
		
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
		testSoulFlameParticle = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, 0.025, 0.025, 0.025, 0);
		testObsidianTearParticle = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 2, 0.025, 0.025, 0.025, 0);
		testLavaDripParticle = new ParticleWrapper(Particle.FALLING_LAVA, 2, 0.025, 0.025, 0.025, 0);
		
		basicSwordBlueTransitionParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
				new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.75f));
		basicSwordWhiteTransitionParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
				new Particle.DustTransition(Color.fromRGB(235, 243, 255), Color.fromRGB(120, 121, 255), 0.65f));
		basicSwordHit1 = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 30, 0.5, 0.5, 0.5, 0.1);
		basicSwordHit2 = new ParticleWrapper(Particle.ENCHANTED_HIT, 20, 0.5, 0.5, 0.5, 0.3);
		basicSwordEnterGround = new ParticleWrapper(Particle.CRIT, 30, 0.1, 0.1, 0.1, 0.5);
		
		
		grabCloudParticle = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
		
		
	}
}
