package btm.sword.combat.appliedEffect;

import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
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
	public void applyEffect(SwordEntity executor, HashSet<SwordEntity> targets) {
		for (SwordEntity target : targets) {
			target.getAssociatedEntity().damage(damage, executor.getAssociatedEntity());
		}
	}
}
