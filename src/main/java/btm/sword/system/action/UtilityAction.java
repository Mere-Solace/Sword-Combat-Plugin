package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.HitboxUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class UtilityAction extends SwordAction {
	
	public static Runnable grab(SwordEntity executor) {
		// outer runnable
		return new BukkitRunnable() {
			@Override
			public void run() {
				// define scheduler for other tasks
				final BukkitScheduler scheduler = Bukkit.getScheduler();
				
				// inner grab runnable for creation of an accessible bukkit task that can be
				// assigned to the player's current grab task, and canceled as needed
				Runnable grabRunnable = new BukkitRunnable() {
					@Override
					public void run() {
						LivingEntity ex = executor.getAssociatedEntity();
						double range = executor instanceof SwordPlayer ? 2.5 + (0.1*((SwordPlayer) executor).getCombatProfile().getStat(StatType.MIGHT)) : 2.5;
						LivingEntity target = HitboxUtil.rayTrace(ex, range);
						
						if (target == null) {
							ex.sendMessage("Miss");
							return;
						}
						
						SwordEntity swordTarget = SwordEntityArbiter.get(target.getUniqueId());
						
						ex.sendMessage(ex + " grabbed " + target);
						
						if (executor instanceof SwordPlayer) {
							((SwordPlayer) executor).setGrabbing(true);
							((SwordPlayer) executor).setGrabbedEntity(swordTarget);
						}
						
						int baseDuration = 60;
						final int duration = executor instanceof SwordPlayer ? baseDuration + (int)(0.2*((SwordPlayer) executor).getCombatProfile().getStat(StatType.MIGHT)) : baseDuration;
						
						final int[] ticks = {0};
						new BukkitRunnable() {
							@Override
							public void run() {
								if (ticks[0] >= duration - 1) {
									if (executor instanceof SwordPlayer) {
										((SwordPlayer) executor).setGrabbing(false);
									}
									cancel();
								}
								
								else if (executor instanceof SwordPlayer && !((SwordPlayer) executor).isGrabbing()) {
									cancel();
								}
								ticks[0]++;
								
								
								ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 6));
								
								target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 10));
								
								Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(1.5)).subtract(target.getLocation().toVector());
								double distance = direction.length();
								
								if (distance < 0.1) {
									target.setVelocity(new Vector(0,0,0));
								}
								else {
									Vector velocity = direction.normalize().multiply(0.75);
									if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
										target.setVelocity(velocity);
									}
								}
							}
						}.runTaskTimer(Sword.getInstance(), 0, 1);
					}
				};
				
				BukkitTask grabTask = scheduler.runTask(Sword.getInstance(), grabRunnable);
				
				if (executor instanceof SwordPlayer) {
					((SwordPlayer) executor).setGrabTask(grabTask);
				}
			}
		};
	}
}
