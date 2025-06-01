package btm.sword.combat;

import btm.sword.Sword;
import btm.sword.effectshape.*;
import btm.sword.utils.ParticleWrapper;
import btm.sword.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CombatManager {
	public static void executeAttack(Player player) {
		LineShape line = new LineShape(
				List.of(
						List.of(
							new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 1f)),
							new ParticleWrapper(Particle.WHITE_ASH, 10, .2, .2, .2)
						)
				),
				4, 5, 25);
		line.generatePoints(player.getEyeLocation(), player.getEyeLocation().getDirection());
		
		line.display(EffectExecutionType.INSTANT);
		
		HashSet<LivingEntity> hit = new HashSet<>();
		for (List<Location> sections : line.getPoints()) {
			for (Location l : sections) {
				hit.addAll(l.getNearbyLivingEntities(0.1));
			}
		}
		hit.remove(player);
		for (LivingEntity target : hit) {
			target.damage(5, player);
		}
		
		player.getWorld().spawnParticle(
				Particle.DUST_COLOR_TRANSITION,
				player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3)),
				10, 1, 1, 1, 1,
				new Particle.DustTransition(Color.fromRGB(228, 222, 75), Color.fromRGB(245, 171, 60), 2f)
		);
	}
	
	public static void test(Player player) {
		ObjectShape busterSword = new ObjectShape(
				ObjectShapePrefab.BUSTER_SWORD.getPoints(),
				ObjectShapePrefab.BUSTER_SWORD.getParticles(),
				1,
				1);
		
		busterSword.generatePoints(player.getEyeLocation().add(new Vector(0, -1, 0)), player.getEyeLocation().getDirection());
		busterSword.display(EffectExecutionType.INSTANT);
		
		HashSet<LivingEntity> hit = new HashSet<>();
		for (List<Location> sections : busterSword.getPoints()) {
			for (Location l : sections) {
				hit.addAll(l.getNearbyLivingEntities(0.1));
			}
		}
		hit.remove(player);
		for (LivingEntity target : hit) {
			target.damage(10, player);
			BoundingBox boundingBox = target.getBoundingBox();
		}
		
		player.isBlocking();
	}
	
	public static void arcTest(Player player) {
		ArcShape arc = new ArcShape(
				List.of(
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.75f))
						),
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.BLACK, 0.75f))
						),
						List.of(
								new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 2, 0, 0, 0, 0)
						),
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.75f))
						)
				),
				20, 5, 1, 5, 2.5,
				-45, 0,
				100);
		
		ArcShape arc2 = new ArcShape(
				List.of(
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.RED, 4f))
						)
				),
				20, 5, 1, 5.25, 5.25,
				-45, 0,
				105);
		
		Location l = player.getEyeLocation().add(0, -.5, 0);
		Vector e = l.getDirection();
		l.add(e.clone().multiply(2));
		
		arc.generatePoints(l, e);
		arc2.generatePoints(l, e);
		
		arc.display(EffectExecutionType.ITERATIVE);
		arc2.display(EffectExecutionType.ITERATIVE);
		
//		List<LivingEntity> hit = new ArrayList<>(l.add(e.multiply(2.75)).getNearbyLivingEntities(2.75));
		
//		List<LivingEntity> hit = new ArrayList<>(l.add(e.multiply(2)).getNearbyLivingEntities(3.5));
		
		
		// hitbox class
		List<LivingEntity> hit = new ArrayList<>(l.getNearbyLivingEntities(5.25));
		hit.remove(player);
		List<Vector> basis = VectorUtils.getBasis(l, e);
		VectorUtils.rotateBasis(basis, -45, 0);
		
		ParticleWrapper p = new ParticleWrapper(Particle.FLASH, 2, 0.1, 0.1, 0.1, 0.5);
		ParticleWrapper w = new ParticleWrapper(Particle.CRIT, 10, 0, 0, 0);
		
		boolean tipper = false;
		
		for (LivingEntity target : hit) {
			Vector toTarget = target.getEyeLocation().subtract(l).toVector();
			
			double forwardDist = toTarget.dot(basis.getLast());
			double sideOffset = toTarget.dot(basis.getFirst());
			double upOffset = toTarget.dot(basis.get(1));
			
			// Define thresholds (tweak as needed)
			double tipMin = 3;      // Start of tip zone
			double tipMax = 5.5;      // End of tip zone
			double sideMax = 5.5;     // Width of tip
			double upMax = 1;       // Vertical thickness
			
			if (forwardDist >= tipMin && forwardDist <= tipMax &&
					Math.abs(sideOffset) <= sideMax &&
					Math.abs(upOffset) <= upMax) {
				tipper = true;
				// This target is in the tip region!
				target.damage(12, player);
				p.display(target.getEyeLocation());
				player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 5f, 1.44f);
				player.sendMessage("Tipper!");
			}
		}
		
		// should go in apply self effects of an attack... Maybe attack should just be an interface
		if (tipper) {
			for (int i = 0; i < 3; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						player.setVelocity(player.getVelocity().add(new Vector(0, .25, 0)));
					}
				}.runTaskLater(Sword.getInstance(), i);
			}
		}
	}
}
