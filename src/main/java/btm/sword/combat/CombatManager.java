package btm.sword.combat;

import btm.sword.Sword;
import btm.sword.effect.*;
import btm.sword.effect.objectshapes.BusterSwordShape;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class CombatManager {
//	public static void executeAttack(Player player, AttackType attack) {
//		double damage = attack.calculateDamage();
//		double range = attack.calculateRange(5.0);
//
//		Location origin = player.getEyeLocation().add(new Vector(0,-0.1,0));
//		Vector dir = origin.getDirection();
//
//		// these two lines should be wrapped into attack.execute, so that targets can be gathered and animations played
//		// sequentially for attacks that require that structure
//		HashSet<LivingEntity> targets = attack.getTargets(player, origin, dir, range);
//
//		Bukkit.getScheduler().runTaskAsynchronously(Sword.getInstance(), () -> attack.drawEffects(origin, dir, range, targets));
//
//		for (LivingEntity target : targets) {
//			target.damage(damage, player);
//		}
//
//		if (!targets.isEmpty()) {
//			StringBuilder attackReport = new StringBuilder("[Targets Hit:\n");
//			Iterator<LivingEntity> i = targets.iterator();
//			while (i.hasNext()) {
//				LivingEntity target = i.next();
//				attackReport.append("  ").append(target);
//				if (i.hasNext()) {
//					attackReport.append(",");
//				}
//				attackReport.append("\n");
//			}
//			attackReport.append("  ").append("For ").append(damage).append(" Damage]");
//			player.sendMessage(attackReport.toString());
//		}
//	}
	
	public static void executeAttack(Player player) {
		LineShape line = new LineShape(EffectExecutionType.ITERATIVE,
				List.of(
						List.of(
							new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.SILVER, 1f)),
							new ParticleWrapper(Particle.WHITE_ASH, 10, .2, .2, .2)
						)
				),
				4, 5, 25);
		List<List<Location>> points = line.generatePoints(player.getEyeLocation(), player.getEyeLocation().getDirection());
		
//		line.printPoints(points);
//		line.displayAllParticles(points);
		
		HashSet<LivingEntity> hit = new HashSet<>();
		for (List<Location> sections : points) {
			for (Location l : sections) {
				hit.addAll(l.getNearbyLivingEntities(0.1));
			}
		}
		hit.remove(player);
		for (LivingEntity target : hit) {
			target.damage(5, player);
		}
		
//		player.getWorld().spawnParticle(
//				Particle.SOUL_FIRE_FLAME,
//				player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3)),
//				10, 1, 1, 1, 5
//		);
//		player.getWorld().spawnParticle(
//				Particle.POOF,
//				player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)),
//				10, 1, 1, 1, 5
//		);
		
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
				EffectExecutionType.INSTANT,
				ObjectShapePrefab.BUSTER_SWORD.getParticles(),
				1,
				1);
		
		List<List<Location>> points =  busterSword.generatePoints(player.getEyeLocation().add(new Vector(0, -1, 0)), player.getEyeLocation().getDirection());
		busterSword.displayAllParticles(points);
		
		HashSet<LivingEntity> hit = new HashSet<>();
		for (List<Location> sections : points) {
			for (Location l : sections) {
				hit.addAll(l.getNearbyLivingEntities(0.1));
			}
		}
		hit.remove(player);
		for (LivingEntity target : hit) {
			target.damage(10, player);
		}
		
		player.isBlocking();
	}
	
	public static void arcTest(Player player) {
		ArcShape arc = new ArcShape(
				EffectExecutionType.ITERATIVE,
				List.of(
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.75f))
						)
				),
				20, 5, .05f, 5, 1,
				-45, 0,
				190);
		
		ArcShape arc2 = new ArcShape(
				EffectExecutionType.ITERATIVE,
				List.of(
						List.of(
								new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.RED, 4f)),
								new ParticleWrapper(Particle.FLAME, 4, .5, .5, .5, 0)
						)
				),
				20, 5, .05f, 5.25, 5.25,
				-45, 0,
				190);
		
		Location l = player.getEyeLocation();
		Vector e = l.getDirection();
		l.add(e.clone().multiply(2));
		
		List<List<Location>> points = arc.generatePoints(l, e);
		List<List<Location>> points2 = arc2.generatePoints(l, e);
		
		arc.display(points);
		arc2.display(points2);
	}
}
