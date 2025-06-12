package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.playerdata.StatType;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MovementAction extends SwordAction {
	
	public static Runnable dash(SwordEntity executor, boolean forward) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity ex = executor.getAssociatedEntity();
				double dashPower = executor instanceof SwordPlayer ? 0.75 + (0.1*((SwordPlayer) executor).getCombatProfile().getStat(StatType.CELERITY)) : 0.75;
				double m = forward ? dashPower : -dashPower;
				
				for (int i = 0; i < 2; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							ex.setVelocity(ex.getEyeLocation().getDirection().multiply(m).add(new Vector(0, .4, 0)));
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
			}
		};
	}
	
	public static Runnable translate(LivingEntity executor, LivingEntity target, double endDistance) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				Vector direction = executor.getLocation().toVector().subtract(target.getLocation().toVector());
				double distance = direction.length();
				
				if (distance < endDistance || distance == 0) {
					target.setVelocity(new Vector(0,0,0));
					return;
				}
				Vector velocity = direction.normalize();
				
				if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
					target.setVelocity(velocity);
				}
			}
		};
	}
	
	public static Runnable toss(SwordEntity tosser, SwordEntity tossee) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				LivingEntity bTosser = tosser.getAssociatedEntity();
				LivingEntity bTossee = tossee.getAssociatedEntity();
				
				double baseForce = 1.5;
				double force = tosser instanceof SwordPlayer ? baseForce + (int)(0.25*((SwordPlayer) tosser).getCombatProfile().getStat(StatType.MIGHT)) : baseForce;
				
				for (int i = 0; i < 2; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							bTossee.setVelocity(new Vector(0,.35,0));
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
				
				for (int i = 0; i < 3; i++) {
					new BukkitRunnable() {
						@Override
						public void run() {
							bTossee.setVelocity(bTosser.getEyeLocation().getDirection().multiply(force));
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
			}
		};
	}
}
