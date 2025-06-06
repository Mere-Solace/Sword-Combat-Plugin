package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.util.ParticleSpawner;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;

public class EffectManager {
	private final Plugin plugin;
	private final ParticleSpawner spawner;
	
	private final HashMap<Effect, BukkitTask> effects = new HashMap<>();
	
	public EffectManager(ParticleSpawner spawner) {
		this(Sword.getInstance(), spawner);
	}
	
	public EffectManager(Plugin plugin, ParticleSpawner spawner) {
		this.plugin = plugin;
		this.spawner = spawner;
	}
	
	public void display(List<ParticleWrapper> particles, Location location) {
		for (ParticleWrapper particle : particles)
			display(particle, location);
	}
	
	public void display(ParticleWrapper particle, Location location) {
		spawner.spawnParticle(particle, location, null);
	}
	
	public void start(Effect effect) {
		
		BukkitScheduler s = Bukkit.getScheduler();
		
		BukkitTask task = s.runTaskTimer(plugin, effect, effect.getDelayTicks(), effect.getPeriod());
		
		effects.put(effect, task);
	}
	
	public void removeEffect(Effect effect) {
		BukkitTask existingTask = effects.get(effect);
		
		if (existingTask != null) existingTask.cancel();
		
		effects.remove(effect);
	}
	
	public void done(Effect effect) {
		synchronized (this) {
			removeEffect(effect);
			if (effect.callback != null) Bukkit.getScheduler().runTask(plugin, effect.callback);
		}
	}
}
