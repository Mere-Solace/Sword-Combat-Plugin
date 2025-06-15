package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.BezierUtil;
import btm.sword.util.Cache;
import btm.sword.util.HitboxUtil;
import btm.sword.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AttackAction {
	
	public static Runnable basic(SwordEntity executor, int stage) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Material item;
				if (executor instanceof SwordPlayer) {
					item = ((SwordPlayer) executor).getItemInUse();
					executor.getAssociatedEntity().sendMessage("basic method using: " + item);
				}
				else {
					UtilityAction.noOp(executor).run();
					return;
				}
				
				if (item.name().endsWith("_SWORD")) {
					basicSword(executor, stage, item).run();
					return;
				}
				
				UtilityAction.noOp(executor).run();
			}
		};
	}
	
	public static Runnable basicSword(SwordEntity executor, int stage, Material swordType) {
		double damage;
		switch (swordType) {
			case NETHERITE_SWORD -> damage = 15;
			case DIAMOND_SWORD -> damage = 11;
			case IRON_SWORD -> damage = 8;
			case GOLDEN_SWORD -> damage = 6;
			default -> damage = 4;
		}
		
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				
				double rangeMultiplier;
				ArrayList<Vector> basis;
				List<Vector> controlVectors;
				List<Double> bezierRatios;
				List<Vector> bezierVectors;
				
				switch (stage) {
					case 1 -> {
						rangeMultiplier = 2.0;
						basis = VectorUtil.getBasisWithoutPitch(ex.getEyeLocation());
						controlVectors = new ArrayList<>(Cache.dragonKillerArc);
						bezierRatios = new ArrayList<>(Cache.dragonKillerArcRatios);
						List<Vector> transformedControlVectors = controlVectors.stream()
								.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
								.toList();
						
						bezierVectors = BezierUtil.cubicBezierRational3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
								bezierRatios.getFirst(), bezierRatios.get(1), bezierRatios.get(2), bezierRatios.getLast(), 30);
					}
					// basic 0:
					default -> {
						rangeMultiplier = 0.8;
						basis = VectorUtil.getBasis(ex.getEyeLocation(), ex.getEyeLocation().getDirection());
						controlVectors = new ArrayList<>(Cache.forwardSwordSlash1);
						List<Vector> transformedControlVectors = controlVectors.stream()
								.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
								.toList();
						
						bezierVectors = BezierUtil.cubicBezier3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
								30);
					}
				}
				
				int duration = 5;
				int period = 1;
				int[] step = {0};
				int size = bezierVectors.size();
				int perIteration = bezierVectors.size()/duration;
				
				HashSet<LivingEntity> hit = new HashSet<>();
				Location o = ex.getEyeLocation();
				
				new BukkitRunnable() {
					@Override
					public void run() {
						for (int i = 0; i < perIteration; i++) {
							if (step[0] >= size) {
								cancel();
								break;
							}
							
							ex.setVelocity(new Vector(ex.getVelocity().getX() * 0.2, ex.getVelocity().getY() * 0.3, ex.getVelocity().getZ() * 0.2));
							ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 6));
							
							Vector v = bezierVectors.get(step[0]);
							Location l = o.clone().add(v);
							
							double spread = 0.25 + ((1.5* i) / 5);
							Vector offset = v.clone().normalize().multiply(spread);
							
							Location behind = l.clone().subtract(offset);
							Location exact = l.clone().add(v);
							Location ahead = l.clone().add(v).add(offset);
							
							Cache.testSoulFlameParticle.display(behind);
							Cache.testObsidianTearParticle.display(exact);
							Cache.testSoulFlameParticle.display(ahead);
							if (step[0] > size/2) {
								Location behind2 = l.clone().subtract(offset.clone().multiply(1.5));
								Cache.testLavaDripParticle.display(behind2);
							}
							
							HashSet<LivingEntity> curHit = HitboxUtil.line(ex, o, l, 0.4);
							executor.getAssociatedEntity().sendMessage(curHit.toString());
							for (LivingEntity target : curHit)
								if (!hit.contains(target))
									target.damage(damage, ex);
							hit.addAll(curHit);
							step[0]++;
						}
					}
				}.runTaskTimer(Sword.getInstance(), 0, period);
			}
		};
	}
}
