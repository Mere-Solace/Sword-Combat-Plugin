package btm.sword.combat.visuals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import btm.sword.combat.simulation.ParticleEffect;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * A two-point line {@link ParticleDisplay} — draws a series of particles from the resolved
 * start point (the inherited anchor) to a resolved end point (the {@link #endAnchor}). The
 * {@code end} anchor has its own fixed offset and optional random jitter.
 */
@Getter
@Setter
public final class LineDisplay extends ParticleDisplay {

    /** Origin anchor for the line's end point. Never {@code null}. */
    private OriginAnchor endAnchor;
    /** Fixed offset applied after {@link #endAnchor} resolution, in the attack's local space. */
    private Vector3f endOffset;
    /** Per-axis half-extents of the random-jitter box around the end point. */
    private Vector3f endRandomRange;
    /** Spacing (in blocks) between particle samples along the line. */
    private double spacing;

    /**
     * Creates a line display with explicit field values.
     *
     * @param anchor            origin anchor for the start point
     * @param originOffset      fixed local-space offset at the start point
     * @param randomOffsetRange per-axis jitter half-extents at the start point
     * @param repeatCount       number of emissions
     * @param repeatPeriodTicks ticks between emissions
     * @param particles         particles drawn at each sample along the line
     * @param endAnchor         anchor for the line's end point
     * @param endOffset         fixed local-space offset at the end point
     * @param endRandomRange    per-axis jitter half-extents at the end point
     * @param spacing           distance between particle samples along the line, in blocks
     */
    public LineDisplay(OriginAnchor anchor, Vector3f originOffset, Vector3f randomOffsetRange,
            int repeatCount, int repeatPeriodTicks, List<ParticleEffect> particles,
            OriginAnchor endAnchor, Vector3f endOffset, Vector3f endRandomRange, double spacing) {
        super(anchor, originOffset, randomOffsetRange, repeatCount, repeatPeriodTicks, particles);
        this.endAnchor = endAnchor != null ? endAnchor : OriginAnchor.owning();
        this.endOffset = endOffset != null ? endOffset : new Vector3f();
        this.endRandomRange = endRandomRange != null ? endRandomRange : new Vector3f();
        this.spacing = spacing > 0 ? spacing : 0.25;
    }

    /** Convenience factory: zero-length line anchored to its owning keyframe at both ends. */
    public static LineDisplay defaults() {
        return new LineDisplay(OriginAnchor.owning(), new Vector3f(), new Vector3f(),
            1, 0, new ArrayList<>(),
            OriginAnchor.owning(), new Vector3f(), new Vector3f(), 0.25);
    }

    @Override
    protected void draw(Location start, EffectsContext ctx) {
        Location end = OriginResolver.resolve(endAnchor, ctx);
        Vector3f worldEndOffset = ctx.worldTransform().transformDirection(new Vector3f(endOffset));
        end.add(worldEndOffset.x, worldEndOffset.y, worldEndOffset.z);
        if (endRandomRange.x != 0f || endRandomRange.y != 0f || endRandomRange.z != 0f) {
            Vector3f jitter = ctx.worldTransform().transformDirection(VectorUtil.randomInBox(endRandomRange));
            end.add(jitter.x, jitter.y, jitter.z);
        }
        List<ParticleWrapper> wrappers = particles.stream().map(ParticleEffect::toWrapper).toList();
        DrawUtil.secant(wrappers, start, end, spacing);
    }

    @Override
    public String shapeTypeLabel() {
        return "Line";
    }

    @Override
    public LineDisplay copy() {
        LineDisplay c = new LineDisplay(anchor,
            new Vector3f(originOffset), new Vector3f(randomOffsetRange),
            repeatCount, repeatPeriodTicks, copyParticles(),
            endAnchor, new Vector3f(endOffset), new Vector3f(endRandomRange), spacing);
        c.betweenKfRepeat = betweenKfRepeat;
        return c;
    }
}
