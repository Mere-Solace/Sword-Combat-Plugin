package btm.sword.combat.appliedEffect;

import btm.sword.Sword;
import btm.sword.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Objects;

public class DirectKnockbackEffect extends AppliedEffect {
	private final double knockbackStrength;
	private final int durationTicks;
	
	public DirectKnockbackEffect(double knockbackStrength, int durationTicks) {
		this.knockbackStrength = knockbackStrength;
		this.durationTicks = durationTicks;
	}
	
	@Override
	public void applyEffect(PlayerData executorData, HashSet<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			for (int i = 0; i < durationTicks; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						target.setVelocity(Objects.requireNonNull(Bukkit.getPlayer(executorData.getUUID())).getLocation().getDirection().multiply(knockbackStrength));
					}
				}.runTaskLater(Sword.getInstance(), 1);
			}
		}
	}
}
