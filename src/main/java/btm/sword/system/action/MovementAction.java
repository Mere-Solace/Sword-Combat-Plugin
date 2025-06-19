package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.playerdata.StatType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MovementAction extends SwordAction {
	
	public static Runnable dash(Combatant executor, boolean forward) {
		return new BukkitRunnable() {
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
			}
		};
	}
	
	public static Runnable toss(Combatant executor, SwordEntity target) {
		return new BukkitRunnable() {
			@Override
			public void run() {
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
					if (!check[0]) return;
					
					new BukkitRunnable() {
						@Override
						public void run() {
							if (t.getWorld().rayTraceBlocks(t.getLocation().add(new Vector(0,1,0)), t.getVelocity().normalize(), 0.6) != null) {
								t.getWorld().createExplosion(t.getEyeLocation().add(t.getVelocity().normalize()), 1, false, false);
								check[0] = false;
							}
						}
					}.runTaskLater(Sword.getInstance(), i);
				}
			}
		};
	}
}
