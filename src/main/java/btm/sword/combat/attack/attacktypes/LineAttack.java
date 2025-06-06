package btm.sword.combat.attack.attacktypes;

import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.combat.appliedEffect.DamageEffect;
import btm.sword.combat.appliedEffect.DirectKnockbackEffect;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackManager;
import btm.sword.effect.Effect;
import btm.sword.effect.effects.Line;
import btm.sword.util.HitboxUtils;

import java.util.List;
import java.util.Objects;

public class LineAttack extends Attack {
	private double maxRange = 50;
	private double thickness = 0.5;
	
	public LineAttack(AttackManager attackManager, List<Effect> effects, List<AppliedEffect> appliedEffects) {
		super(attackManager);
		this.effects.addAll(Objects.requireNonNullElseGet(effects, () ->
				List.of(new Line(attackManager.getEffectManager(), maxRange))));
		this.appliedEffects.addAll(Objects.requireNonNullElseGet(appliedEffects, () ->
				List.of(new DamageEffect(5), new DirectKnockbackEffect(1, 2))));
	}
	
	public LineAttack(AttackManager attackManager, List<Effect> effects, List<AppliedEffect> appliedEffects, double maxRange, double thickness) {
		this(attackManager, effects, appliedEffects);
		this.maxRange = maxRange;
		this.thickness = thickness;
	}
	
	public LineAttack(AttackManager attackManager, double maxRange, double thickness) {
		this(attackManager, null, null);
		this.maxRange = maxRange;
		this.thickness = thickness;
	}
	
	@Override
	public void onRun() {
		setTargets(HitboxUtils.line(executor, executor.getEyeLocation().subtract(0, .5, 0), executor.getEyeLocation().getDirection(),
				maxRange, thickness));
		
		for (AppliedEffect appliedEffect : appliedEffects) {
			appliedEffect.applyEffect(playerData, targets);
		}
	}
}
