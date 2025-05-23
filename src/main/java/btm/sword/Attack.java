package btm.sword;

import btm.sword.effectshapes.EffectShape;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;

public abstract class Attack {
	private final int id;
	private final double baseDamage;
	private final double cooldownSeconds;
	private final EffectShape shape;
	
	public Attack(int id, double damage, double cooldownSeconds, EffectShape shape) {
		this.id = id;
		this.baseDamage = damage;
		this.cooldownSeconds = cooldownSeconds;
		this.shape = shape;
	}
	
	public int getId() {
		return id;
	}
	
	public double getBaseDamage() {
		return baseDamage;
	}
	
	public double getCooldownSeconds() {
		return cooldownSeconds;
	}
	
	public EffectShape getShape() {
		return shape;
	}

	public abstract void applyEffects(LivingEntity attacker, Collection<LivingEntity> targets);
	
	public abstract void applySelfEffects(LivingEntity attacker);
}