package btm.sword.system.action.utility;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

public class UtilityAction extends SwordAction {
	public static void allowDrop(SwordPlayer executor) {
		cast(executor, 0L, new BukkitRunnable() {
			@Override
			public void run() {
				executor.setCanDrop(true);
				new BukkitRunnable() {
					@Override
					public void run() {
						executor.setCanDrop(false);
					}
				}.runTaskLater(Sword.getInstance(), 5L);
			}
		});
	}
	
	public static void death(Combatant executor) {
		cast(executor, 0L, new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.entity();
				Location l = executor.entity().getEyeLocation();
				RayTraceResult ray = ex.getWorld().rayTraceEntities(l, l.getDirection(), 6, entity -> entity.getUniqueId() != ex.getUniqueId());
				if (ray != null && ray.getHitEntity() != null) {
					Entity target = ray.getHitEntity();
					if (target instanceof LivingEntity le)
						SwordEntityArbiter.getOrAdd(le.getUniqueId()).hit(executor,
								1000, 20000,
								1, l.getDirection().multiply(100));
					else {
						target.getWorld().createExplosion(target.getLocation(), 5, true, true);
					}
				}
			}
		});
	}
}
