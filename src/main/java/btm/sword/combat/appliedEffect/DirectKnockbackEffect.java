package btm.sword.combat.appliedEffect;

import btm.sword.Sword;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

public class DirectKnockbackEffect extends AppliedEffect {
	private final double knockbackStrength;
	private final int durationTicks;
	
	public DirectKnockbackEffect(double knockbackStrength, int durationTicks) {
		this.knockbackStrength = knockbackStrength;
		this.durationTicks = durationTicks;
	}
	
	@Override
	public void applyEffect(SwordEntity executor, HashSet<SwordEntity> targets) {
		for (SwordEntity target : targets) {
			for (int i = 0; i < durationTicks; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						target.getAssociatedEntity().setVelocity(
								executor.getAssociatedEntity().
										getLocation().
										getDirection().
										multiply(knockbackStrength));
					}
				}.runTaskLater(Sword.getInstance(), 1);
			}
		}
	}
}
