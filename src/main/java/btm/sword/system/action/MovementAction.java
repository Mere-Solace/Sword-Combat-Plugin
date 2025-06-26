package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.util.Cache;
import btm.sword.util.EntityUtil;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collection;

public class MovementAction extends SwordAction {
	
	public static void dash(Combatant executor, boolean forward) {
		cast (executor, 5L, new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.entity();

				double dashPower = 0.6;
				double s = forward ? dashPower : -dashPower;
				
				Vector velocity;
				if (EntityUtil.isOnGround(ex)) {
					double yaw = Math.toRadians(ex.getEyeLocation().getYaw());
					Vector f = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
					velocity = f.multiply(s).add(new Vector(0, 0.4, 0));
				}
				else {
					velocity = ex.getEyeLocation().getDirection().multiply(s * 1.25);
				}
				
				for (int i = 0; i < 2; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							ex.setVelocity(velocity);
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
				executor.message("before: " + executor.getAirDashesPerformed());
				if (!EntityUtil.isOnGround(ex))
					executor.increaseAirDashesPerformed();
				executor.message("  Cur air dodges performed: " + executor.getAirDashesPerformed());
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
							check[0] = false;
						}
					}
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
