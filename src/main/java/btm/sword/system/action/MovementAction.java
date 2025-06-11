package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.StatType;
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
}
