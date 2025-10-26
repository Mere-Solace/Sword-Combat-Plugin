package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.util.Vector;

import java.util.List;

public class DisplayUtil {
    public static void setInterpolationValues(Display display, int delay, int duration) {
        display.setInterpolationDelay(delay);
        display.setInterpolationDuration(duration);
    }

	public static void secant(List<ParticleWrapper> particles, Location origin, Location end, double width) {
		Vector direction = end.clone().subtract(origin).toVector();
		int steps = (int) (direction.length() / (width));
		if (steps == 0) steps = 1;
		
		Vector step = direction.clone().normalize().multiply(width);
		Location cur = origin.clone();
		
		for (int i = 0; i <= steps; i++) {
			cur.add(step);
			for (ParticleWrapper p : particles) {
				p.display(cur);
			}
		}
	}
	
	public static void line(List<ParticleWrapper> particles, Location origin, Vector dir, double length, double width) {
		Vector step = dir.clone().normalize().multiply(width);
		Location cur = origin.clone();
		for (double i = 0; i <= length; i += width) {
			cur.add(step);
			for (ParticleWrapper p : particles) {
				p.display(cur);
			}
		}
	}
}
