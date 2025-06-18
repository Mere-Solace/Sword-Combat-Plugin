package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.HitboxUtil;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class UtilityAction extends SwordAction {
	
	public static Runnable grab(Combatant executor) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				int baseDuration = 60;
				double baseGrabRange = 3;
				double baseGrabThickness = 0.3;
				
				int duration = (int) executor.calcValue(StatType.MIGHT, baseDuration, 0.2);
				double range = executor.calcValue(StatType.WILLPOWER, baseGrabRange, 0.1);
				double grabThickness = executor.calcValue(StatType.WILLPOWER, baseGrabThickness, 0.1);
				
				LivingEntity ex = executor.entity();
				Location o = ex.getEyeLocation();

				LivingEntity target = HitboxUtil.firstInLineKnownLength(ex, o, o.getDirection(), range, grabThickness);
				if (target == null) {
					executor.entity().sendMessage("Missed, stopping your current grab action: " + executor.getAbilityTask());
					disassociateTask(executor);
					return;
				}
				SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
				
				executor.onGrab(swordTarget);
				
				final int[] ticks = {0};
				new BukkitRunnable() {
					@Override
					public void run() {
						if (ticks[0] >= duration - 1) {
							executor.onGrabLetGo();
							cancel();
							return;
						}
						if (!executor.isGrabbing()) {
							executor.onGrabThrow();
							cancel();
							return;
						}
						Vector v = ex.getVelocity();
						ex.setVelocity(new Vector(v.getX() * 0.2, v.getY() * 0.7,v.getZ() * 0.2));
						
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
	}
	
	public static Runnable noOp(Combatant executor) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				executor.entity().sendMessage("Safely performing no operation");
				
				disassociateTask(executor);
			}
		};
	}
}
