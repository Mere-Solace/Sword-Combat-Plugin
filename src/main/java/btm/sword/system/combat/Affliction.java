package btm.sword.system.combat;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;

//STUNNED,        // Duration
//GROUNDED,       // Duration, Strength
//BLEEDING,       // Duration, Strength
//SLOWNESS,       // Duration, Strength
//ARMOR_BREAK     // Duration

public abstract class Affliction {
	private final boolean reapply;
	protected long tickDuration;
	protected double strength;
	
	protected int[] curTicks;
	private boolean shouldCancel;
	private boolean shouldReapply;
	
	public Affliction(boolean reapply, long tickDuration) {
		this.reapply = reapply;
		this.tickDuration = tickDuration;
		this.strength = -1;
		
		curTicks = new int[]{0};
		shouldCancel = false;
		shouldReapply = false;
	}
	
	public Affliction(boolean reapply, long tickDuration, double strength) {
		this(reapply, tickDuration);
		this.strength = strength;
	}
	
	public int getCurTicks() {
		return curTicks[0];
	}
	
	public long getTicksLeft() {
		return tickDuration - curTicks[0];
	}
	
	// this method should contain logic for only one-two ticks of the debuff!
	protected abstract void onApply(SwordEntity afflicted);
	
	protected abstract void end(SwordEntity afflicted);
	
	public void apply(SwordEntity afflicted) {
		if (!entityExists(afflicted))
			onApply(afflicted);
	}
	
	public void start(SwordEntity afflicted) {
		if (preApplicationCheck(afflicted)) return;
		
		apply(afflicted);
		new BukkitRunnable() {
			@Override
			public void run() {
				curTicks[0] += 2;
				if (shouldCancel || curTicks[0] > tickDuration) {
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
	
	public void cancel() {
		this.shouldCancel = true;
	}
	
	protected void extend(long tickExtension) {
		tickDuration += tickExtension;
		if (!reapply) shouldReapply = true;
	}
	
	protected boolean entityExists(SwordEntity afflicted) {
		if (afflicted == null || afflicted.entity().isDead()) {
			cancel();
			return true;
		}
		
		return false;
	}
	
	protected boolean preApplicationCheck(SwordEntity afflicted) {
		Affliction current = afflicted.getAffliction(this.getClass());
		if (current != null) {
			current.extend(tickDuration - current.getTicksLeft());
			return true;
		}
		return false;
	}
}
