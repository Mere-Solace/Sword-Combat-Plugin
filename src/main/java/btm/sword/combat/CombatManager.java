package btm.sword.combat;

import btm.sword.Sword;
import btm.sword.effect.EffectExecutionType;
import btm.sword.effect.LineShape;
import btm.sword.effect.ObjectShape;
import btm.sword.effect.ObjectShapePrefab;
import btm.sword.effect.objectshapes.BusterSwordShape;
import btm.sword.utils.ParticleWrapper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

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
		player.sendMessage("generating points:\n");
		LineShape line = new LineShape(EffectExecutionType.ITERATIVE,
				List.of(
						List.of(
							new ParticleWrapper(Particle.LANDING_OBSIDIAN_TEAR, 4, .25, .25, .25)
						),
						List.of(
							new ParticleWrapper(Particle.DUST, new Particle.DustOptions(Color.BLACK, 1.25f))
						)
				),
				4, 5, 10);
		List<List<Location>> points = line.generatePoints(player.getEyeLocation(), player.getEyeLocation().getDirection());
		
		Sword.getInstance().getLogger().info("Printing Points: ");
		line.printPoints(points);
		line.displayAllParticles(points);
		
		player.getWorld().spawnParticle(
				Particle.SOUL_FIRE_FLAME,
				player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3)),
				10, 1, 1, 1, 0
		);
	}
	
	public static void test(Player player) {
		ObjectShape busterSword = new ObjectShape(
				ObjectShapePrefab.BUSTER_SWORD.getPoints(),
				EffectExecutionType.INSTANT,
				ObjectShapePrefab.BUSTER_SWORD.getParticles(),
				1,
				1);
		
		List<List<Location>> points =  busterSword.generatePoints(player.getEyeLocation(), player.getEyeLocation().getDirection());
		busterSword.displayAllParticles(points);
	}
}
