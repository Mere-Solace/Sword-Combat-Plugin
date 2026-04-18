package btm.sword.system.attack.visuals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.utility.display.ParticleWrapper;

/**
 * A single-point {@link ParticleDisplay} — emits its particles at the resolved origin.
 * This is the simplest shape and maps onto the pre-overhaul {@code ParticleEffect}.
 */
public final class PointDisplay extends ParticleDisplay {

    /**
     * Creates a point display with explicit field values.
     *
     * @param anchor            origin anchor
     * @param originOffset      fixed local-space offset
     * @param randomOffsetRange per-axis jitter half-extents
     * @param repeatCount       number of emissions
     * @param repeatPeriodTicks ticks between emissions
     * @param particles         particles to emit at the resolved point
     */
    public PointDisplay(OriginAnchor anchor, Vector3f originOffset, Vector3f randomOffsetRange,
            int repeatCount, int repeatPeriodTicks, List<ParticleEffect> particles) {
        super(anchor, originOffset, randomOffsetRange, repeatCount, repeatPeriodTicks, particles);
    }

    /** Convenience factory: creates an empty PointDisplay anchored to its owning keyframe. */
    public static PointDisplay defaults() {
        return new PointDisplay(OriginAnchor.owning(), new Vector3f(), new Vector3f(),
            1, 0, new ArrayList<>());
    }

    @Override
    protected void draw(Location resolved, EffectsContext ctx) {
        List<ParticleWrapper> wrappers = particles.stream().map(ParticleEffect::toWrapper).toList();
        ParticleWrapper.displayAll(wrappers, resolved);
    }

    @Override
    public String shapeTypeLabel() {
        return "Point";
    }
}
