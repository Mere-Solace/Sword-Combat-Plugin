package btm.sword.combat.attack.attacktypes;

import btm.sword.Sword;
import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.combat.appliedEffect.DamageEffect;
import btm.sword.combat.appliedEffect.DirectKnockbackEffect;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackManager;
import btm.sword.effect.Effect;
import btm.sword.effect.effects.Arc;
import btm.sword.util.HitboxUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ArcAttack extends Attack {
	private double maxRange = 5.0f;
	private double minRange = 2.0f;
	private double thickness = 0.5f;
	private double maxAngle = 120;
	private double roll = 45;
	
	public ArcAttack(AttackManager attackManager, List<Effect> effects, List<AppliedEffect> appliedEffects) {
		super(attackManager);
		this.effects.addAll(Objects.requireNonNullElseGet(effects, () ->
				List.of(new Arc(attackManager.getEffectManager(), maxRange, minRange, 0, roll, maxAngle))));
		this.appliedEffects.addAll(Objects.requireNonNullElseGet(appliedEffects, () ->
				List.of(new DamageEffect(5), new DirectKnockbackEffect(1, 2))));
	}
	
	public ArcAttack(AttackManager attackManager, List<Effect> effects, List<AppliedEffect> appliedEffects, double maxRange, double minRange, double thickness, double maxAngle, double roll) {
		this(attackManager, effects, appliedEffects);
		this.maxRange = maxRange;
		this.minRange = minRange;
		this.thickness = thickness;
		this.maxAngle = Math.toRadians(maxAngle);
		this.roll = Math.toRadians(roll);
	}
	
	public ArcAttack(AttackManager attackManager, double maxRange, double minRange, double thickness, double maxAngle, double roll) {
		this(attackManager, null, null);
		this.maxRange = maxRange;
		this.minRange = minRange;
		this.thickness = thickness;
		this.maxAngle = Math.toRadians(maxAngle);
		this.roll = Math.toRadians(roll);
	}
	
	@Override
	public void onRun() {
		Sword.getInstance().getLogger().info("Arc onRun Starting");
//		setTargets(HitboxUtils.arc(executor, executor.getEyeLocation().subtract(0, .5, 0), executor.getEyeLocation().getDirection(),
//				maxRange, minRange, thickness, maxAngle, roll));
		
		for (AppliedEffect appliedEffect : appliedEffects) {
			appliedEffect.applyEffect(playerData, new HashSet<>(targets));
		}
	}
}
