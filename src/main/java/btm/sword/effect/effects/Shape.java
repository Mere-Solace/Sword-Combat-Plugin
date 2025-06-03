package btm.sword.effect.effects;

import btm.sword.effect.Effect;
import btm.sword.effect.EffectExecutionType;
import btm.sword.effect.EffectManager;
import btm.sword.util.ParticleWrapper;

import java.util.List;

public class Shape extends Effect {
	
	public Shape(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles) {
		super(effectManager, type, particles);
	}
	
	@Override
	public void onRun() {
	
	}
}
