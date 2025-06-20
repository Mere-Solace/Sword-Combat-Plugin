package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.playerdata.StatType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

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
		double force = baseForce + (int)(0.25*(executor).getCombatProfile().getStat(StatType.MIGHT));
		
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
					Location l = t.getLocation().add(new Vector(0,1,0).add(v));
					
					Collection<LivingEntity> entities = t.getWorld().getNearbyLivingEntities(
							l, 0.2, 0.2, 0.2, entity -> !entity.getUniqueId().equals(t.getUniqueId()));

					RayTraceResult blockResult = t.getWorld().rayTraceBlocks(l, v, 0.3);
					
					if ((blockResult != null && blockResult.getHitBlock() != null) || !entities.isEmpty()) {
						t.getWorld().createExplosion(t.getEyeLocation().add(t.getVelocity().normalize()), 2, false, false);
						check[0] = false;
					}
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
