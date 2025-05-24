package btm.sword.visualeffect;

import btm.sword.Sword;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LineVisual extends VisualEffect {
	List<Particle> particles;
	double spacing;
	
	public LineVisual(List<Particle> particles, double spacing) {
	
	}
	
	public void drawEffect(Location origin, Vector direction, double range) {
		Sword sword = Sword.getInstance();
		
		Bukkit.getScheduler().runTaskAsynchronously(sword, () -> {
			List<Location> points = new ArrayList<>();
			Vector step = direction.clone().normalize().multiply(spacing);
			
			Location cur = origin.clone();
			
			for (double i = 0; i < range; i+= spacing) {
				points.add(cur.add(step));
			}
			
			Bukkit.getScheduler().runTask(sword, () -> {
				World world = origin.getWorld();
				for (Location l : points) {
					for (Particle p : particles) {
						world.spawnParticle(p, l, 1);
					}
				}
			});
		});
	}
}
