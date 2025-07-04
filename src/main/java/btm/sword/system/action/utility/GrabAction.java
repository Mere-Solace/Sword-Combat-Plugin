package btm.sword.system.action.utility;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.util.HitboxUtil;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class GrabAction extends SwordAction {
	public static void grab(Combatant executor) {
		cast(executor, 12L,
		new BukkitRunnable() {
			@Override
			public void run() {
				int baseDuration = 60;
				double baseGrabRange = 3;
				double baseGrabThickness = 0.4;
				
				long duration = (long) executor.calcValueAdditive(AspectType.MIGHT, 100L, baseDuration, 0.2);
				double range = executor.calcValueAdditive(AspectType.WILLPOWER, 4.5, baseGrabRange, 0.1);
				double grabThickness = executor.calcValueAdditive(AspectType.WILLPOWER, 0.75, baseGrabThickness, 0.1);
				
				LivingEntity ex = executor.entity();
				Location o = ex.getEyeLocation();
				
				Entity grabbed  = HitboxUtil.ray(o, o.getDirection(), range, grabThickness, entity -> !entity.isDead() && entity.getUniqueId() != ex.getUniqueId());
				executor.message("Grabbed: " + grabbed);
				
				if (grabbed instanceof ItemDisplay id) {
					InteractiveItemArbiter.onGrab(id, executor);
					return;
				}
				
				HashSet<LivingEntity> hit = HitboxUtil.line(ex, o, o.getDirection(), range, grabThickness);
				if (hit.isEmpty()) {
					return;
				}
				
				LivingEntity target = hit.stream().toList().getFirst();
				
				if (target == null) {
					return;
				}
				
				SwordEntity swordTarget = SwordEntityArbiter.getOrAdd(target.getUniqueId());
				if (swordTarget.isHit()) return;
				
				executor.onGrab(swordTarget);
				
				final int[] ticks = {0};
				new BukkitRunnable() {
					@Override
					public void run() {
						if (ticks[0] >= duration - 1 || target.isDead()) {
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
						ex.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 2, 1));
						ex.setVelocity(new Vector(v.getX() * 0.2, v.getY(),v.getZ() * 0.2));
						
						double holdDist = 2;
						Vector direction = ex.getLocation().toVector().add(ex.getEyeLocation().getDirection().multiply(holdDist)).subtract(target.getLocation().toVector());
						double distanceSquared = direction.lengthSquared();
						double bufferDistance = 0.4;
						double pullSpeed = 0.6;
						
						if (distanceSquared < bufferDistance*bufferDistance) {
							target.setVelocity(new Vector(0,target.getVelocity().getY()*0.25,0));
						}
						else {
							double force = pullSpeed;
							if (Math.abs(target.getEyeLocation().getY() - ex.getEyeLocation().getY()) > 1.2) {
								force *= 2;
							}
							Vector velocity = direction.normalize().multiply(force);
							if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
								target.setVelocity(velocity);
							}
						}
						ticks[0]++;
					}
				}.runTaskTimer(Sword.getInstance(), 0, 1);
			}
		});
	}
}
