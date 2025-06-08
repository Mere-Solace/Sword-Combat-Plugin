package btm.sword.combat.attack;

import btm.sword.effect.EffectManager;
import btm.sword.system.entity.SwordEntity;
import org.bukkit.Bukkit;

import java.util.Objects;

public abstract class Attack implements Runnable {
	private final AttackManager attackManager;
	private final EffectManager effectManager;
	
	protected final SwordEntity executor;
	
	public Attack(AttackManager attackManager, AttackOptions options, SwordEntity executor) {
		this.attackManager = attackManager;
		effectManager = attackManager.getEffectManager();
		this.executor = executor;
	}
	
	public abstract void onRun();
	
	public void run() {
		executor.getAssociatedEntity().sendMessage("Attacking!");
		onRun();
	}
}
