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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class AttackAction extends SwordAction {
	
	public static Runnable basic(Combatant executor, int stage) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Material item;
				// basic grounded attacks
				if (executor.onGround()) {
					
					if (executor instanceof SwordPlayer) {
						item = ((SwordPlayer) executor).getItemInUse();
						
						if (item.name().endsWith("_SWORD")) {
							s.runTask(plugin, basicSword(executor, stage, item));
						}
						
					}
				}
				// basic aerials
//				else {
//
//				}
			}
		};
	}
	
	public static Runnable basicSword(Combatant executor, int stage, Material swordType) {
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
				LivingEntity ex = executor.entity();
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
								disassociateTask(executor);
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
							
							int step2 = step[0]*step[0];
							if (step[0] > size * (0.2)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.5 + ((double) size /16000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.4)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.75 + ((double) size /14500)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.6)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.75 + ((double) size /10000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
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
				// basic grounded attacks
				if (executor.onGround()) {
					
					if (executor instanceof SwordPlayer) {
						item = ((SwordPlayer) executor).getItemInUse();
						
						if (item.name().endsWith("_SWORD")) {
							s.runTask(plugin, heavySword(executor, stage, item));
						}
						
					}
				}
				// basic aerials
//				else {
//
//				}
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
				LivingEntity ex = executor.entity();
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
								disassociateTask(executor);
								cancel();
								break;
							}

							ex.setVelocity(new Vector(
									ex.getVelocity().getX() * 0,
									ex.getVelocity().getY() * 0,
									ex.getVelocity().getZ() * 0));
							
							Vector v = bezierVectors.get(step[0]).add(new Vector(0, 1.5, 0));
							Location l = b.clone().add(v);
							
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
							
							int step2 = step[0]*step[0];
							if (step[0] > size * (0.2)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.5 + ((double) size /40000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.4)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.75 + ((double) size /34000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.6)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.8 + ((double) size /20000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
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
				BukkitTask task = null;
				Material item;
				// basic grounded attacks
				if (executor.onGround()) {
					
					if (executor instanceof SwordPlayer) {
						item = ((SwordPlayer) executor).getItemInUse();
						
						if (item.name().endsWith("_SWORD")) {
							s.runTask(plugin, sideStepSword(executor, right, item));
						}
						
					}
				}
				// basic aerials
//				else {
//
//				}M
			}
		};
	}
	
	public static Runnable sideStepSword(Combatant executor, boolean right, Material swordType) {
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
				if (!executor.onGround()) {
					disassociateTask(executor);
					cancel();
					return;
				}
				LivingEntity ex = executor.entity();
				Location o = ex.getEyeLocation();
				Vector e = o.getDirection();
				ArrayList<Vector> basis = VectorUtil.getBasisWithoutPitch(o);
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
								disassociateTask(executor);
								cancel();
								return;
							}
							
							if (step[0] < size * 0.15) {
								Vector dashVelocity = right ? r.clone().add(new Vector(0, 0.2, 0)) : r.clone().multiply(-1).add(new Vector(0, 0.2, 0));
								ex.setVelocity(dashVelocity);
							}
							
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
							
							int step2 = step[0]*step[0];
							if (step[0] > size * (0.2)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.5 + ((double) size /24000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.4)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.75 + ((double) size /19000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
							}
							if (step[0] > size * (0.6)) {
								Location p = l.clone().subtract(vOff.clone().multiply(0.8 + ((double) size /15000)*step2));
								Cache.basicSwordWhiteTransitionParticle.display(p);
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
