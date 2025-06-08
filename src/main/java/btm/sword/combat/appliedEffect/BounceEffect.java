package btm.sword.combat.appliedEffect;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class BounceEffect extends AppliedEffect {
	private final double bounceStrength;
	private final int durationTicks;
	
	public BounceEffect(double bounceStrength, int durationTicks) {
		this.bounceStrength = bounceStrength;
		this.durationTicks = durationTicks;
	}
	
	@Override
	public void applyEffect(SwordEntity executor, HashSet<SwordEntity> targets) {
		for (SwordEntity target : targets) {
			for (int i = 0; i < durationTicks; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						target.getAssociatedEntity().setVelocity(new Vector(0,1,0).multiply(bounceStrength));
					}
				}.runTaskLater(Sword.getInstance(), 1);
			}
		}
	}
}
