package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class ParticleSpawner {
	private double displayRadius = 50;
	
	public ParticleSpawner() {
	
	}
	
	public ParticleSpawner(double displayRadius) {
		this.displayRadius = displayRadius;
	}
	
	public double getDisplayRadius() {
		return displayRadius;
	}
	
	public void setDisplayRadius(double displayRadius) {
		this.displayRadius = displayRadius;
	}
	
	public void spawnParticle(ParticleWrapper particle, Location origin, List<Player> targetPlayers) {
		if (targetPlayers == null) {
			particle.display(origin);
		}
	}
}
