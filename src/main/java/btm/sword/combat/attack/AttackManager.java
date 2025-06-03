package btm.sword.combat.attack;

import btm.sword.effect.Effect;
import btm.sword.effect.EffectManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class AttackManager {
	private final EffectManager effectManager;
	private HashMap<Attack, BukkitTask> attacks;
	
	public AttackManager(EffectManager effectManager) {
		this.effectManager = effectManager;
	}
	
	public void start(Attack attack) {
		attack.run();
	}
	
	public void runAttackEffect(Effect effect) {
		effectManager.start(effect);
	}
	
	public EffectManager getEffectManager() {
		return effectManager;
	}
}
