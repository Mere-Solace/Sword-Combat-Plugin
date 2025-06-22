package btm.sword.system.combat;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;

//STUNNED,        // Duration
//GROUNDED,       // Duration
//BLEEDING,       // Duration, Strength
//SLOWNESS,       // Duration, Strength
//ARMOR_BREAK     // Duration, Strength

public abstract class Affliction {
	private final boolean reapply;
	private long tickDuration;
	private int strength;
	
	private boolean shouldCancel;
	private boolean shouldReapply;
	
	public Affliction(boolean reapply, long tickDuration) {
		this.reapply = reapply;
		this.tickDuration = tickDuration;
		this.strength = -1;
		shouldCancel = false;
		shouldReapply = false;
	}
	
	public Affliction(boolean reapply, long tickDuration, int strength) {
		this(reapply, tickDuration);
		this.strength = strength;
	}
	
	// this method should contain logic for only one-two ticks of the debuff!
	protected abstract void apply(SwordEntity afflicted);
	
	protected abstract void end(SwordEntity afflicted);
	
	public void start(SwordEntity afflicted) {
		apply(afflicted);
		int[] ticks = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				ticks[0] += 2;
				if (shouldCancel || ticks[0] > tickDuration) {
					end(afflicted);
					cancel();
				}
				else if (reapply || shouldReapply) {
					if (shouldReapply) shouldReapply = false;
					apply(afflicted);
				}
			}
		}.runTaskTimer(Sword.getInstance(), 2L, 2L);
	}
	
	public void cancel(boolean shouldCancel) {
		this.shouldCancel = shouldCancel;
	}
	
	protected void extend(long tickExtension) {
		tickDuration += tickExtension;
		if (!reapply) shouldReapply = true;
	}
}
