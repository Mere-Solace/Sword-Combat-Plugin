package btm.sword.util.display;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class DrawUtil {
    /**
     * Creates a linear sequence of particles (or effects) between two locations using the secant method.
     * The particles are spaced evenly along the line from origin to end, separated by a specified spacing.
     *
     * @param particles a list of {@link ParticleWrapper} objects responsible for display
     * @param origin the starting {@link Location}
     * @param end the ending {@link Location}
     * @param spacing the space between each particle effect along the line
     */
    public static void secant(List<ParticleWrapper> particles, Location origin, Location end, double spacing) {
        Vector direction = end.clone().subtract(origin).toVector();
        int steps = (int) (direction.length() / (spacing));
        if (steps == 0) steps = 1;

        Vector step = direction.clone().normalize().multiply(spacing);
        Location cur = origin.clone();

        for (int i = 0; i <= steps; i++) {
            cur.add(step);
            for (ParticleWrapper p : particles) {
                p.display(cur);
            }
        }
    }

    /**
     * Draws a line of particles or effects along a specified direction, starting from a location.
     * The line extends for a specified length and has a certain width between particle points.
     *
     * @param particles a list of {@link ParticleWrapper} objects responsible for display
     * @param origin the starting {@link Location}
     * @param dir the direction vector of the line
     * @param length the length of the line in blocks
     * @param width the spacing between each particle or effect along the line
     */
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
