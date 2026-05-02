package btm.sword.util.display;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import btm.sword.util.math.Basis;

/** Utility methods for drawing particle sequences between world locations. */
public final class DrawUtil {

    private DrawUtil() {}

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
            ParticleWrapper.displayAll(particles, cur);
        }
    }

    /**
     * Displays a filled ring of particles in the plane defined by {@code basis}.
     *
     * @param particles     the {@link btm.sword.utility.sound.ParticleWrapper} list to spawn
     * @param origin        the center of the ring
     * @param basis         orientation basis; the ring lies in the {@code forward}–{@code right} plane
     * @param outerRadius   outer edge of the ring, in blocks
     * @param innerRadius   inner edge (hole) of the ring, in blocks
     * @param spacingRadial distance between particle rows along the radial direction, in blocks
     * @param spacingArc    angular spacing between particles around the ring, in radians
     */
    public static void circle(List<ParticleWrapper> particles, Location origin, Basis basis, double outerRadius, double innerRadius, double spacingRadial, double spacingArc) {
        Vector up = basis.up();
        Vector forward = basis.forward();
        Vector rotator;

        int radialIterations = Math.max(1, (int) ((outerRadius - innerRadius) / spacingRadial));

        double theta = 0;
        while (theta < 2 * Math.PI) {
            theta += spacingArc;
            rotator = forward.clone().rotateAroundAxis(up, theta);
            for (int i = radialIterations; i > 0; i--) {
                ParticleWrapper.displayAll(particles, origin.clone().add(rotator.clone().multiply(innerRadius + (i * spacingRadial))));
            }
        }
    }

    /**
     * Displays particles distributed on a sphere surface (shell) or throughout its volume.
     *
     * <p>Uses Gaussian-trick sampling for uniform direction, then scales by {@code radius}
     * (for shell) or {@code radius * cbrt(uniform(0,1))} (for filled ball).</p>
     *
     * @param particles the particle wrappers to display at each sample point
     * @param origin    the sphere centre
     * @param radius    the sphere radius in blocks
     * @param density   the number of sample points to draw
     * @param filled    {@code true} for filled-volume distribution; {@code false} for surface shell
     */
    public static void sphere(List<ParticleWrapper> particles, Location origin, double radius, int density, boolean filled) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < density; i++) {
            double x = r.nextGaussian();
            double y = r.nextGaussian();
            double z = r.nextGaussian();
            double norm = Math.sqrt(x * x + y * y + z * z);
            if (norm == 0.0) continue;
            double scale = radius / norm;
            if (filled) {
                scale *= Math.cbrt(r.nextDouble());
            }
            Location cur = origin.clone().add(x * scale, y * scale, z * scale);
            ParticleWrapper.displayAll(particles, cur);
        }
    }
}
