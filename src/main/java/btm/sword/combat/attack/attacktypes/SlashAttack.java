package btm.sword.combat.attack.attacktypes;

import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.combat.attack.AttackType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Stack;

public class SlashAttack extends AttackType {
	
	public SlashAttack(float baseDamage, Stack<AppliedEffect> appliedEffects) {
		super(baseDamage, appliedEffects);
	}
	
	@Override
	public HashSet<LivingEntity> getTargets(Player executor) {
		return null;
	}
}
