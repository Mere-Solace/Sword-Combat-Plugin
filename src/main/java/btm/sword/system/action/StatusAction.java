package btm.sword.system.action;

import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class StatusAction {
	
	public static Runnable halt(List<LivingEntity> targets, int duration) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				for (LivingEntity target : targets) {
					target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255));
				}
			}
		};
	}
	
	public static Runnable clear(List<SwordEntity> targets) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				for (LivingEntity target : SwordEntityArbiter.convertToLivingEntities(targets)) {
					target.clearActivePotionEffects();
				}
			}
		};
	}
}
