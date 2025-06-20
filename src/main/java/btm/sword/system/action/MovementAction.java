package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.playerdata.StatType;
import btm.sword.util.Cache;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
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

				double dashPower = 0.7;
				double m = forward ? dashPower : -dashPower;
				
				for (int i = 0; i < 2; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							ex.setVelocity(ex.getEyeLocation().getDirection().multiply(m).add(new Vector(0, .2, 0)));
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
				executor.increaseAirDashesPerformed();
			}
		});
	}
	
	public static void toss(Combatant executor, SwordEntity target) {
		LivingEntity ex = executor.entity();
		LivingEntity t = target.entity();
		
		double baseForce = 1.5;
		double force = executor.calcValueAdditive(StatType.MIGHT, 2.5, baseForce, 0.1);
		
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
					Vector v = t.getVelocity().normalize();
					Location l = t.getLocation().add(new Vector(0,t.getEyeHeight() * 0.3,0).add(v));
					
					Cache.throwTrailParticle.display(l);
					
					RayTraceResult blockResult = t.getWorld().rayTraceBlocks(l, v, 0.4, FluidCollisionMode.NEVER, true);
					
					Collection<LivingEntity> entities = t.getWorld().getNearbyLivingEntities(
							l, 0.4, 0.4, 0.4,
							entity -> !entity.getUniqueId().equals(t.getUniqueId()) && !entity.getUniqueId().equals(ex.getUniqueId()));
					
					if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
						if (!entities.isEmpty()) {
							Vector knockbackDir = t.getLocation().toVector().subtract(((LivingEntity)Arrays.stream(entities.toArray()).toList().getFirst()).getLocation().toVector());
							t.setVelocity(knockbackDir.normalize().multiply(0.3 * force));
						}
						t.getWorld().createExplosion(l, 2, false, false);
						check[0] = false;
					}
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
