package btm.sword.combat.attack;

import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.player.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;

public abstract class AttackType {
	private final List<AppliedEffect> appliedEffects;
	
	public AttackType(List<AppliedEffect> appliedEffects) {
		this.appliedEffects = appliedEffects;
	}
	
	public abstract HashSet<LivingEntity> getTargets(Player executor);
	
	public void applyEffects(PlayerData executorData, HashSet<LivingEntity> targets) {
		for (AppliedEffect effect : appliedEffects) {
			effect.applyEffect(executorData, targets);
		}
	}
}
