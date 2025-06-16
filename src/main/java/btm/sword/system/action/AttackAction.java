package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class AttackAction {
	
	public static Runnable basic(Combatant executor, int stage) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Material item;
				if (executor instanceof SwordPlayer) {
					item = ((SwordPlayer) executor).getItemInUse();
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
	
	public static Runnable basicSword(Combatant executor, int stage, Material swordType) {
		executor.getAssociatedEntity().sendMessage("On ground?: " + executor.onGround());
		
		double damage;
		switch (swordType) {
			case NETHERITE_SWORD -> damage = 15;
			case DIAMOND_SWORD -> damage = 11;
			case IRON_SWORD -> damage = 8;
			case GOLDEN_SWORD -> damage = 6;
			default -> damage = 4;
		}
		double[] d = {damage};
		
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				ArrayList<Vector> basis = VectorUtil.getBasis(ex.getEyeLocation(), ex.getEyeLocation().getDirection());
				
				double rangeMultiplier;
				List<Vector> controlVectors;
				List<Vector> bezierVectors;
				
				switch (stage) {
					case 1 -> {
						rangeMultiplier = 1.2;
						controlVectors = new ArrayList<>(Cache.basicSword2);
					}
					case 2 -> {
						rangeMultiplier = 1.2;
						controlVectors = new ArrayList<>(Cache.basicSword3);
					}
					// basic 0:
					default -> {
						rangeMultiplier = 1.2;
						controlVectors = new ArrayList<>(Cache.basicSword1);
					}
				}
				
				List<Vector> transformedControlVectors = controlVectors.stream()
						.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
						.toList();
				
				bezierVectors = BezierUtil.cubicBezier3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
						30);
				
				int duration = 3;
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
							
							ex.setVelocity(new Vector(
									ex.getVelocity().getX() * 0.5,
									ex.getVelocity().getY() * 0.4,
									ex.getVelocity().getZ() * 0.5));
							
							Vector v = bezierVectors.get(step[0]);
							Location l = o.clone().add(v);
							
							double offset = 0.1 + ((double) step[0] / (size*3));
							Vector vOff = v.clone().normalize().multiply(offset);
							Vector vOff2 = vOff.clone().multiply(1/2);
							Location lOff = l.clone().subtract(vOff);
							Location lOff2 = l.clone().subtract(vOff2);
							
							Cache.basicSwordBlueTransitionParticle.display(l);
							Cache.testSoulFlameParticle.display(l);
							Cache.basicSwordBlueTransitionParticle.display(lOff);
							Cache.testSoulFlameParticle.display(lOff);
							Cache.basicSwordBlueTransitionParticle.display(lOff2);
							Cache.testSoulFlameParticle.display(lOff2);
							
							if (step[0] > size * (0.3)) {
								Location p = l.clone().subtract(vOff.clone().multiply(1.2));
								Cache.basicSwordBlueTransitionParticle.display(p);
							}
							if (step[0] > size * (0.6)) {
								Location p = l.clone().subtract(vOff.clone().multiply(1.5));
								Cache.basicSwordBlueTransitionParticle.display(p);
							}
							
							HashSet<LivingEntity> curHit = HitboxUtil.line(ex, o, l, 0.4);
							for (LivingEntity target : curHit)
								if (!hit.contains(target)) {
									target.damage(damage, ex);
									Cache.basicSwordHit1.display(target.getLocation());
									Cache.basicSwordHit2.display(target.getLocation());
								}
							hit.addAll(curHit);
							
							if (step[0] < size-1) {
								Vector direction = bezierVectors.get(step[0] + 1).clone().subtract(v);
								RayTraceResult result = ex.getWorld().rayTraceBlocks(l, direction, 0.3);
								if (result != null) {
									new ParticleWrapper(Particle.BLOCK, 10, 0.5, 0.5, 0.5, Objects.requireNonNull(result.getHitBlock()).getBlockData()).display(l);
									Cache.basicSwordEnterGround.display(l);
									// For each point that passes through a block, reduce damage by one, down to a min of 20% of original damage
									d[0] = Math.max(damage*(0.2), d[0]-1);
								}
							}
							
							step[0]++;
						}
					}
				}.runTaskTimer(Sword.getInstance(), 0, period);
			}
		};
	}
	
	public static Runnable heavy(Combatant executor, int stage) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Material item;
				if (executor instanceof SwordPlayer) {
					item = ((SwordPlayer) executor).getItemInUse();
				}
				else {
					UtilityAction.noOp(executor).run();
					return;
				}
				
				if (item.name().endsWith("_SWORD")) {
					heavySword(executor, stage, item).run();
					return;
				}
				
				UtilityAction.noOp(executor).run();
			}
		};
	}
	
	public static Runnable heavySword(Combatant executor, int stage, Material swordType) {
		double damage;
		switch (swordType) {
			case NETHERITE_SWORD -> damage = 20;
			case DIAMOND_SWORD -> damage = 15;
			case IRON_SWORD -> damage = 12;
			case GOLDEN_SWORD -> damage = 10;
			default -> damage = 7;
		}
		
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				ArrayList<Vector> basis = VectorUtil.getBasisWithoutPitch(ex.getEyeLocation());
				
				double rangeMultiplier;
				List<Vector> controlVectors;
				List<Double> bezierRatios;
				List<Vector> bezierVectors;
				
				switch (stage) {
					case 1 -> {
						rangeMultiplier = 2;
						controlVectors = new ArrayList<>(Cache.dragonKillerArc);
						bezierRatios = new ArrayList<>(Cache.dragonKillerArcRatios);
					}
					case 2 -> {
						rangeMultiplier = 3;
						controlVectors = new ArrayList<>(Cache.dragonKillerArc);
						bezierRatios = new ArrayList<>(Cache.dragonKillerArcRatios);
					}
					default -> {
						rangeMultiplier = 1.5;
						controlVectors = new ArrayList<>(Cache.dragonKillerArc);
						bezierRatios = new ArrayList<>(Cache.dragonKillerArcRatios);
					}
				}
				
				List<Vector> transformedControlVectors = controlVectors.stream()
						.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
						.toList();
				
				bezierVectors = BezierUtil.cubicBezierRational3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
						bezierRatios.getFirst(), bezierRatios.get(1), bezierRatios.get(2), bezierRatios.getLast(), 40);
				
				int duration = 4;
				int period = 1;
				int[] step = {0};
				int size = bezierVectors.size();
				int perIteration = bezierVectors.size()/duration;
				
				HashSet<LivingEntity> hit = new HashSet<>();
				Location o = ex.getEyeLocation();
				Location b = ex.getLocation();
				
				new BukkitRunnable() {
					@Override
					public void run() {
						for (int i = 0; i < perIteration; i++) {
							if (step[0] >= size) {
								cancel();
								break;
							}

							ex.setVelocity(new Vector(
									ex.getVelocity().getX() * 0,
									ex.getVelocity().getY() * 0,
									ex.getVelocity().getZ() * 0));
							
							Vector v = bezierVectors.get(step[0]).add(new Vector(0, 1.5, 0));
							Location l = b.clone().add(v);
							
							double offset = 0.1 + ((double) step[0] / 35);
							Vector vOff = v.clone().normalize().multiply(offset);
							Location lOff = l.clone().subtract(vOff);
							
							Cache.basicSwordBlueTransitionParticle.display(l);
							Cache.testSoulFlameParticle.display(l);
							Cache.basicSwordBlueTransitionParticle.display(lOff);
							
							if (step[0] > size * 0.6) {
								Vector v3 = v.clone().multiply(0.875);
								Location l4 = b.clone().add(v3);
								Cache.basicSwordBlueTransitionParticle.display(l4);
								
								Location lOff2 = l.clone().subtract(vOff.clone().multiply(1.1));
								Cache.basicSwordBlueTransitionParticle.display(lOff2);
							}
							
							HashSet<LivingEntity> curHit = HitboxUtil.line(ex, o, l, 0.4);
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
	
	public static Runnable sideStep(Combatant executor, boolean right) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Material item;
				if (executor instanceof SwordPlayer) {
					item = ((SwordPlayer) executor).getItemInUse();
				}
				else {
					UtilityAction.noOp(executor).run();
					return;
				}
				
				if (item.name().endsWith("_SWORD")) {
					sideStepSword(executor, right, item).run();
					return;
				}
				
				UtilityAction.noOp(executor).run();
			}
		};
	}
	
	public static Runnable sideStepSword(Combatant executor, boolean right, Material swordType) {
		executor.getAssociatedEntity().sendMessage("On ground?: " + executor.onGround());
		
		double damage;
		switch (swordType) {
			case NETHERITE_SWORD -> damage = 15;
			case DIAMOND_SWORD -> damage = 11;
			case IRON_SWORD -> damage = 8;
			case GOLDEN_SWORD -> damage = 6;
			default -> damage = 4;
		}
		double[] d = {damage};
		
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				Location o = ex.getEyeLocation();
				Vector e = o.getDirection();
				ArrayList<Vector> basis = VectorUtil.getBasis(o, e);
				Vector r = basis.getFirst();
				
				double rangeMultiplier = 1;
				List<Vector> controlVectors;
				List<Vector> bezierVectors;
				
				controlVectors = right ? Cache.sideStepSwordR : Cache.sideStepSwordL;
				
				List<Vector> transformedControlVectors = controlVectors.stream()
						.map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
						.toList();
				
				bezierVectors = BezierUtil.cubicBezier3D(transformedControlVectors.getFirst(),transformedControlVectors.get(1), transformedControlVectors.get(2), transformedControlVectors.getLast(),
						40);
				
				int duration = 5;
				int period = 1;
				int[] step = {0};
				int size = bezierVectors.size();
				int perIteration = bezierVectors.size()/duration;
				
				HashSet<LivingEntity> hit = new HashSet<>();
				
				new BukkitRunnable() {
					@Override
					public void run() {
						for (int i = 0; i < perIteration; i++) {
							if (step[0] >= size) {
								cancel();
								return;
							}
							
							if (step[0] < size * 0.1) {
								Vector dashVelocity = right ? r.clone().add(new Vector(0, 0.2, 0)) : r.clone().multiply(-1).add(new Vector(0, 0.2, 0));
								ex.setVelocity(dashVelocity);
							}
							
							Vector v = bezierVectors.get(step[0]);
							Location l = ex.getEyeLocation().clone().add(v);
							
							double offset = 0.1 + ((double) step[0] / (size*3));
							Vector vOff = v.clone().normalize().multiply(offset);
							Vector vOff2 = vOff.clone().multiply(1/2);
							Location lOff = l.clone().subtract(vOff);
							Location lOff2 = l.clone().subtract(vOff2);
							
							Cache.basicSwordBlueTransitionParticle.display(l);
							Cache.testSoulFlameParticle.display(l);
							Cache.basicSwordBlueTransitionParticle.display(lOff);
							Cache.testSoulFlameParticle.display(lOff);
							Cache.basicSwordBlueTransitionParticle.display(lOff2);
							Cache.testSoulFlameParticle.display(lOff2);
							
							if (step[0] > size * (0.3)) {
								Location p = l.clone().subtract(vOff.clone().multiply(1.2));
								Cache.basicSwordBlueTransitionParticle.display(p);
							}
							if (step[0] > size * (0.6)) {
								Location p = l.clone().subtract(vOff.clone().multiply(1.5));
								Cache.basicSwordBlueTransitionParticle.display(p);
							}
							
							HashSet<LivingEntity> curHit = HitboxUtil.line(ex, o, l, 0.4);
							for (LivingEntity target : curHit)
								if (!hit.contains(target)) {
									target.damage(damage, ex);
									Cache.basicSwordHit1.display(target.getLocation());
									Cache.basicSwordHit2.display(target.getLocation());
								}
							hit.addAll(curHit);
							
							if (step[0] < size-1) {
								Vector direction = bezierVectors.get(step[0] + 1).clone().subtract(v);
								RayTraceResult result = ex.getWorld().rayTraceBlocks(l, direction, 0.3);
								if (result != null) {
									new ParticleWrapper(Particle.BLOCK, 10, 0.5, 0.5, 0.5, Objects.requireNonNull(result.getHitBlock()).getBlockData()).display(l);
									Cache.basicSwordEnterGround.display(l);
									// For each point that passes through a block, reduce damage by one, down to a min of 20% of original damage
									d[0] = Math.max(damage*(0.2), d[0]-1);
								}
							}
							
							step[0]++;
						}
					}
				}.runTaskTimer(Sword.getInstance(), 0, period);
			}
		};
	}
}
