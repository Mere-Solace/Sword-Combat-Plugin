package btm.sword.combat.appliedEffect;

import org.bukkit.entity.LivingEntity;

import java.util.Stack;

public abstract class AppliedEffect {
	public abstract void applyEffect(LivingEntity executor, Stack<LivingEntity> targets);
}
