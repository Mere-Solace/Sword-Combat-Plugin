package btm.sword.combat.appliedEffect;

import btm.sword.system.playerdata.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;

public class DamageEffect extends AppliedEffect {
	private final float damage;
	
	public DamageEffect(float damage) {
		this.damage = damage;
	}
	
	@Override
	public void applyEffect(PlayerData executorData, HashSet<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			target.damage(damage, Bukkit.getPlayer(executorData.getUUID()));
		}
	}
}
