package btm.sword.effect.effects;

import btm.sword.effect.Effect;
import btm.sword.effect.EffectExecutionType;
import btm.sword.effect.EffectManager;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Line extends Effect {
	double length = 10;
	
	public Line(EffectManager effectManager) {
		super(effectManager);
	}
	
	public Line(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution, double length) {
		super(effectManager, type, particles, resolution);
		this.length = length;
	}
	
	@Override
	public void onRun() {
		
		Vector step = direction.clone().normalize().multiply(1/resolution);
		
		for (int x = 0; x < length*resolution; x++) {
			points.add(location.add(step));
		}
	}
}
