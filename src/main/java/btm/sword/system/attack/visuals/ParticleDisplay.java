package btm.sword.system.attack.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.joml.Vector3f;

import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.system.control.SwordScheduler;
import btm.sword.utility.math.VectorUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base for all keyframe visual effects. A {@code ParticleDisplay} resolves a
 * world-space origin from an {@link OriginAnchor}, applies a local-space fixed offset and
 * optional random jitter, and delegates actual shape drawing to its subclass.
 *
 * <p>Sealed over the built-in shape set: {@link PointDisplay}, {@link LineDisplay},
 * {@link SphereDisplay}, {@link CircleDisplay}.</p>
 *
 * <p>Supports repeat emissions via {@link #repeatCount} and {@link #repeatPeriodTicks}.
 * Repeats are scheduled on the main thread via {@link SwordScheduler#runBukkitTaskLater}.
 * Each repeat re-resolves the anchor and re-rolls jitter so body-point-anchored displays
 * track a moving attacker.</p>
 */
@Getter
@Setter
public abstract sealed class ParticleDisplay
    permits PointDisplay, LineDisplay, SphereDisplay, CircleDisplay {

    /** Where the display origin comes from. Never {@code null}. */
    protected OriginAnchor anchor;
    /** Fixed offset applied after anchor resolution, in the attack's local space. */
    protected Vector3f originOffset;
    /** Per-axis half-extents of the random-jitter box. Component-wise zero disables per-axis jitter. */
    protected Vector3f randomOffsetRange;
    /** How many times to emit this display. {@code 1} = single-shot (no repeats). */
    protected int repeatCount;
    /** Ticks between repeat emissions. Ignored when {@link #repeatCount} &lt;= 1. */
    protected int repeatPeriodTicks;
    /**
     * When &gt; 1, fire this display this many total times evenly distributed across the window
     * from this keyframe's {@code t} to the next keyframe's {@code t} (scheduled by
     * {@link btm.sword.system.attack.simulation.EffectsDispatcher}).
     * {@code 0} means inactive — normal {@link #repeatCount}/{@link #repeatPeriodTicks} logic applies.
     */
    protected int betweenKfRepeat = 0;
    /** Particles to display at each point this shape emits to. Never {@code null}. */
    protected List<ParticleEffect> particles;

    /**
     * Creates a ParticleDisplay with the shared visual fields. Subclasses add shape-specific state.
     *
     * @param anchor            origin anchor; {@code null} defaults to {@link OriginAnchor#owning()}
     * @param originOffset      fixed local-space offset; {@code null} defaults to zero
     * @param randomOffsetRange per-axis jitter half-extents; {@code null} defaults to zero
     * @param repeatCount       number of emissions; clamped to {@code >= 1}
     * @param repeatPeriodTicks ticks between emissions; clamped to {@code >= 0}
     * @param particles         particles to emit; {@code null} defaults to an empty mutable list
     */
    protected ParticleDisplay(OriginAnchor anchor, Vector3f originOffset, Vector3f randomOffsetRange,
            int repeatCount, int repeatPeriodTicks, List<ParticleEffect> particles) {
        this.anchor = anchor != null ? anchor : OriginAnchor.owning();
        this.originOffset = originOffset != null ? originOffset : new Vector3f();
        this.randomOffsetRange = randomOffsetRange != null ? randomOffsetRange : new Vector3f();
        this.repeatCount = Math.max(1, repeatCount);
        this.repeatPeriodTicks = Math.max(0, repeatPeriodTicks);
        this.particles = particles != null ? particles : new ArrayList<>();
    }

    /**
     * Main-thread entry point. Emits once immediately, then schedules {@code repeatCount - 1}
     * follow-up emissions at {@code repeatPeriodTicks}-tick intervals. Each emission re-resolves
     * the anchor and re-rolls any configured random jitter.
     *
     * @param ctx the effects dispatch context for this keyframe firing
     */
    public final void render(EffectsContext ctx) {
        emitOnce(ctx);
        for (int i = 1; i < repeatCount; i++) {
            int delayMs = i * repeatPeriodTicks * 50;
            SwordScheduler.runBukkitTaskLater(() -> emitOnce(ctx), delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /** Fires a single emission without scheduling any repeats — used by between-kf repeat scheduling. */
    public final void renderOnce(EffectsContext ctx) {
        emitOnce(ctx);
    }

    private void emitOnce(EffectsContext ctx) {
        Location base = OriginResolver.resolve(anchor, ctx);
        Vector3f worldOffset = ctx.worldTransform().transformDirection(new Vector3f(originOffset));
        base.add(worldOffset.x, worldOffset.y, worldOffset.z);
        if (randomOffsetRange.x != 0f || randomOffsetRange.y != 0f || randomOffsetRange.z != 0f) {
            Vector3f jitter = ctx.worldTransform().transformDirection(VectorUtil.randomInBox(randomOffsetRange));
            base.add(jitter.x, jitter.y, jitter.z);
        }
        draw(base, ctx);
    }

    /**
     * Subclass hook — draws this display's shape at the resolved world-space location.
     *
     * @param resolved the resolved, offset-and-jittered world location
     * @param ctx      the effects dispatch context (carries worldTransform for shape orientation)
     */
    protected abstract void draw(Location resolved, EffectsContext ctx);

    /** Returns a short identifier of this display's shape type — used in menus. */
    public abstract String shapeTypeLabel();

    /**
     * Returns a deep copy of this display — all mutable fields (Vector3fs, particle list,
     * nested Vector3fs inside particles) are freshly allocated so the copy can be mutated
     * without affecting the source. Used by dev editor menus that work on a draft and
     * commit back to the session on save.
     */
    public abstract ParticleDisplay copy();

    /** Shared helper for subclass {@code copy()} — deep-copies the particle list. */
    protected final List<ParticleEffect> copyParticles() {
        List<ParticleEffect> out = new ArrayList<>(particles.size());
        for (ParticleEffect e : particles) {
            out.add(new ParticleEffect(e.type(), e.count(),
                new Vector3f(e.spreadOffset()), e.speed(), e.dustOptions()));
        }
        return out;
    }
}
