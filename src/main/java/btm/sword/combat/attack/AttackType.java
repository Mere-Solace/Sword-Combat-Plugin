package btm.sword.combat.attack;

import btm.sword.effectshape.EffectShape;
import btm.sword.effectshape.LineShape;
import btm.sword.visualeffect.LineVisual;
import btm.sword.visualeffect.PointVisual;
import btm.sword.visualeffect.TargetVisual;
import btm.sword.visualeffect.VisualEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public enum AttackType {
	
	GUNSHOT(10.0,
			List.of(
				new LineShape()),
			List.of(
				new LineVisual(
				List.of(Particle.WHITE_ASH),
				3, .01, .25),
				new TargetVisual(
				List.of(Particle.ENCHANTED_HIT, Particle.CRIT),
				7, .05),
				new PointVisual(
				List.of(Particle.POOF),
				2, 0)),
			50.0,
			2.0);
	
	private final double damage;
	private final List<EffectShape> effectShapes;
	private final List<VisualEffect> visualEffects;
	private final double range;
	private final double cooldown;
	
	AttackType(double damage, List<EffectShape> effectShapes, List<VisualEffect> visualEffects, double range, double cooldown) {
		this.damage = damage;
		this.effectShapes = effectShapes;
		this.visualEffects = visualEffects;
		this.range = range;
		this.cooldown = cooldown;
	}
	
	public double calculateDamage() {
		return damage;
	}
	
	public double calculateRange(double rangeMultiplier) {
		return range;
	}
	
	public double calculateCooldown() {
		return cooldown;
	}
	
	public List<EffectShape> getEffectShapes() {
		return effectShapes;
	}
	
	public List<VisualEffect> getVisualEffects() {
		return visualEffects;
	}
	
	public HashSet<LivingEntity> getTargets(Player player, Location origin, Vector direction, double range) {
		HashSet<LivingEntity> targets = new HashSet<>();
		for (EffectShape hitBox : effectShapes) {
			targets.addAll(hitBox.getTargets(player, origin, direction, range));
		}
		
		return targets;
	}
	
	public void drawEffects(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		for (VisualEffect visual : visualEffects) {
			visual.drawEffect(origin, direction, range, targets);
		}
	}
}
