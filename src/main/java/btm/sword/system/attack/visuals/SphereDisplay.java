package btm.sword.system.attack.visuals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.display.ParticleWrapper;
import lombok.Getter;
import lombok.Setter;

/**
 * A sphere {@link ParticleDisplay} — draws particles distributed on a sphere surface (shell)
 * or throughout its volume (filled). Uses {@link DrawUtil#sphere} for uniform sampling.
 */
@Getter
@Setter
public final class SphereDisplay extends ParticleDisplay {

    /** Sphere radius in blocks. */
    private double radius;
    /** Approximate number of sample points drawn per emission. */
    private int density;
    /** {@code true} for filled-volume distribution; {@code false} for surface shell. */
    private boolean filled;

    /**
     * Creates a sphere display with explicit field values.
     *
     * @param anchor            origin anchor for the sphere centre
     * @param originOffset      fixed local-space offset
     * @param randomOffsetRange per-axis jitter half-extents
     * @param repeatCount       number of emissions
     * @param repeatPeriodTicks ticks between emissions
     * @param particles         particles drawn at each sample point
     * @param radius            sphere radius in blocks
     * @param density           number of sample points per emission
     * @param filled            {@code true} for filled-volume, {@code false} for shell
     */
    public SphereDisplay(OriginAnchor anchor, Vector3f originOffset, Vector3f randomOffsetRange,
            int repeatCount, int repeatPeriodTicks, List<ParticleEffect> particles,
            double radius, int density, boolean filled) {
        super(anchor, originOffset, randomOffsetRange, repeatCount, repeatPeriodTicks, particles);
        this.radius = Math.max(0.0, radius);
        this.density = Math.max(1, density);
        this.filled = filled;
    }

    /** Convenience factory: unit-radius shell sphere anchored to its owning keyframe. */
    public static SphereDisplay defaults() {
        return new SphereDisplay(OriginAnchor.owning(), new Vector3f(), new Vector3f(),
            1, 0, new ArrayList<>(), 1.0, 30, false);
    }

    @Override
    protected void draw(Location resolved, EffectsContext ctx) {
        List<ParticleWrapper> wrappers = particles.stream().map(ParticleEffect::toWrapper).toList();
        DrawUtil.sphere(wrappers, resolved, radius, density, filled);
    }

    @Override
    public String shapeTypeLabel() {
        return "Sphere";
    }
}
