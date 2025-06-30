package btm.sword.system.action.utility;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.SoundUtils;
import btm.sword.util.sound.SoundType;
import org.bukkit.Location;
import org.bukkit.Particle;
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
	
	public static void soundTest(Combatant executor, int startIndex) {
		int i = 0;
		for (SoundType soundType : SoundType.values()) {
			if (i < startIndex) {
				i++;
				continue;
			}
			int finalI = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					SoundUtils.playSound(executor.entity(), soundType, 1f, 1f);
					executor.message("i: " + finalI + ", " + soundType.getKey());
				}
			}.runTaskLater(Sword.getInstance(), 30L * (i-startIndex));
			i++;
		}
	}
	
	public static void particleTest(Combatant executor) {
		Location l = executor.getChestLocation().add(executor.entity().getEyeLocation().getDirection().multiply(2));
		int i = 0;
		for (Particle particle : Particle.values()) {
			int finalI = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						new ParticleWrapper(particle, 1, 0, 0, 0, 0).display(l);
					} catch (Exception e) {
						executor.message(e.getMessage());
					}
					executor.message("i: " + finalI + ", " + particle.name());
				}
			}.runTaskLater(Sword.getInstance(), 30L * i);
			i++;
		}
	}
}
