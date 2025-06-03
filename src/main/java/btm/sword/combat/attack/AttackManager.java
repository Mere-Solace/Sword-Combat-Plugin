package btm.sword.combat.attack;

import btm.sword.effect.Effect;
import btm.sword.effect.EffectManager;

public class AttackManager {
	private final EffectManager effectManager;
	
	public AttackManager(EffectManager effectManager) {
		this.effectManager = effectManager;
	}
	
	public void runAttackEffect(Effect effect) {
		effectManager.start(effect);
	}
	
	public EffectManager getEffectManager() {
		return effectManager;
	}
}
