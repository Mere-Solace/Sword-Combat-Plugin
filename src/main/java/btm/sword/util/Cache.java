package btm.sword.util;

import org.bukkit.Color;
import org.bukkit.Material;
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
	
	public static final List<Vector> aerialForwardSword;
	public static final List<Vector> aerialSwordDown;
	public static final List<Vector> aerialSwordUp;
	
	public static final ParticleWrapper testFlameParticle;
	public static final ParticleWrapper testSoulFlameParticle;
	public static final ParticleWrapper testObsidianTearParticle;
	public static final ParticleWrapper testLavaDripParticle;
	public static final ParticleWrapper testSwingParticle;
	public static final ParticleWrapper testHitParticle;
	public static final ParticleWrapper testBleedParticle;
	
	public static final ParticleWrapper thrownItemStickParticle;
	
	public static final ParticleWrapper thrownItemMarkerParticle;
	public static final ParticleWrapper thrownItemMarkerParticle2;
	
	public static final ParticleWrapper basicSwordBlueTransitionParticle;
	public static final ParticleWrapper basicSwordWhiteTransitionParticle;
	public static final ParticleWrapper basicSwordHit1;
	public static final ParticleWrapper basicSwordHit2;
	public static final ParticleWrapper basicSwordEnterGround;
	
	public static final ParticleWrapper grabCloudParticle;
	public static final ParticleWrapper throwTrailParticle;
	public static final ParticleWrapper throwTrailParticle2;
	public static final ParticleWrapper grabHitParticle;
	public static final ParticleWrapper grabHitParticle2;
	
	public static final ParticleWrapper toughnessBreakParticle1;
	public static final ParticleWrapper toughnessRechargeParticle;
	public static final ParticleWrapper toughnessRechargeParticle2;
	
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
				new Vector(0.14, 1.8, 3.4)));
		dragonKillerArcRatios = new ArrayList<>(List.of(
				0.86, 2.13, 2.3, 0.87));
		
		aerialForwardSword = new ArrayList<>(List.of(
				new Vector(1.0961, 3.324, -1.13),
				new Vector(0, -1.083, -0.791),
				new Vector(-0.2825, 0.951, 10.509),
				new Vector(-0.7458, -5.151, -1.808)));
		
		aerialSwordDown = new ArrayList<>(List.of(
				new Vector(-0.35, 2.53, 0.56),
				new Vector(0, -3.42, -0.581),
				new Vector(0.329, -0.165, 4.97),
				new Vector(-0.07, -6.15, 0.98)));
		
		aerialSwordUp = new ArrayList<>(List.of(
				new Vector(-1.19, -0.389, -0.63),
				new Vector(0.84, -0.053, 3.64),
				new Vector(-1.449, 0.71, 3.43),
				new Vector(-0.91, -0.13, 4.83)));
		
		
		testFlameParticle = new ParticleWrapper(Particle.FLAME, 2, 0.025, 0.025, 0.025, 0);
		testSoulFlameParticle = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, 0.025, 0.025, 0.025, 0);
		testObsidianTearParticle = new ParticleWrapper(Particle.DRIPPING_OBSIDIAN_TEAR, 1, 0, 0, 0, 0);
		testLavaDripParticle = new ParticleWrapper(Particle.DRIPPING_LAVA, 2, 0, 0, 0, 0);
		testSwingParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 2, 0, 0, 0, 1,
				new Particle.DustTransition(Color.fromRGB(211, 222, 240), Color.fromRGB(36, 103, 220), 0.7f));
		testHitParticle = new ParticleWrapper(Particle.SONIC_BOOM, 3, 0, 0, 0, 0.5);
		testBleedParticle = new ParticleWrapper(Particle.BLOCK, 5, 0, 0, 0, Material.CRIMSON_HYPHAE.createBlockData());
		
		thrownItemStickParticle = new ParticleWrapper(Particle.SOUL, 2, 0, 0, 0, 0);
		
		thrownItemMarkerParticle = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION, 3, 0.01, 0.01, 0.01, 0);
		thrownItemMarkerParticle2 = new ParticleWrapper(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, 4, 0.01, 0.01, 0.01, 0);
		
		basicSwordBlueTransitionParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
				new Particle.DustTransition(Color.fromRGB(14, 107, 207), Color.fromRGB(162, 226, 255), 0.75f));
		basicSwordWhiteTransitionParticle = new ParticleWrapper(Particle.DUST_COLOR_TRANSITION, 5, 0.025, 0.025, 0.025, 1,
				new Particle.DustTransition(Color.fromRGB(235, 243, 255), Color.fromRGB(120, 121, 255), 0.65f));
		
		basicSwordHit1 = new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 30, 0.5, 0.5, 0.5, 0.1);
		basicSwordHit2 = new ParticleWrapper(Particle.ENCHANTED_HIT, 30, 0.5, 0.5, 0.5, 0.3);
		
		basicSwordEnterGround = new ParticleWrapper(Particle.CRIT, 10, 0.1, 0.1, 0.1, 0.5);
		
		grabCloudParticle = new ParticleWrapper(Particle.POOF, 20, 0.5, 0.5, 0.5, 0.1);
		throwTrailParticle = new ParticleWrapper(Particle.DUST, 4, 0.2, 0.2, 0.2,
				new Particle.DustOptions(Color.WHITE, 2.5f));
		throwTrailParticle2 = new ParticleWrapper(Particle.DUST_PLUME, 5, 0.1, 0.1, 0.1, 0);
		grabHitParticle = new ParticleWrapper(Particle.FLAME, 50, 0.6, 0.6, 0.6, 0.02);
		grabHitParticle2 = new ParticleWrapper(Particle.DUST, 3, 0.01, 0.01, 0.01,
				new Particle.DustOptions(Color.ORANGE, 3f));
		
		toughnessBreakParticle1 = new ParticleWrapper(Particle.GUST, 2, 0.1, 0.1, 0.1, 1);
		
		toughnessRechargeParticle = new ParticleWrapper(Particle.LAVA, 10, 0.1, 0.1, 0.1, 0.25);
		toughnessRechargeParticle2 = new ParticleWrapper(Particle.FLAME, 100, 0.5, 0.5, 0.5, 0.5);
	}
}
