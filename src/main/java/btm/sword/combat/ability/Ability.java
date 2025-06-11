package btm.sword.combat.ability;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class Ability {
	protected SwordEntity executor;
	private int delayTicks = 0;
	
	public Ability(SwordEntity executor) {
		this.executor = executor;
	}
	
	public BukkitTask returnRunnable() {
		return new BukkitRunnable() {
			@Override
			public void run() {
				onRun();
			}
		}.runTaskLater(Sword.getInstance(), delayTicks);
	}
	
	public abstract void onRun();
	
	
	public void setDelayTicks(int delayTicks) {
		this.delayTicks = delayTicks;
	}
}
