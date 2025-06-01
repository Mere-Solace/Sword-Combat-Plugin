package btm.sword.combat.appliedEffect;

import org.bukkit.entity.LivingEntity;

import java.util.Stack;

public class DamageEffect extends AppliedEffect{
	private final float damage;
	
	public DamageEffect(float damage) {
		this.damage = damage;
	}
	
	@Override
	public void applyEffect(LivingEntity executor, Stack<LivingEntity> targets) {
		while (!targets.empty()) {
			targets.pop().damage(damage, executor);
		}
	}
}
