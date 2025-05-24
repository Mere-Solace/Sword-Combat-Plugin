package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public abstract class VisualEffect {
	List<Particle> particles;
	int count;
	double offset;
	
	public VisualEffect(List<Particle> particles, int count, double offset) {
		this.particles = particles;
		this.count = count;
		this. offset = offset;
	}
	
	public abstract void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets);
}
