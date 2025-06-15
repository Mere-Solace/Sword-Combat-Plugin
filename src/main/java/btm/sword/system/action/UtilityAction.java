package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.Cache;
import btm.sword.util.HitboxUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class UtilityAction extends SwordAction {
	
	public static Runnable grab(Combatant executor) {
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
						int baseDuration = 60;
						int duration = baseDuration + (int)(0.2*executor.getCombatProfile().getStat(StatType.MIGHT));
						
						double baseGrabRange = 3;
						double range = baseGrabRange + (0.1*executor.getCombatProfile().getStat(StatType.MIGHT));
						
						double baseGrabThickness = 0.3;
						double grabThickness = baseGrabThickness + (0.1*executor.getCombatProfile().getStat(StatType.WILLPOWER));
						
						LivingEntity ex = executor.getAssociatedEntity();
						Location o = ex.getEyeLocation();
//						LivingEntity target = HitboxUtil.rayTrace(ex, range);
						LivingEntity target = HitboxUtil.firstInLineKnownLength(ex, o, o.getDirection(), range, grabThickness);
						if (target == null) {
							executor.getAssociatedEntity().sendMessage("Missed 'em");
							return;
						}
						executor.getAssociatedEntity().sendMessage("Grabbed him by te scruff o' da neck!");
						SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
						
						swordTarget.setBeingGrabbed(true);
						executor.setGrabbing(true);
						executor.setGrabbedEntity(swordTarget);
						
						target.damage(0.25, ex);
						Cache.grabCloudParticle.display(target.getLocation().add(new Vector(0, 1, 0)));
						
						final int[] ticks = {0};
						new BukkitRunnable() {
							@Override
							public void run() {
								if (ticks[0] >= duration - 1 || !executor.isGrabbing()) {
									executor.setGrabbing(false);
									swordTarget.setBeingGrabbed(false);
									cancel();
								}
								
								ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 6));
								
								double holdDist = 2;
								Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(holdDist)).subtract(target.getLocation().toVector());
								double distanceSquared = direction.lengthSquared();
								double bufferDistance = 0.5;
								double pullSpeed = 0.6;
								
								if (distanceSquared < bufferDistance*bufferDistance) {
									target.setVelocity(new Vector(0,target.getVelocity().getY()*0.3,0));
								}
								else {
									Vector velocity = direction.normalize().multiply(pullSpeed);
									if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
										target.setVelocity(velocity);
									}
								}
								
								ticks[0]++;
							}
						}.runTaskTimer(Sword.getInstance(), 0, 1);
					}
				};
				
				BukkitTask grabTask = scheduler.runTask(Sword.getInstance(), grabRunnable);
				
				executor.setGrabTask(grabTask);
			}
		};
	}
	
	public static Runnable noOp(SwordEntity executor) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				executor.getAssociatedEntity().sendMessage("Safely performing no operation");
			}
		};
	}
}
