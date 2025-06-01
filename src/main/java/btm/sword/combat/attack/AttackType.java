package btm.sword.combat.attack;

import btm.sword.combat.appliedEffect.AppliedEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Stack;

public abstract class AttackType {
	private float baseDamage;
	private final Stack<AppliedEffect> appliedEffects;
	
	public AttackType(float baseDamage, Stack<AppliedEffect> appliedEffects) {
		this.baseDamage = baseDamage;
		this.appliedEffects = appliedEffects;
	}
	
	public abstract HashSet<LivingEntity> getTargets(Player executor);
	
	public Stack<AppliedEffect> getAppliedEffects() {
		return appliedEffects;
	}
}
