package btm.sword.combat.attack;

import btm.sword.effectshape.EffectHitbox;
import btm.sword.effectshape.LineHitbox;
import btm.sword.visualeffect.*;
import org.bukkit.Color;
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
				new LineHitbox()),
			List.of(
				new LineVisual(
					List.of(
							new ParticleData(Particle.WHITE_ASH),
							new ParticleData(Particle.DUST, new Particle.DustOptions(Color.SILVER, 0.5f))),
					2, .01, .25),
				new TargetVisual(
					List.of(
							new ParticleData(Particle.CRIMSON_SPORE),
							new ParticleData(Particle.CRIT)),
					20, 1),
				new DirectionalPointVisual(
					List.of(
							new ParticleData(Particle.POOF),
							new ParticleData(Particle.SMOKE)),
					15, 0, new Vector(0,-0.5,3))),
			50.0,
			2.0),
	
	SWORD_SLASH(7,
			List.of(
				new LineHitbox()),
			List.of(
				new ArcVisual(
					List.of(
							new ParticleData(Particle.DUST, new Particle.DustOptions(Color.SILVER, 0.75f))
					),
					12, .25, 180, 45, 4.0),
				new ArcVisual(
					List.of(
							new ParticleData(Particle.DRIPPING_LAVA)),
					12, .25, 90, -45, 6.0),
				new TargetVisual(
					List.of(
							new ParticleData(Particle.FLAME),
							new ParticleData(Particle.DRAGON_BREATH)),
					12, .5),
				new TargetVisual(
					List.of(
							new ParticleData(Particle.FLASH)),
					2, .5)),
			7.0,
			2.0);
	
	private final double damage;
	private final List<EffectHitbox> effectHitboxes;
	private final List<VisualEffect> visualEffects;
	private final double range;
	private final double cooldown;
	
	AttackType(double damage, List<EffectHitbox> effectHitboxes, List<VisualEffect> visualEffects, double range, double cooldown) {
		this.damage = damage;
		this.effectHitboxes = effectHitboxes;
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
	
	public List<EffectHitbox> getEffectShapes() {
		return effectHitboxes;
	}
	
	public List<VisualEffect> getVisualEffects() {
		return visualEffects;
	}
	
	public HashSet<LivingEntity> getTargets(Player player, Location origin, Vector direction, double range) {
		HashSet<LivingEntity> targets = new HashSet<>();
		for (EffectHitbox hitBox : effectHitboxes) {
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
