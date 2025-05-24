package btm.sword.combat.attack;

import btm.sword.effectshape.EffectShape;
import btm.sword.effectshape.LineShape;
import btm.sword.visualeffect.LineVisual;
import btm.sword.visualeffect.VisualEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public enum AttackType {
	GUNSHOT(10.0, new LineShape(), new LineVisual(List.of(Particle.FLAME, Particle.CRIT), 0.25), 50.0, 2.0);
	
	private final double damage;
	private final EffectShape effectShape;
	private final VisualEffect visualEffect;
	private final double range;
	private final double cooldown;
	
	AttackType(double damage, EffectShape effectShape, VisualEffect visualEffect, double range, double cooldown) {
		this.damage = damage;
		this.effectShape = effectShape;
		this.visualEffect = visualEffect;
		this.range = range;
		this.cooldown = cooldown;
	}
	
	public double calculateDamage() {
		return damage; // * stats.getStrengthMultiplier(); // TODO: PlayerData class w/ stats
	}
	
	public double calculateRange(double rangeMultiplier) {
		return range;
	}
	
	public double calculateCooldown() {
		return cooldown;
	}
	
	public EffectShape getEffectShape() {
		return effectShape;
	}
	
	public VisualEffect getVisualEffect() {
		return visualEffect;
	}
	
	public Collection<LivingEntity> getTargets(Player player, Location origin, Vector direction, double range) {
		return effectShape.getTargets(player, origin, direction, range);
	}
	
	public void drawEffect(Location origin, Vector direction, double range) {
		visualEffect.drawEffect(origin, direction, range);
	}
}
