package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.util.*;
import btm.sword.util.sound.SoundType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MovementAction extends SwordAction {
	public static void dash(Combatant executor, boolean forward) {
		double maxDistance = 12;
		
		cast (executor, 5L, new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.entity();
				boolean onGround = executor.isGrounded();
				Location o = ex.getEyeLocation();
				
				Entity targetedItem = HitboxUtil.ray(o, o.getDirection(), maxDistance, 0.7,
						entity -> entity.getType() == EntityType.ITEM_DISPLAY && !entity.isDead());
				executor.message("Targeted: " + targetedItem);
				
				if (targetedItem instanceof ItemDisplay id
						&& !id.isDead()
						&& !id.getItemStack().isEmpty()) {
					RayTraceResult impedanceCheck = ex.getWorld().rayTraceBlocks(
							ex.getLocation().add(new Vector(0,0.3,0)),
							targetedItem.getLocation().subtract(ex.getLocation()).toVector().normalize(),
							maxDistance/2);
					
					Location loc = ex.getLocation().add(new Vector(0,0.3,0));
					Vector dir = targetedItem.getLocation().subtract(ex.getLocation()).toVector().normalize();
					int[] t = {0};
					new BukkitRunnable() {
						@Override
						public void run() {
							DisplayUtil.line(List.of(Cache.basicSwordBlueTransitionParticle), loc, dir,
									maxDistance/2, 0.3);
							t[0]+=2;
							if (t[0] > 60) cancel();
						}
					}.runTaskTimer(Sword.getInstance(), 0L, 2L);
					
					
					if (impedanceCheck != null)
						executor.message("Hit block: " + impedanceCheck.getHitBlock());
					
					if (impedanceCheck == null || impedanceCheck.getHitBlock() == null) {
						double length = id.getLocation().subtract(ex.getEyeLocation()).length();
						
						executor.setVelocity(ex.getEyeLocation().getDirection().multiply(Math.sqrt(length)));
						
						Vector u = executor.getFlatDir().multiply(forward ? 1 : -1).add(VectorUtil.UP.clone().multiply(0.25));
						
						new BukkitRunnable() {
							@Override
							public void run() {
								if (id.getLocation().subtract(ex.getEyeLocation()).lengthSquared() < 8.5) {
									BlockData blockData = ex.getLocation().add(new Vector(0,-0.75,0)).getBlock().getBlockData();
									new ParticleWrapper(Particle.DUST_PILLAR, 100, 1.25,1.25,1.25, blockData).display(ex.getLocation());
									SoundUtil.playSound(ex, SoundType.RANDOM_BANE_SLASH, 1f, 1f);
									executor.setVelocity(u);
									InteractiveItemArbiter.onGrab(id, executor);
									InteractiveItemArbiter.onGrabTest(id, executor);
								}
								else {
									Vector v = ex.getVelocity();
									ex.setVelocity(new Vector(v.getX()*0.3,v.getY()*0.3,v.getZ()*0.3));
									executor.message("Didn't get there");
								}
							}
						}.runTaskLater(Sword.getInstance(), 4L);
						return;
					}
					else {
						executor.message("You can't dash to that item...");
					}
				}
				
				double dashPower = 0.85;
				double s = forward ? dashPower : -dashPower;
				
				for (int i = 0; i < 2; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							Vector dir = ex.getEyeLocation().getDirection();
							if (onGround && (
									(forward && dir.dot(new Vector(0, 1, 0)) < 0)
									||
									(!forward && dir.dot(new Vector(0, 1, 0)) > 0))) {
								dir = executor.getFlatDir();
							}
							ex.setVelocity(dir.multiply(s));
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
				if (!onGround)
					executor.increaseAirDashesPerformed();
			}
		});
	}
	
	public static void toss(Combatant executor, SwordEntity target) {
		LivingEntity ex = executor.entity();
		LivingEntity t = target.entity();
		
		double baseForce = 1.5;
		double force = executor.calcValueAdditive(AspectType.MIGHT, 2.5, baseForce, 0.1);
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					t.setVelocity(new Vector(0,.25,0));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
		
		for (int i = 0; i < 3; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					t.setVelocity(ex.getEyeLocation().getDirection().multiply(force));
				}
			}.runTaskLater(Sword.getInstance(), i+2);
		}
		
		boolean[] check = {true};
		for (int i = 0; i < 15; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!check[0]) {
						cancel();
						return;
					}
					World world = t.getWorld();
					Location base = t.getLocation();
					double h = t.getEyeHeight();
					Vector v = t.getVelocity().normalize();
					Location l = base.add(new Vector(0,h * 0.3,0).add(v));
					
					Cache.throwTrailParticle.display(base.add(new Vector(0, h * 0.5, 0)));
					
					if (l.isFinite()) {
						RayTraceResult blockResult = world.rayTraceBlocks(l, v, h * 0.6, FluidCollisionMode.NEVER, true);
						
						Collection<LivingEntity> entities = world.getNearbyLivingEntities(
								l, 0.4, 0.4, 0.4,
								entity -> !entity.getUniqueId().equals(t.getUniqueId()) && !entity.getUniqueId().equals(ex.getUniqueId()));
						
						if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
							if (!entities.isEmpty()) {
								Vector knockbackDir = base.toVector().subtract(((LivingEntity) Arrays.stream(entities.toArray()).toList().getFirst()).getLocation().toVector());
								t.setVelocity(knockbackDir.normalize().multiply(0.3 * force));
							}
							world.createExplosion(l, 2, false, false);
							target.hit(executor, 3, 2, 30, 5,new Vector());
							check[0] = false;
						}
					}
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
