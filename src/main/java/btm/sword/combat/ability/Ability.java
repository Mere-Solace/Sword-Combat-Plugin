package btm.sword.combat.ability;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public abstract class Ability implements Runnable{
	protected SwordEntity executor;
	private final int delayTicks;
	
	public Ability(AbilityOptions options, SwordEntity executor) {
		this.executor = executor;
		delayTicks = options.getDelayTicks();
	}
	
	public abstract void onRun();
	
	@Override
	public void run() {
		onRun();
	}
	
	public int getDelayTicks() {
		return delayTicks;
	}
}
