package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.effect.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class AttackManager {
	private final Plugin plugin;
	private final EffectManager effectManager;
	private final HashMap<Attack, BukkitTask> attacks = new HashMap<>();
	
	public AttackManager(EffectManager effectManager) {
		plugin = Sword.getInstance();
		this.effectManager = effectManager;
	}
	
	public void start(Attack attack) {
		Sword.getInstance().getLogger().info("Starting attack: " + attack.toString());
		BukkitScheduler s = Bukkit.getScheduler();
		
		BukkitTask task = s.runTaskTimer(plugin, attack, attack.getDelayTicks(), attack.getPeriodTicks());
		
		attacks.put(attack, task);
	}
	
	public void removeAttack(Attack attack) {
		BukkitTask existingAttackTask = attacks.get(attack);
		
		if (existingAttackTask != null) existingAttackTask.cancel();
		
		attacks.remove(attack);
	}
	
	public EffectManager getEffectManager() {
		return effectManager;
	}
	
	public void done(Attack attack) {
		removeAttack(attack);
	}
}
